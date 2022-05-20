package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.*
import ai.arcblroth.somnus3.data.withPlayerData
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.*
import java.io.StringReader
import java.util.*

fun CommandRegistry.registerAdminCommands(kord: Kord, config: Config) {
    slash("kill", user = false) {
        description = "Murder."
        options = listOf(
            UserOption(
                name = "victim",
                description = "Victim of this murder.",
                optional = true,
                onParseFailure = ::wrongUserMessage
            )
        )
        execute = { author, _, options ->
            withOptionalUserArg(kord, options["victim"] as Snowflake?, author) { victim ->
                // check permissions
                if (author.id == victim.id || victim.id in config.noSudoRequiredKillVictims || author.id in config.sudoers) {
                    withPlayerData(victim.id) {
                        onDeath(victim, this, false)
                    }
                } else {
                    respond {
                        sudoFailMessage(author)
                    }
                }
            }
        }
    }

    slash("bean") {
        description = "What happened with the bois at 3AM last night."
        options = listOf(
            UserOption(
                name = "victim",
                description = "Target of this beaning.",
                optional = true,
                onParseFailure = ::wrongUserMessage
            )
        )
        execute = { author, _, options ->
            withOptionalUserArg(kord, options["victim"] as Snowflake?, author) { victim ->
                withPlayerData(victim.id) {
                    respond {
                        deathMessage(victim, this@withPlayerData, true)
                    }
                }
            }
        }
    }

    text("massrename") {
        options = listOf(
            MessageContentOption(
                name = "channels",
                description = "Map of channels to rename, in Java's `Properties` file format."
            )
        )
        execute = execute@{ author, guild, options ->
            if (guild == null) {
                respond {
                    somnusEmbed {
                        color = Constants.ERROR_COLOR
                        title = "!massrename can only be used in a guild."
                    }
                }
                return@execute
            }

            val member = guild.getMember(author.id)
            val self = guild.getMember(kord.selfId)

            if (Permission.ManageChannels !in member.getPermissions()) {
                respond {
                    sudoFailMessage(member)
                }
                return@execute
            }
            if (Permission.ManageChannels !in self.getPermissions()) {
                respond {
                    somnusEmbed {
                        color = Constants.ERROR_COLOR
                        title = "Couldn't rename channels"
                        description = "Somnus requires the `Manage Channels` permission to execute a !massrename."
                    }
                }
                return@execute
            }

            val channels = Constants.PROPERTIES_REGEX.find(options["channels"] as String)
            if (channels == null || channels.groups[2] == null) {
                respond {
                    somnusEmbed {
                        color = Constants.ERROR_COLOR
                        title = "Couldn't find mass rename map"
                        field("Usage", false) { "\\`\\`\\`properties\n<channel id>=new-name\n\\`\\`\\`" }
                    }
                }
                return@execute
            }

            val properties = Properties()
            try {
                properties.load(StringReader(channels.groups[2]!!.value))
            } catch (_: Exception) {
                respond {
                    somnusEmbed {
                        color = Constants.ERROR_COLOR
                        title = "Couldn't parse mass rename map"
                        field("Usage", false) { "\\`\\`\\`properties\n<channel id>=new-name\n\\`\\`\\`" }
                    }
                }
                return@execute
            }

            val renameReason = "!massrename by ${author.username} (${author.id})"
            var succeeded = true
            val succeededRenames = mutableListOf<Snowflake>()

            try {
                for ((key, value) in properties) {
                    val id = Snowflake(key as String)
                    val newName = value as String
                    when (val channel = kord.getChannel(id)) {
                        is TextChannel -> channel.edit { name = newName; reason = renameReason }
                        is VoiceChannel -> channel.edit { name = newName; reason = renameReason }
                        is StageChannel -> channel.edit { name = newName; reason = renameReason }
                        is NewsChannel -> channel.edit { name = newName; reason = renameReason }
                        is Category -> channel.edit { name = newName; reason = renameReason }
                        else -> {
                            succeeded = false
                            continue
                        }
                    }
                    succeededRenames.add(id)
                }
            } catch (_: Exception) {
                succeeded = false
            }

            val succeededChannels = if (succeededRenames.isNotEmpty()) {
                succeededRenames.joinToString("\n") { "<#$it>" }
            } else {
                "None :("
            }
            respond {
                somnusEmbed {
                    if (succeeded) {
                        color = Constants.GOOD_COLOR
                        title = "Renamed all channels!"
                        field("Succeeded", false) { succeededChannels }
                    } else {
                        color = Constants.ERROR_COLOR
                        title = "Couldn't rename all channels"
                        field("Succeeded Renames", false) { succeededChannels }
                    }
                }
            }
        }
    }

    var lastTaskkillTime: Long = 0
    text("taskkill", "yeet") {
        execute = { author, _, _ ->
            if (author.id in config.sudoers) {
                if (System.currentTimeMillis() - lastTaskkillTime < 5000) {
                    (this as TextBasedSlashCommandExecutionBuilder).message.addReaction(ReactionEmoji.Unicode("✅"))
                    // note that this immediately cancels the current coroutine,
                    // meaning that respond { } wouldn't actually send a message here
                    kord.shutdown()
                } else {
                    lastTaskkillTime = System.currentTimeMillis()
                    respond {
                        somnusEmbed {
                            color = Constants.ERROR_COLOR
                            title = "Execute Taskkill?"
                            description = "Type !taskkill again within 5 seconds to yeet the bot."
                        }
                    }
                }
            } else {
                respond {
                    sudoFailMessage(author)
                }
            }
        }
    }
}
