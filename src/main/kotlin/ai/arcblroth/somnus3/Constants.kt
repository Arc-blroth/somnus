package ai.arcblroth.somnus3

import dev.kord.common.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

object Constants {
    // Colors
    val COLOR = Color(0xf0eda3)
    val GOOD_COLOR = Color(0x31f766)
    val ERROR_COLOR = Color(0xf54242)

    // Text
    const val PREFIX = "!"
    const val FOOTER = "Somnus by Arc'blroth and Drshadoww"
    const val FIRE_DISCLAIMER_FOOTER = "Data provided by CalFire. May not be up-to-date."
    const val BLANK_FIELD = "\u200b"

    // Message Filters
    val MENTION_FILTER = Regex("<@([!&]?)(\\d*?)>")
    val CHANNEL_FILTER = Regex("<#(\\d*?)>")
    val EMOJI_FILTER = Regex("<:.*?:\\d*?>")
    val TIMESTAMP_FILTER = Regex("<t:\\d*?(:.)?>")
    val PROPERTIES_REGEX = Regex("```(properties)?\\n((.|\\n)*)\\n```")

    // Game Settings
    const val SLEEP_POINTS_PER_DAY = 8
    const val SLEEP_POINTS_PER_BED_TIER = 4
    const val DAMAGE_PER_DAY = 1

    const val DIG_COST = 1
    const val DIG_REWARD_MULT = 20
    const val DIG_LEARN_MULTI = 0.1
    const val LEARN_COST = 10
    const val LEARN_REWARD_MAX = 5

    const val RAMEN_COST = 18
    const val RAMEN_REWARD_MAX = 2
    const val MSG_COST = 45
    const val MSG_REWARD_MIN = -2
    const val MSG_REWARD_MAX = 4

    const val GAME_COST = 6
    const val GAME_REWARD = 4
    const val GAME_IMG = "https://media.discordapp.net/attachments/617461750203416576/700757068130418739/unknown.png"

    const val WORSHIP_COST = 7
    const val SWAG_NAME = "Sr. Gomez"
    const val SWAG_IMG = "https://media.discordapp.net/attachments/756279756374409236/798971096996642816/image0.jpg"

    const val SUMMON_MONEY_COST = 1000
    const val SUMMON_SWAG_COST = 100

    // Data Handling
    @OptIn(ExperimentalSerializationApi::class)
    val lenientJson =
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
}
