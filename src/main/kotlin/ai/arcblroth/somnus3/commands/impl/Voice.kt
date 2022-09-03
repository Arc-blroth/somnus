package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.Somnus
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.commands.StringOption
import dev.kord.core.Kord
import dev.kord.core.entity.ReactionEmoji
import io.ktor.util.logging.*
import org.slf4j.LoggerFactory
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import kotlin.io.path.exists

fun CommandRegistry.registerVoiceCommands(kord: Kord, config: Config, somnus: Somnus) {
    val soundboardLogger = LoggerFactory.getLogger("Soundboard")

    val soundboardConfig = config.soundboardSounds
        .mapValues { (_, value) ->
            try {
                Paths.get(value).let { if (it.exists()) { it } else { null } }
            } catch (e: InvalidPathException) {
                null
            }
        }
        .filterValues { it != null }
        .mapValues { (_, value) -> value!! }

    if (soundboardConfig.isNotEmpty()) {
        slash("soundboard", "sb") {
            description = "Play a sound clip in your current voice channel."
            options = listOf(
                StringOption(
                    name = "sound",
                    description = "Sound clip to play.",
                    choices = soundboardConfig.mapValues { (key, _) -> key },
                    onParseFailure = {
                        somnusEmbed {
                            color = Constants.ERROR_COLOR
                            title = "Unknown voice clip"
                            description = "Note that sound names are case-sensitive."
                        }
                    }
                )
            )
            execute = execute@{ author, guild, options ->
                if (guild == null) {
                    respond {
                        somnusEmbed {
                            color = Constants.ERROR_COLOR
                            title = "${prefix}soundboard can only be used in a guild."
                        }
                    }
                    return@execute
                }

                val clipName = options["sound"] as String
                val clip = soundboardConfig[clipName]
                if (clip == null) {
                    respond {
                        somnusEmbed {
                            color = Constants.ERROR_COLOR
                            title = "Unknown voice clip `$clipName`"
                            description = "Please reload Discord and try again."
                        }
                    }
                    return@execute
                }

                val member = guild.getMember(author.id)
                val voiceChannel = member.getVoiceStateOrNull()?.getChannelOrNull()
                if (voiceChannel == null) {
                    respond {
                        somnusEmbed {
                            color = Constants.ERROR_COLOR
                            title = "Can't find your voice channel!"
                            description = "This might be because you aren't in a voice channel" +
                                " or because the channel is private to Somnus."
                        }
                    }
                    return@execute
                }

                try {
                    somnus.soundboardManager.play(voiceChannel, clip)
                    acknowledge(ReactionEmoji.Unicode("🔊"), "Playing `$clipName`...")
                } catch (e: Exception) {
                    soundboardLogger.error(e)
                    respond {
                        somnusEmbed {
                            color = Constants.ERROR_COLOR
                            title = "Couldn't play sound clip!"
                            description = "`${e.javaClass.name}: ${e.message}`"
                        }
                    }
                }
            }
        }
    }
}
