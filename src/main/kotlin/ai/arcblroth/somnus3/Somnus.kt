package ai.arcblroth.somnus3

import dev.kord.common.entity.ActivityType
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.gateway.builder.PresenceBuilder
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.nio.file.Paths

class Somnus(val config: Config) {
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

    private fun registerCallbacks(kord: Kord) {
        kord.on<MessageCreateEvent> {
            val author = message.author
            if (author != null && allowOnServer(message)) {
                val command = message.content
                val tokens = command.trim().split(Regex("\\s+?"))
                handleWittyResponses(message, author, command, tokens)
            }
        }
    }

    private suspend fun handleWittyResponses(message: Message, author: User, command: String, tokens: List<String>) {
        val strippedCommand = command
            .replace(Constants.MENTION_FILTER, "")
            .replace(Constants.CHANNEL_FILTER, "")
            .replace(Constants.EMOJI_FILTER, "")
            .replace(Constants.TIMESTAMP_FILTER, "")

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

suspend inline fun Message.respond(builder: UserMessageCreateBuilder.() -> Unit) {
    if (this.author != null) {
        this.channel.createMessage(builder)
    }
}
