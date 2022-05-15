package ai.arcblroth.somnus3.commands

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.respond
import dev.kord.common.entity.ApplicationCommandType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.GuildUserCommandInteraction
import dev.kord.core.event.interaction.GuildApplicationCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.slf4j.LoggerFactory

@SomnusCommandsDsl
class CommandRegistry private constructor(private val kord: Kord, private val config: Config) {
    private val logger = LoggerFactory.getLogger("CommandRegistry")
    private var finished = false
    private val slashCommands: MutableMap<String, SlashCommand> = mutableMapOf()
    private val userCommands: MutableMap<String, SlashCommand> = mutableMapOf()
    private val registeredCommands: MutableMap<Snowflake, SlashCommand> = mutableMapOf()

    /**
     * Registers a new slash command.
     *
     * If `user` is set to true and the command takes a single user argument,
     * it will additionally be registered as a user command for consistency.
     */
    fun slash(name: String, user: Boolean = true, builder: SlashCommandBuilder.() -> Unit) {
        check(!finished)

        val builderObj = SlashCommandBuilder().also(builder)
        val desc = requireNotNull(builderObj.description) { "Slash command must have a description!" }
        val exec = requireNotNull(builderObj.execute) { "Slash command must actually do something!" }
        check(name.matches(INPUT_COMMAND_NAME_REGEX)) { "Main slash command name must match the validation regex!" }
        check(name.length in 1..32) { "Name must be between 1 and 32 characters long!" }
        check(desc.length in 1..100) { "Description must be between 1 and 100 characters long!" }
        var numRequiredOptions = 0
        var foundOptionalArg = false
        for (option in builderObj.options) {
            when (option.optional) {
                true -> foundOptionalArg = true
                false -> {
                    check(!foundOptionalArg) { "All required options must be listed before optional options!" }
                    numRequiredOptions++
                }
            }
        }

        val command = SlashCommand(name, desc, builderObj.options, numRequiredOptions, exec)
        slashCommands[name] = command

        if (user && command.options.size == 1 && command.options[0] is UserOption) {
            userCommands[name] = command
        }
    }

    /**
     * Registers a new slash command with one or more aliases.
     */
    fun slash(name: String, vararg aliases: String, user: Boolean = true, builder: SlashCommandBuilder.() -> Unit) {
        slash(name, user, builder)
        val command = slashCommands[name]!!
        aliases.forEach {
            slashCommands[it] = command.copy(name = it, isAlias = true)
        }
    }

    /**
     * Actually invokes the Discord API to finish command registration.
     */
    private suspend fun finish() {
        finished = true

        // update commands
        val servers = EntitySupplyStrategy.rest.supply(kord).guilds.toSet().filter { it.id in config.allowedServers }
        for (server in servers) {
            try {
                val currentCommands = kord.getGuildApplicationCommands(server.id).toList()

                // register new commands
                kord.createGuildApplicationCommands(server.id) {
                    for (command in slashCommands.values) {
                        if (command.isAlias && !command.name.matches(INPUT_COMMAND_NAME_REGEX)) {
                            // skip aliases that don't conform to the slash command name regex
                            // this is used for the !++ and !-- counter commands
                            continue
                        }
                        input(command.name, command.description) {
                            options = command.options
                                .map(Option<*>::toOptionsBuilder)
                                .toMutableList()
                        }
                    }
                    for (command in userCommands.values) {
                        user(command.name)
                    }
                }.collect {
                    registeredCommands[it.id] = slashCommands[it.name]!!
                }

                // delete any old commands
                for (oldCommand in currentCommands) {
                    when (oldCommand.type) {
                        ApplicationCommandType.ChatInput -> if (oldCommand.name !in slashCommands) {
                            oldCommand.delete()
                        }
                        ApplicationCommandType.User -> if (oldCommand.name !in userCommands) {
                            oldCommand.delete()
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                logger.warn("Couldn't register commands for server `${server.name}` (${server.id})")
            }
        }

        // register application command callback
        // note that the prefix command callback is implemented in `Somnus`
        kord.on<GuildApplicationCommandInteractionCreateEvent> {
            if (interaction.data.guildId.value in config.allowedServers) {
                val slashCommand = registeredCommands[interaction.invokedCommandId]
                if (slashCommand != null) {
                    val options = when (interaction) {
                        is GuildUserCommandInteraction -> {
                            mapOf(slashCommand.options[0].name to (interaction as GuildUserCommandInteraction).targetId)
                        }
                        else -> {
                            interaction.data.data.options.value?.associate {
                                it.name to it.value.value?.value
                            }.orEmpty()
                        }
                    }
                    interaction.respondPublic {
                        object : SlashCommandExecutionBuilder {
                            override fun respond(builder: MessageCreateBuilder.() -> Unit) {
                                this@respondPublic.builder()
                            }
                        }.(slashCommand.execute)(interaction.user, interaction.getGuildOrNull(), options)
                    }
                }
            }
        }
    }

    // invoked from `Somnus` so that we don't tokenize twice
    // assumes that the message has been sent from an allowed server
    internal suspend fun handleMessage(message: Message, author: User, commandPrefix: String, tokens: List<String>) {
        val slashCommand = slashCommands[commandPrefix] ?: return
        if (tokens.size - 1 >= slashCommand.numRequiredOptions) {
            val options = slashCommand.options.withIndex().associate {
                if (it.index + 1 >= tokens.size) {
                    return@associate it.value.name to null
                }
                val token = tokens[it.index + 1]
                val maybeParsed = it.value.parse(token)
                if (maybeParsed != null) {
                    return@associate it.value.name to maybeParsed
                } else {
                    val onParseFailure = it.value.onParseFailure
                    if (onParseFailure != null) {
                        message.respond {
                            onParseFailure(token)
                        }
                    }
                    return@handleMessage
                }
            }
            message.respond actual@{
                object : SlashCommandExecutionBuilder {
                    override fun respond(builder: MessageCreateBuilder.() -> Unit) {
                        this@actual.builder()
                    }
                }.(slashCommand.execute)(author, message.getGuildOrNull(), options)
            }
        }
    }

    companion object {
        private val INPUT_COMMAND_NAME_REGEX = Regex("^[-_\\p{L}\\p{N}\\p{sc=Deva}\\p{sc=Thai}]{1,32}\$")

        suspend fun registerCommands(kord: Kord, config: Config, builder: CommandRegistry.() -> Unit) =
            CommandRegistry(kord, config).also(builder).apply { finish() }
    }
}
