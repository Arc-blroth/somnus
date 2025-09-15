package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.UserOption
import ai.arcblroth.somnus3.data.AngelData
import ai.arcblroth.somnus3.data.PlayerData
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import kotlinx.datetime.Clock
import java.util.stream.Collectors

/**
 * Adds an embed to this message in the standardized format used for most of Somnus' functions.
 */
fun MessageCreateBuilder.somnusEmbed(
    thumbnailUser: User? = null,
    block: EmbedBuilder.() -> Unit,
) {
    embed {
        color = Constants.COLOR
        thumbnail = thumbnailUser?.avatar?.cdnUrl?.let { url -> EmbedBuilder.Thumbnail().also { it.url = url.toUrl() } }
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
fun wrongUserMessage(
    builder: MessageCreateBuilder,
    target: String,
) {
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
fun MessageCreateBuilder.poorMessage(
    author: User,
    type: String,
    cost: Int,
) {
    somnusEmbed {
        color = Constants.ERROR_COLOR
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
fun MessageCreateBuilder.deathMessage(
    author: User,
    data: PlayerData,
    bean: Boolean,
) {
    somnusEmbed(thumbnailUser = author) {
        title = "${author.username} has Died${if (bean) "?" else "!"}"
        field("Stats") {
            """
            :zzz: ${data.sleepPoints}
            :brain: ${data.knowledgePoints}
            :money_with_wings: ${data.moneyPoints}
            :video_game: ${data.gamePoints}
            :sunglasses: ${data.swagPoints}
            :chipmunk: ${data.furryPoints.toInt()}
            :bed: ${data.bedType.uiName}
            """.trimIndent()
        }
    }
}

/**
 * Embed content for reporting sleep angel stats.
 */
fun EmbedBuilder.angelStats(data: AngelData) {
    with(data) {
        color = angelType.rarity.color

        thumbnail {
            url =
                angelType.uiEmoji.codePoints().mapToObj { it.toString(16).lowercase() }.collect(
                    Collectors.joining("-", "https://raw.githubusercontent.com/twitter/twemoji/master/assets/72x72/", ".png"),
                )
        }

        description =
            if (angelType.hpDamageModifier == 1) {
                angelType.flavor
            } else {
                angelType.flavor + "\n**WARNING**: This angel increases the amount of HP you lose per day!"
            }

        field("Sleep", false) { formatModifier(angelType.sleepModifier) }
        if (angelType.hpDamageModifier != 1) {
            field("HP Loss / Day", false) { String.format("%+d", -angelType.hpDamageModifier) }
        }

        field("Digging", true) { formatModifier(digModifier) }
        field("Eating", true) { formatModifier(eatModifier) }
        field("Learning", true) { formatModifier(learnModifier) }
        field("Gaming", true) { formatModifier(gameModifier) }
        field("Worshipping", true) { formatModifier(worshipModifier) }
    }
}

fun formatModifier(modifier: Double) = String.format("%+.0f%%", (modifier - 1.0) * 100)

fun formatRarity(rarity: Double) = String.format("%.2f%%", rarity * 100)
