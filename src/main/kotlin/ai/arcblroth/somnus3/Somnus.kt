package ai.arcblroth.somnus3

import ai.arcblroth.somnus3.activity.ActivityDetector
import ai.arcblroth.somnus3.commands.impl.update
import ai.arcblroth.somnus3.commands.registerCommandCallbacks
import ai.arcblroth.somnus3.data.withPreferencesData
import ai.arcblroth.somnus3.feeds.XkcdFeed
import ai.arcblroth.somnus3.mcserver.ServerInfoProvider
import ai.arcblroth.somnus3.panel.InteractivePanel
import ai.arcblroth.somnus3.panel.InteractivePanelBuilder
import ai.arcblroth.somnus3.panel.InteractivePanelBuilderImpl
import ai.arcblroth.somnus3.soundboard.SoundboardManager
import dev.kord.common.entity.ActivityType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildApplicationCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.user.PresenceUpdateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.gateway.builder.PresenceBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.maow.owo.util.Options
import dev.maow.owo.util.OwOFactory
import io.github.reactivecircus.cache4k.Cache
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.nio.file.Paths
import kotlin.time.Duration.Companion.minutes

class Somnus(private val config: Config, private val serverInfoProvider: ServerInfoProvider?) {
    private val activityDetectors: MutableList<ActivityDetector> = mutableListOf()
    private val panels: Cache<Snowflake, InteractivePanel> = Cache.Builder().expireAfterWrite(5.minutes).build()
    private val owofier = OwOFactory.INSTANCE.create(Options.defaults().apply { addSuffix(", meow!") })
    val soundboardManager = SoundboardManager()

    suspend fun start() {
        start {
            when (config.statusType) {
                ActivityType.Streaming -> streaming(
                    config.status.substringBefore(","),
                    config.status.substringAfter(","),
                )
                ActivityType.Listening -> listening(config.status)
                ActivityType.Watching -> watching(config.status)
                ActivityType.Competing -> competing(config.status)
                else -> playing(config.status)
            }
        }
    }

    suspend fun start(activity: PresenceBuilder.() -> Unit) {
        val token = readToken()
        val kord = Kord(token)

        kord.on<ReadyEvent> {
            val self = kord.getSelf()
            println("Logged in as ${self.username}#${self.discriminator}!")

            kord.editPresence(activity)
        }

        registerCallbacks(kord)

        @OptIn(PrivilegedIntent::class)
        kord.login {
            intents += Intent.GuildPresences
            intents += Intent.MessageContent
        }
    }

    private suspend fun registerCallbacks(kord: Kord) {
        val commands = registerCommandCallbacks(kord, this, config, serverInfoProvider)

        kord.on<MessageCreateEvent> {
            activityDetectors.forEach { it.invalidate(this) }

            val author = message.author
            if (author != null && allowOnServer(message)) {
                val command = message.content.trim()
                val strippedCommand = command
                    .replace(Constants.MENTION_FILTER, "")
                    .replace(Constants.CHANNEL_FILTER, "")
                    .replace(Constants.EMOJI_FILTER, "")
                    .replace(Constants.TIMESTAMP_FILTER, "")
                val tokens = command.split(Regex("\\s+?"))

                val (showWittyMessages, showKittyMessages) = withPreferencesData(author.id) {
                    showWittyMessages to showKittyMessages
                }

                if (showWittyMessages) {
                    handleWittyResponses(message, author, strippedCommand, tokens)
                }
                if (showKittyMessages) {
                    handleKittyResponses(message, strippedCommand)
                }

                if (!author.isBot) {
                    update(message.channel, author)
                    if (command.startsWith(Constants.PREFIX)) {
                        commands.handleMessage(message, author, tokens[0].substring(1), tokens)
                    }
                }
            }
        }

        kord.on<GuildApplicationCommandInteractionCreateEvent> {
            update(interaction.channel, interaction.user)
            commands.handleSlashCommand(this)
        }

        kord.on<ButtonInteractionCreateEvent> {
            with(interaction.deferPublicMessageUpdate()) {
                panels.get(interaction.message.id)?.let {
                    when (interaction.component.customId) {
                        "previous" -> it.previousPage()
                        "next" -> it.nextPage()
                    }
                    edit { it.updatePage(this) }
                }
            }
        }

        kord.on<VoiceStateUpdateEvent> {
            soundboardManager.update(this)
        }

        suspend fun bakePerGuildChannelMap(map: Map<Snowflake, Snowflake>) =
            map.mapValues { kord.getChannel(it.value) as? TextChannel? }
                .filterValues { it != null }
                .mapValues { it.value!! }

        if (config.enableActivityDetectors) {
            val leagueConfig = bakePerGuildChannelMap(config.leagueDetectorConfig!!)
            val leagueDetector = ActivityDetector(leagueConfig, "@%s is playing **LEAGUE OF LEGENDS**") {
                it.applicationId in config.leagueAppIds &&
                    it.name.equals("league of legends", ignoreCase = true) &&
                    it.state.equals("in game", ignoreCase = true)
            }

            val intellijConfig = bakePerGuildChannelMap(config.intellijDetectorConfig!!)
            val intellijDetector = ActivityDetector(intellijConfig, "@%s is writing bugs again") {
                it.applicationId in config.intellijAppIds
            }

            activityDetectors.add(leagueDetector)
            activityDetectors.add(intellijDetector)

            kord.on<PresenceUpdateEvent> {
                activityDetectors.forEach { it.update(this) }
            }
        }

        if (config.enableFeeds) {
            if (config.xkcdSubscribers != null) {
                val xkcdConfig = bakePerGuildChannelMap(config.xkcdSubscribers)
                XkcdFeed.start(kord, xkcdConfig)
            }
        }
    }

