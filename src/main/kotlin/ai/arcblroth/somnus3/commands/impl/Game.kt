package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.commands.SlashCommandExecutionBuilder
import ai.arcblroth.somnus3.commands.UserOption
import ai.arcblroth.somnus3.data.PlayerData
import ai.arcblroth.somnus3.data.initPlayerData
import ai.arcblroth.somnus3.data.withPlayerData
import ai.arcblroth.somnus3.data.withPreferencesData
import ai.arcblroth.somnus3.respond
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

fun CommandRegistry.registerGameCommands(kord: Kord, config: Config) {
    slash("stats") {
        description = "Query your own or someone else's stats."
        options = listOf(
            UserOption(
                name = "player",
                description = "Target player.",
                optional = true,
                onParseFailure = ::wrongUserMessage
            )
        )
        execute = { author, _, options ->
            withOptionalUserArg(kord, options["player"] as Snowflake?, author) { user ->
                withPlayerData(user.id) {
                    respond {
                        somnusEmbed(thumbnailUser = user) {
                            title = "${user.username}'s Stats"
                            field("Sleep Points", true) {
                                "${if (sleepPoints > 0) ":zzz:" else ":warning:"} $sleepPoints"
                            }
                            field("Knowledge Points", false) { ":brain: $knowledgePoints" }
                            field("Money", true) { ":money_with_wings: \$$moneyPoints" }
                            field("HP", true) { ":heart: $hitPoints" }
                            field("Gaming Sessions", false) { ":video_game: $gamePoints" }
                            field("Swag Power", false) { ":sunglasses: $swagPoints" }
                        }
                    }
                }
            }
        }
    }

    slash("dig", "mine") {
        description = "Spend some sleep points working at Ryancoal Industries and earn minimum wage."
        execute = { author, _, _ ->
            withPlayerData(author.id) {
                val random = Math.random()
                val coalFound = floor(random * Constants.DIG_REWARD_MULT + 1).toInt()
                val knowledgeBonus = knowledgePoints * Constants.DIG_LEARN_MULTI
                val actualCoalFound = floor(random * (Constants.DIG_REWARD_MULT + knowledgeBonus) + 1).toInt()
                sleepPoints -= Constants.DIG_COST
                moneyPoints += actualCoalFound

                respond {
                    somnusEmbed(thumbnailUser = author) {
                        title = "${author.username} went mining"
                        description = "and found $coalFound pieces of coal, worth \$$actualCoalFound"
                    }
                }
            }
        }
    }

    slash("ramen") {
        description = "Replenish HP by eating ramen. Ramen costs \$18 per cup."
        execute = { author, _, _ ->
            withPlayerData(author.id) {
                if (moneyPoints < Constants.RAMEN_COST) {
                    respond {
                        poorMessage(author, "ramen", Constants.RAMEN_COST)
                    }
                } else {
                    val random = Math.random()
                    val hpGained = floor(random * (Constants.RAMEN_REWARD_MAX - 1) + 1).toInt()
                    hitPoints += hpGained
                    moneyPoints -= Constants.RAMEN_COST

                    respond {
                        somnusEmbed(thumbnailUser = author) {
                            title = "${author.username} ate some ramen :ramen:"
                            description = "and gained $hpGained HP!"
                        }
                    }
                }
            }
        }
    }

    slash("msg") {
        description = "Forget the noodles and consume some pure MSG™. Can both heal and hurt you. Costs \$45 per pack."
        execute = { author, _, _ ->
            withPlayerData(author.id) {
                if (moneyPoints < Constants.MSG_COST) {
                    respond {
                        poorMessage(author, "msg", Constants.MSG_COST)
                    }
                } else {
                    val random = Math.random()
                    val hpGained = (random * (Constants.MSG_REWARD_MAX - Constants.MSG_REWARD_MIN) + Constants.MSG_REWARD_MIN).roundToInt()
                    hitPoints += hpGained
                    moneyPoints -= Constants.MSG_COST

                    respond {
                        somnusEmbed(thumbnailUser = author) {
                            title = "${author.username} snorted some pure MSG  :fog:"
                            description = "and ${if (hpGained < 0) "lost" else "gained"} $hpGained HP!"
                        }
                    }
                }
            }
        }
    }

    slash("learn") {
        description = "Spend 10 sleep points and read some of those textbooks that you've been neglecting."
        execute = { author, _, _ ->
            withPlayerData(author.id) {
                val random = Math.random()
                val kpGained = floor(random * (Constants.LEARN_REWARD_MAX - 1) + 1).toInt()
                knowledgePoints += kpGained
                sleepPoints -= Constants.LEARN_COST

                respond {
                    somnusEmbed(thumbnailUser = author) {
                        title = "${author.username} did some learning  :book:"
                        description = "and rejuvenated $kpGained brain cells."
                    }
                }
            }
        }
    }

    slash("game") {
        description = "Take a break from learning and mining and GAME! Replenishes HP but might kill a few brain cells..."
        execute = { author, _, _ ->
            withPlayerData(author.id) {
                val random = Math.random()
                val hpGained = floor(random * Constants.GAME_REWARD + 1).toInt()
                val knowledgeLost = floor(random * Constants.GAME_COST + 1).toInt()
                knowledgePoints -= knowledgeLost
                hitPoints += hpGained
                gamePoints += 1

                respond {
                    somnusEmbed {
                        thumbnail { url = Constants.GAME_IMG }
                        title = "${author.username} gamed for a while"
                        description = "and gained $hpGained hp at the cost of $knowledgeLost brain cells."
                    }
                }
            }
        }
    }

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
                        thumbnail { url = worshipConfig.url }
                        title = "${author.username} worshipped ${worshipConfig.name}"
                        description = "and gained $swagGained swag points at the cost of $sleepLost sleep points."
                    }
                }
            }
        }
    }
}

suspend fun update(msg: Message, author: User) {
    var response: (MessageCreateBuilder.() -> Unit)? = null
    val context = object : SlashCommandExecutionBuilder {
        override fun respond(builder: MessageCreateBuilder.() -> Unit) {
            response = builder
        }
    }

    with(context) {
        withPlayerData(author.id) {
            val now = System.currentTimeMillis()
            val daysSince = floor((now - lastDailyRewardTime) / 86400000.0).toInt()
            if (daysSince > 0) {
                // Update sleep points
                sleepPoints += daysSince * Constants.SLEEP_POINTS_PER_DAY
                lastDailyRewardTime += daysSince * 86400000L

                // 1 hp of damage is taken a day.
                hitPoints -= daysSince * Constants.DAMAGE_PER_DAY

                // If missing sleep you take another 2 points of damage.
                if (sleepPoints < 0) {
                    hitPoints -= daysSince * Constants.DAMAGE_PER_DAY * abs(sleepPoints / 2)
                }

                // Did the player just die?
                if (hitPoints <= 0) {
                    onDeath(author, this, true)
                }
            }
        }
    }

    if (response != null) {
        msg.respond(response!!)
    }
}

fun SlashCommandExecutionBuilder.onDeath(victim: User, data: PlayerData, allowSuppress: Boolean) {
    var showMessage = true
    if (allowSuppress) {
        withPreferencesData(victim.id) {
            showMessage = showDeathMessages
        }
    }

    if (showMessage) {
        respond {
            deathMessage(victim, data, false)
        }
    }

    data.initPlayerData()
}
