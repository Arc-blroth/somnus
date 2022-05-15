package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.UserOption
import ai.arcblroth.somnus3.data.PlayerData
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
        footer { text = Constants.FOOTER }
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

/**
 * Error message for not having enough money to buy an item.
 */
fun MessageCreateBuilder.poorMessage(author: User, type: String, cost: Int) {
    somnusEmbed {
        title = "${author.username} doesn't have enough money to buy $type"
        description = "That costs \$$cost."
    }
}

/**
 * Error message for not being in the sudoers list.
 */
fun MessageCreateBuilder.sudoFailMessage(author: User) {
    somnusEmbed(thumbnailUser = author) {
        color = Constants.ERROR_COLOR
        title = "${author.username} is not in the sudoers file."
        description = "This incident will be reported."
        footer { text = "https://xkcd.com/838/" }
    }
}

/**
 * Message shown on death.
 */
fun MessageCreateBuilder.deathMessage(author: User, data: PlayerData, bean: Boolean) {
    somnusEmbed(thumbnailUser = author) {
        title = "${author.username} has Died${if (bean) "?" else "!"}"
        field("Stats") {
            """
                :zzz: ${data.sleepPoints}
                :brain: ${data.knowledgePoints}
                :money_with_wings: ${data.moneyPoints}
                :video_game: ${data.gamePoints}
                :sunglasses: ${data.swagPoints}
            """.trimIndent()
        }
    }
}
