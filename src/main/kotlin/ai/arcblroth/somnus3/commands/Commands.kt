package ai.arcblroth.somnus3.commands

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Constants
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock

class Commands internal constructor(private val registry: CommandRegistry) {
    suspend fun handleMessage(message: Message, author: User, commandPrefix: String, tokens: List<String>) {
        registry.handleMessage(message, author, commandPrefix, tokens)
    }
}

suspend fun registerCommandCallbacks(kord: Kord, config: Config): Commands {
    val registry = CommandRegistry.registerCommands(kord, config) {
        slash {
            name = "bean"
            description = "What happened with the bois at 3AM last night."
            options = listOf(
                UserOption(
                    name = "victim",
                    description = "Target of this beaning.",
                    onParseFailure = {
                        content = "Could not find target user."
                    }
                )
            )
            execute = { _, options ->
                val victim = kord.getUser(options["victim"] as Snowflake)
                if (victim != null) {
                    respond {
                        embed {
                            color = Constants.COLOR
                            thumbnail = victim.avatar?.url?.let { url -> EmbedBuilder.Thumbnail().also { it.url = url } }
                            title = "${victim.username} has Died?"
                            field {
                                name = Constants.BLANK_FIELD
                                value = Constants.BLANK_FIELD
                                inline = false
                            }
                            footer = EmbedBuilder.Footer().also { it.text = Constants.FOOTER }
                            timestamp = Clock.System.now()
                        }
                    }
                }
            }
        }
    }
    return Commands(registry)
}
