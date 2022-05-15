package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.UserOption
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock

/**
 * Adds an embed to this message in the standardized format used for most of Somnus' functions.
 */
fun MessageCreateBuilder.somnusEmbed(thumbnailUser: User? = null, block: EmbedBuilder.() -> Unit) {
    embed {
        color = Constants.COLOR
        thumbnail = thumbnailUser?.avatar?.url?.let { url -> EmbedBuilder.Thumbnail().also { it.url = url } }
        footer = EmbedBuilder.Footer().also { it.text = Constants.FOOTER }
        timestamp = Clock.System.now()

        block()

        field {
            name = Constants.BLANK_FIELD
            value = Constants.BLANK_FIELD
            inline = false
        }
    }
}

/**
 * Error message for malformed [UserOption]s.
 */
fun wrongUserMessage(builder: MessageCreateBuilder, target: String) {
    builder.somnusEmbed {
        color = Constants.ERROR_COLOR
        title = "Could not find target user."
        description = "Did you perhaps copy the wrong id? (got snowflake `$target`)"
    }
}

/**
 * Error message for malformed [UserOption]s.
 */
@JvmName("wrongUserMessageExt")
fun MessageCreateBuilder.wrongUserMessage(target: String) = wrongUserMessage(this, target)
