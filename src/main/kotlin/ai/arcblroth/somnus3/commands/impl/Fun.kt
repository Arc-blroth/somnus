package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.data.withPlayerData
import dev.kord.core.Kord
import dev.kord.rest.builder.message.EmbedBuilder
import kotlin.math.floor

fun CommandRegistry.registerFunCommands(kord: Kord, config: Config) {
    slash("worship", "simp") {
        description = "This is definitely not a cult."
        execute = { author, guild, _ ->
            withPlayerData(author.id) {
                val random = Math.random()
                val swagGained = floor(random * (Constants.GAME_REWARD + gamePoints) + 1).toInt()
                val sleepLost = floor(random * Constants.WORSHIP_COST + 1).toInt()
                swagPoints += swagGained
                sleepPoints -= sleepLost

                respond {
                    somnusEmbed {
                        val worshipConfig = guild?.let { config.worshipConfig[guild.id] }
                            ?: Config.WorshipConfig(Constants.SWAG_NAME, Constants.SWAG_IMG)
                        thumbnail = EmbedBuilder.Thumbnail().also { it.url = worshipConfig.url }
                        title = "${author.username} worshipped ${worshipConfig.name}"
                        description = "and gained $swagGained swag points at the cost of $sleepLost sleep points."
                    }
                }
            }
        }
    }
}
