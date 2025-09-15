package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.commands.SlashCommandExecutionBuilder
import ai.arcblroth.somnus3.commands.UserOption
import ai.arcblroth.somnus3.data.*
import ai.arcblroth.somnus3.panel.InteractivePanelBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import kotlin.math.abs
import kotlin.math.floor

fun CommandRegistry.registerGameCommands(
    kord: Kord,
    config: Config,
) {
    slash("stats") {
        description = "Query your own or someone else's stats."
        options =
            listOf(
                UserOption(
                    name = "player",
                    description = "Target player.",
                    optional = true,
                    onParseFailure = ::wrongUserMessage,
                ),
            )
        execute = { author, _, options ->
            withOptionalUserArg(kord, options["player"] as Snowflake?, author) { user ->
                withPlayerData(user.id) {
                    withAngelData(user.id) {
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
                                field("Swag Power", true) { ":sunglasses: $swagPoints" }
                                field("Furryness", true) { ":chipmunk: ${furryPoints.toInt()}" }
                                field("Current Bed", false) { ":bed: ${bedType.uiName}" }
                                field("Sleep Angel", false) { "${angelType.uiEmoji} ${angelType.uiName}" }
                            }
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
                val furryBonus = floor(furryPoints)
                val actualCoalFound =
                    withAngelData(author.id) {
                        applyPowerEffects(random * digModifier * (Constants.DIG_REWARD_MULT + knowledgeBonus + furryBonus) + 1)
                    }
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
                    val hpGained =
                        withAngelData(author.id) {
                            applyPowerEffects(random * eatModifier * (Constants.RAMEN_REWARD_MAX - 1) + 1)
                        }
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
                    val hpGained =
                        withAngelData(author.id) {
                            applyPowerEffects(
                                random * eatModifier * (Constants.MSG_REWARD_MAX - Constants.MSG_REWARD_MIN) + Constants.MSG_REWARD_MIN,
                                round = true,
                            )
                        }
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
                val maxKp =
                    Constants.LEARN_REWARD_MAX +
                        if (bedType == BedType.COTTON) {
                            3
                        } else {
                            0
                        }
                val kpGained =
                    withAngelData(author.id) {
                        applyPowerEffects(random * learnModifier * (maxKp - 1) + 1)
                    }
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
                val baseReward =
                    Constants.GAME_REWARD +
                        if (bedType == BedType.POLY) {
                            2
                        } else {
                            0
                        }
                val hpGained =
                    withAngelData(author.id) {
                        applyPowerEffects(random * gameModifier * baseReward + 1)
                    }
                val knowledgeLost = floor(random * Constants.GAME_COST + 1).toInt()
                knowledgePoints -= knowledgeLost
                hitPoints += hpGained
                gamePoints +=
                    if (bedType == BedType.POLY) {
                        2
                    } else {
                        1
                    }

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
                val baseReward =
                    Constants.GAME_REWARD +
                        if (bedType == BedType.SILK) {
                            4
                        } else {
                            0
                        }
                val swagGained =
                    withAngelData(author.id) {
                        applyPowerEffects(random * worshipModifier * (baseReward + gamePoints) + 1)
                    }
                val sleepLost = floor(random * Constants.WORSHIP_COST + 1).toInt()
                swagPoints += swagGained
                sleepPoints -= sleepLost

                val worshipConfig =
                    guild?.let { config.worshipConfig[guild.id] }
                        ?: Config.WorshipConfig(Constants.SWAG_NAME, Constants.SWAG_IMG)

                if (worshipConfig.furry) {
                    if (Math.random() < 0.5) {
                        furryPoints += 1.0
                    }
                }

                respond {
                    somnusEmbed {
                        thumbnail { url = worshipConfig.url }
                        title = "${author.username} worshipped ${worshipConfig.name}"
                        description = "and gained $swagGained swag points at the cost of $sleepLost sleep points."
                    }
                }
            }
        }
    }
}

suspend fun update(
    channel: MessageChannelBehavior,
    author: User,
) {
    val response = UserMessageCreateBuilder()
    var hasResponse = false
    val context =
        object : SlashCommandExecutionBuilder {
            override fun respond(builder: MessageCreateBuilder.() -> Unit) {
                hasResponse = true
                response.apply(builder)
            }

            override fun respondPanel(builder: InteractivePanelBuilder.() -> Unit) = throw NotImplementedError()

            override fun acknowledge(
                emoji: ReactionEmoji,
                message: String,
            ) = throw NotImplementedError()
        }

    with(context) {
        withPlayerData(author.id) {
            val now = System.currentTimeMillis()
            val daysSince = floor((now - lastDailyRewardTime) / 86400000.0).toInt()
            if (daysSince > 0) {
                withAngelData(author.id) {
                    // Update sleep points
                    val baseSleep = Constants.SLEEP_POINTS_PER_DAY + bedType.ordinal * Constants.SLEEP_POINTS_PER_BED_TIER
                    sleepPoints += daysSince * (angelType.sleepModifier * baseSleep).toInt()
                    lastDailyRewardTime += daysSince * 86400000L

                    // 1 hp of damage is taken a day.
                    val actualDamagePerDay = angelType.hpDamageModifier * Constants.DAMAGE_PER_DAY
                    hitPoints -= daysSince * actualDamagePerDay

                    // If missing sleep you take another 2 points of damage.
                    if (sleepPoints < 0) {
                        hitPoints -= daysSince * actualDamagePerDay * abs(sleepPoints / 2)
                    }
                }

                // Bed effects
                if (bedType == BedType.STRAW) {
                    furryPoints += 0.2
                }

                // Did the player just die?
                if (hitPoints <= 0) {
                    onDeath(author, this, true)
                }
            }
        }
    }

    if (hasResponse) {
        channel.kord.rest.channel
            .createMessage(channel.id, response.toRequest())
    }
}

fun SlashCommandExecutionBuilder.onDeath(
    victim: User,
    data: PlayerData,
    allowSuppress: Boolean,
) {
    val showMessage =
        if (allowSuppress) {
            withPreferencesData(victim.id) {
                showDeathMessages
            }
        } else {
            true
        }

    if (showMessage) {
        respond {
            deathMessage(victim, data, false)
        }
    }

    data.initPlayerData()
    data.hitPoints += (-4..4).random()
}
