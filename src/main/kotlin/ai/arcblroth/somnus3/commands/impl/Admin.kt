package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.*
import ai.arcblroth.somnus3.data.withPlayerData
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.*
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.request.RestRequestException
import gr.james.sampling.LiLSampling
import kotlinx.coroutines.flow.filter
import java.io.StringReader
import java.util.*
import kotlin.time.Duration

fun CommandRegistry.registerAdminCommands(
    kord: Kord,
    config: Config,
) {
    slash("kill", user = false) {
        description = "Murder."
        options =
            listOf(
                UserOption(
                    name = "victim",
                    description = "Victim of this murder.",
                    optional = true,
                    onParseFailure = ::wrongUserMessage,
                ),
            )
        execute = { author, _, _, options ->
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
        options =
            listOf(
                UserOption(
                    name = "victim",
                    description = "Target of this beaning.",
                    optional = true,
                    onParseFailure = ::wrongUserMessage,
                ),
            )
        execute = { author, _, _, options ->
            withOptionalUserArg(kord, options["victim"] as Snowflake?, author) { victim ->
                withPlayerData(victim.id) {
                    respond {
                        deathMessage(victim, this@withPlayerData, true)
                    }
                }
            }
        }
    }

    slash("randomban") {
        description = "Randomly bans someone from this server."
        options =
            listOf(
                BooleanOption(
                    name = "bots",
                    description = "Should Somnus also ban bots? (false by default)",
                    optional = true,
                ),
            )
        execute = execute@{ author, guild, _, options ->
            if (guild == null) {
                respond {
                    somnusEmbed {
                        color = Constants.ERROR_COLOR
                        title = "${prefix}randomban can only be used in a guild."
                    }
                }
                return@execute
            }

            val executor = guild.getMember(author.id)
            val self = guild.getMember(kord.selfId)

            if (Permission.BanMembers !in executor.getPermissions()) {
                respond {
                    sudoFailMessage(executor)
                }
                return@execute
            }
            if (Permission.BanMembers !in self.getPermissions()) {
                respond {
                    somnusEmbed {
                        color = Constants.ERROR_COLOR
                        title = "Couldn't randomly ban someone"
                        description = "Somnus requires the `Ban Members` permission to execute a ${prefix}randomban."
                    }
                }
                return@execute
            }

            val banBots = options["bots"] as Boolean? ?: false
            val reservoir = LiLSampling<Member>(1)
            var realMemberCount = 0
            try {
                // We use the REST entity supply strategy to ensure that the member list
                // is NOT cached, since we'll immediately remove a member from it below.
                // Note that this requires the GUILD_MEMBERS privileged intent!
                EntitySupplyStrategy.rest
                    .supply(kord)
                    .getGuildMembers(guild.id)
                    .filter { it.id != self.id && if (banBots) true else !it.isBot }
                    .collect {
                        reservoir.feed(it)
                        if (!it.isBot) {
                            realMemberCount++
                        }
                    }
            } catch (e: RestRequestException) {
                respond {
                    somnusEmbed {
                        color = Constants.ERROR_COLOR
                        title =
                            if (e.status.code == 403) {
                                "Somnus requires the `GUILD_MEMBERS` privileged intent to execute a ${prefix}randomban."
                            } else {
                                "Couldn't fetch guild member list"
                            }
                    }
                }
                return@execute
            }
            val victim = reservoir.sample().firstOrNull()

            if (victim == null) {
                respond {
                    somnusEmbed {
                        color = Constants.ERROR_COLOR
                        title = "Nobody is in this server"
                        description = "except for Somnus :("
                    }
                }
            } else if (!victim.isBot && realMemberCount == 1) {
                respond {
                    somnusEmbed {
                        color = Constants.ERROR_COLOR
                        title = "Refusing to ban the last human member!"
                    }
                }
            } else {
                try {
                    guild.ban(victim.id) {
                        reason = "!randomban by ${author.username} (${author.id})"
                        deleteMessageDuration = Duration.ZERO
                    }
                    respond {
                        somnusEmbed {
                            color = Constants.GOOD_COLOR
                            title = "${victim.username} (`${victim.id}`) has been banned!"
                        }
                    }
                } catch (e: RestRequestException) {
                    respond {
                        somnusEmbed {
                            title = "${victim.username} (`${victim.id}`) has been chosen for the ban hammer!"
                            description = "(Somnus doesn't have the permissions to ban this person" +
                                " - <@${guild.ownerId}> will need to swing the hammer themselves.)"
                        }
                    }
                }
            }
        }
    }

    slash("purge") {
        description = "Deletes all messages between the two given messages, inclusive."
        options =
            listOf(
                StringOption(
                    name = "first",
                    description = "First message to delete, inclusive.",
                    onParseFailure = {},
                ),
                StringOption(
                    name = "last",
                    description = "Last message to delete, inclusive.",
                    onParseFailure = {},
                ),
            )
        execute = execute@{ author, guild, channel, options ->
            if (guild == null || author !is Member || author.permissions == null || channel !is GuildMessageChannel) {
                acknowledge(
                    ReactionEmoji.Unicode("❌"),
                    "${prefix}purge can only be used in a guild.",
                )
                return@execute
            }

            val executor = guild.getMember(author.id)
            val self = guild.getMember(kord.selfId)

            if (Permission.ManageMessages !in author.permissions!!) {
                respond {
                    sudoFailMessage(executor)
                }
                return@execute
            }
            if (Permission.ManageMessages !in self.getPermissions()) {
                acknowledge(
                    ReactionEmoji.Unicode("❌"),
                    "Somnus requires the `Manage Messages` permission to execute a ${prefix}purge.",
                )
                return@execute
            }

            val getMessage: suspend (String) -> Message? = { option ->
                val id = Snowflake(options[option] as String)
                try {
                    channel.getMessage(id)
                } catch (_: NumberFormatException) {
                    acknowledge(
                        ReactionEmoji.Unicode("❌"),
                        "`${options[option]}` is not a message id!",
                    )
                    null
                } catch (_: EntityNotFoundException) {
                    acknowledge(
                        ReactionEmoji.Unicode("❌"),
                        "A message with id `$id` was not found in this channel.",
                    )
                    null
                }
            }

            val firstMessage = getMessage("first") ?: return@execute
            val secondMessage = getMessage("last") ?: return@execute

            // If the two messages are the same, just do a normal delete here.
            if (firstMessage.id == secondMessage.id) {
                channel.deleteMessage(firstMessage.id)
                acknowledge(
                    ReactionEmoji.Unicode("✅"),
                    "Purged 1 message (`${firstMessage.id}`)",
                )
                return@execute
            }

            var pivotMessageId = firstMessage.id
            val messagesToDelete = mutableListOf(firstMessage.id)
            var secondMessageFound = false
            var secondMessageGone = false
            while (!secondMessageFound) {
                channel.getMessagesAfter(pivotMessageId, 100).collect { message ->
                    if (pivotMessageId < message.id) {
                        pivotMessageId = message.id
                    }
                    if (message.id <= secondMessage.id) {
                        messagesToDelete.add(message.id)
                        if (message.id == secondMessage.id) {
                            secondMessageFound = true
                        }
                    } else {
                        secondMessageGone = true
                    }
                }
                if (secondMessageGone) {
                    acknowledge(
                        ReactionEmoji.Unicode("❌"),
                        "Could not find last message `${secondMessage.id}`, bailing!",
                    )
                    return@execute
                }
            }
            for (messagesToDeleteChunk in messagesToDelete.chunked(100)) {
                if (messagesToDeleteChunk.size == 1) {
                    channel.deleteMessage(messagesToDeleteChunk.first())
                } else {
                    channel.bulkDelete(messagesToDeleteChunk)
                }
            }
            acknowledge(
                ReactionEmoji.Unicode("✅"),
                "Purged ${messagesToDelete.size} messages",
            )
        }
    }

    text("massrename") {
        options =
            listOf(
                MessageContentOption(
                    name = "channels",
                    description = "Map of channels to rename, in Java's `Properties` file format.",
                ),
            )
        execute = execute@{ author, guild, _, options ->
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
                        is TextChannel ->
                            channel.edit {
                                name = newName
                                reason = renameReason
                            }
                        is VoiceChannel ->
                            channel.edit {
                                name = newName
                                reason = renameReason
                            }
                        is StageChannel ->
                            channel.edit {
                                name = newName
                                reason = renameReason
                            }
                        is NewsChannel ->
                            channel.edit {
                                name = newName
                                reason = renameReason
                            }
                        is Category ->
                            channel.edit {
                                name = newName
                                reason = renameReason
                            }
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

            val succeededChannels =
                if (succeededRenames.isNotEmpty()) {
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
        execute = { author, _, _, _ ->
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
