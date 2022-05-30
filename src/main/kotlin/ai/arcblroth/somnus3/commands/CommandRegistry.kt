package ai.arcblroth.somnus3.commands

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Somnus
import ai.arcblroth.somnus3.panel.InteractivePanel
import ai.arcblroth.somnus3.panel.InteractivePanelBuilder
import ai.arcblroth.somnus3.respond
import ai.arcblroth.somnus3.respondPanel
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.GuildUserCommandInteraction
import dev.kord.core.event.interaction.GuildApplicationCommandInteractionCreateEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import kotlinx.coroutines.flow.toSet
import org.slf4j.LoggerFactory

@SomnusCommandsDsl
class CommandRegistry private constructor(private val kord: Kord, private val somnus: Somnus, private val config: Config) {
    private val logger = LoggerFactory.getLogger("CommandRegistry")
    private var finished = false
    private val slashCommands: MutableMap<String, SlashCommand> = mutableMapOf()
    private val userCommands: MutableMap<String, SlashCommand> = mutableMapOf()
    private val textOnlyCommands: MutableMap<String, SlashCommand> = mutableMapOf()
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
        if (builderObj.options.isNotEmpty() && builderObj.options.count { it is MessageContentOption } > 0) {
            check(builderObj.options.size == 1) { "MessageContentOption must be the first and only option present!" }
        }
        val numRequiredOptions = checkOptionalArguments(builderObj.options)

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
     * Registers a new text-only command with one or more aliases.
     */
    fun text(name: String, vararg aliases: String, builder: SlashCommandBuilder.() -> Unit) {
        val builderObj = SlashCommandBuilder().also(builder)
        val exec = requireNotNull(builderObj.execute) { "Slash command must actually do something!" }
        val numRequiredOptions = checkOptionalArguments(builderObj.options)

        val command = SlashCommand(name, "", builderObj.options, numRequiredOptions, exec)
        textOnlyCommands[name] = command

        aliases.forEach {
            textOnlyCommands[it] = command.copy(name = it, isAlias = true)
        }
    }

    private fun checkOptionalArguments(options: List<Option<*>>): Int {
        var numRequiredOptions = 0
        var foundOptionalArg = false
        for (option in options) {
            when (option.optional) {
                true -> foundOptionalArg = true
                false -> {
                    check(!foundOptionalArg) { "All required options must be listed before optional options!" }
                    numRequiredOptions++
                }
            }
        }
        return numRequiredOptions
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
            } catch (e: Exception) {
                logger.warn("Couldn't register commands for server `${server.name}` (${server.id})")
            }
        }
    }

    // application command callback, invoked from `Somnus`
    internal suspend fun handleSlashCommand(event: GuildApplicationCommandInteractionCreateEvent) = with(event) {
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

                var panel: InteractivePanel? = null
                val result = interaction.respondPublic {
                    object : SlashCommandExecutionBuilder {
                        override fun respond(builder: MessageCreateBuilder.() -> Unit) {
                            this@respondPublic.builder()
                        }
                        override fun respondPanel(builder: InteractivePanelBuilder.() -> Unit) {
                            panel = this@respondPublic.respondPanel(builder)
                        }
                    }.(slashCommand.execute)(interaction.user, interaction.getGuildOrNull(), options)
                }
                if (panel != null) {
                    val message = kord.with(EntitySupplyStrategy.rest).getOriginalInteraction(result.applicationId, result.token)
                    somnus.registerInteractivePanel(message.id, panel!!)
                }
            }
        }
    }

    // invoked from `Somnus` so that we don't tokenize twice
    // assumes that the message has been sent from an allowed server
    internal suspend fun handleMessage(message: Message, author: User, commandPrefix: String, tokens: List<String>) {
        val slashCommand = slashCommands[commandPrefix] ?: textOnlyCommands[commandPrefix] ?: return
        if (tokens.size - 1 >= slashCommand.numRequiredOptions) {
            val options = slashCommand.options.withIndex().associate {
                if (it.index + 1 >= tokens.size) {
                    return@associate it.value.name to null
                }
                if (it.value is MessageContentOption) {
                    return@associate it.value.name to message.content.trim().substring(commandPrefix.length + 1)
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

            var panel: InteractivePanel? = null
            val result = message.respond actual@{
                object : TextBasedSlashCommandExecutionBuilder {
                    override val message: Message
                        get() = message

                    override fun respond(builder: MessageCreateBuilder.() -> Unit) {
                        this@actual.builder()
                    }
                    override fun respondPanel(builder: InteractivePanelBuilder.() -> Unit) {
                        panel = this@actual.respondPanel(builder)
                    }
                }.(slashCommand.execute)(author, message.getGuildOrNull(), options)
            }
            if (panel != null && result != null) {
                somnus.registerInteractivePanel(result.id, panel!!)
            }
        }
    }

    companion object {
        private val INPUT_COMMAND_NAME_REGEX = Regex("^[-_\\p{L}\\p{N}\\p{sc=Deva}\\p{sc=Thai}]{1,32}\$")

        suspend fun registerCommands(kord: Kord, somnus: Somnus, config: Config, builder: CommandRegistry.() -> Unit) =
            CommandRegistry(kord, somnus, config).also(builder).apply { finish() }
    }
}
