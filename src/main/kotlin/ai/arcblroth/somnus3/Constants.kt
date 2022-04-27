package ai.arcblroth.somnus3

import dev.kord.common.Color

object Constants {
    // Colors
    val COLOR = Color(0xf0eda3)
    val GOOD_COLOR = Color(0x31f766)
    val ERROR_COLOR = Color(0xf54242)

    // Text
    const val FOOTER = "Somnus by Arc'blroth and Drshadoww"
    const val FIRE_DISCLAIMER_FOOTER = "Data provided by CalFire. May not be up-to-date."
    const val BLANK_FIELD = "\u200b"

    // Message Filters
    val MENTION_FILTER = Regex("<@[!&]?\\d*?>")
    val CHANNEL_FILTER = Regex("<#\\d*?>")
    val EMOJI_FILTER = Regex("<:.*?:\\d*?>")
    val TIMESTAMP_FILTER = Regex("<t:\\d*?(:.)?>")
    val PROPERTIES_REGEX = Regex("```(properties)?\\n((.|\\n)*)\\n```")

    // Game Settings
    const val SLEEP_POINTS_PER_DAY = 8.0
    const val DAMAGE_PER_DAY = 1.0

    const val DIG_COST = 1.0
    const val DIG_REWARD_MULT = 20.0
    const val DIG_LEARN_MULTI = 0.1
    const val LEARN_COST = 10.0
    const val LEARN_REWARD_MAX = 5.0

    const val RAMEN_COST = 18.0
    const val RAMEN_REWARD_MAX = 2.0
    const val MSG_COST = 45.0
    const val MSG_REWARD_MIN = -2.0
    const val MSG_REWARD_MAX = 4.0

    const val GAME_COST = 6.0
    const val GAME_REWARD = 4.0
    const val GAME_IMG = "https://media.discordapp.net/attachments/617461750203416576/700757068130418739/unknown.png"

    const val WORSHIP_COST = 7.0
    const val SWAG_IMG = "https://media.discordapp.net/attachments/756279756374409236/798971096996642816/image0.jpg"
}
