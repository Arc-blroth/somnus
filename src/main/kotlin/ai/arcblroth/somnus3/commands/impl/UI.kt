package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Constants
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