    private suspend fun handleWittyResponses(message: Message, author: User, strippedCommand: String, tokens: List<String>) {
        // nice
        if (strippedCommand.contains("69") || strippedCommand.contains("420")) {
            message.respond("<@!${author.id}> nice")
        }
        if (strippedCommand.contains("valentine", ignoreCase = true)) {
            message.respond("Did you mean: *Singles Appreciation Day*")
        }

        // avenge owo kills
        if (tokens.isNotEmpty()) {
            var mention: String? = null
            if (tokens.size >= 3 && tokens[0] == "owo" && tokens[1] == "kill") {
                mention = tokens[2]
            } else if (tokens.size >= 2 && tokens[0] == "owokill") {
                mention = tokens[1]
            }
            if (mention != null) {
                if (mention.matches(Regex("<@!?${message.kord.selfId}>"))) {
                    message.respond("owo kill <@${author.id}>")
                }
            }
        }
    }

    private suspend fun handleKittyResponses(message: Message, strippedCommand: String) {
        message.respond(this uwu strippedCommand)
    }

    fun registerInteractivePanel(id: Snowflake, panel: InteractivePanel) {
        panels.put(id, panel)
    }

    infix fun uwu(s: String): String = owofier.translate(s)

    /**
     * Searches for a bot token in order from the following sources:
     * - the `TOKEN` env variable
     * - the `SOMNUS_TOKEN` env variable
     * - the `.token` file in the current working directory
     *
     * @throws IllegalStateException if no suitable token was found.
     */
    private fun readToken(): String {
        // search through env variables first
        System.getenv("TOKEN")?.let { return it }
        System.getenv("SOMNUS_TOKEN")?.let { return it }

        // read from .token if possible
        val tokenFile = Paths.get(System.getProperty("user.dir"), ".token").toFile()
        check(tokenFile.exists()) { "Could not start the bot: missing token!" }
        try {
            return BufferedReader(FileReader(tokenFile)).use { reader -> reader.readLine() }
        } catch (e: IOException) {
            error("Could not start the bot: IOException on reading token!")
        }
    }

    private suspend fun allowOnServer(source: Message) = source.getGuildOrNull()?.id?.let {
        it in config.allowedServers
    } ?: true
}

suspend inline fun Message.respond(content: String) {
    if (this.author != null) {
        this.channel.createMessage(content)
    }
}

suspend inline fun Message.respond(builder: UserMessageCreateBuilder.() -> Unit): Message? {
    return if (this.author != null) {
        this.channel.createMessage(builder)
    } else {
        null
    }
}

fun MessageCreateBuilder.respondPanel(builder: InteractivePanelBuilder.() -> Unit): InteractivePanel {
    val panel = InteractivePanelBuilderImpl().apply(builder).build()
    embed(panel.pages[0])
    actionRow {
        interactionButton(panel.style, "previous") { label = "\uD83E\uDC14" }
        interactionButton(panel.style, "next") { label = "\uD83E\uDC16" }
    }
    return panel
}
