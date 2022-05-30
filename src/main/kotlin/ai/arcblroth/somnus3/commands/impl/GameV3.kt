package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.commands.StringOption
import ai.arcblroth.somnus3.commands.UserOption
import ai.arcblroth.somnus3.data.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord

// Game commands added as part of the Somnus v3 update.
fun CommandRegistry.registerGameV3Commands(kord: Kord, config: Config) {
    slash("bed") {
        description = "Browse our selection of fine and magical beds."
        options = listOf(
            StringOption(
                name = "purchase",
                description = "Bed type to purchase.",
                choices = BedType.values().associate { it.uiName to it.name },
                optional = true,
                onParseFailure = {
                    somnusEmbed {
                        title = "We don't have that bed in stock!"
                        description = "Note that bed names are case-sensitive."
                    }
                }
            )
        )
        execute = { author, _, options ->
            withPlayerData(author.id) {
                val purchaseType = options["purchase"] as String?
                if (purchaseType == null) {
                    respond {
                        somnusEmbed {
                            title = "Mystical Beds & Co. Catalogue"
                            description = "Use `${prefix}bed <name>` to purchase one of our lovely beds."
                            BedType.values().forEach {
                                field {
                                    name = "${if (bedType == it) { ":star:" } else { "$${it.cost}" }} - ${it.uiName}"
                                    value = "${it.perks?.let { "**$it**\n" } ?: ""}${it.description}"
                                    inline = false
                                }
                            }
                        }
                    }
                } else {
                    val purchase = BedType.valueOf(purchaseType)
                    if (purchase == BedType.FLOOR) {
                        respond {
                            somnusEmbed {
                                color = Constants.ERROR_COLOR
                                title = "why are you buying the floor"
                                description = "it's literally free"
                            }
                        }
                    } else if (bedType == purchase) {
                        respond {
                            somnusEmbed {
                                color = Constants.ERROR_COLOR
                                title = "You already own a ${bedType.uiName}!"
                            }
                        }
                    } else if (bedType != BedType.FLOOR) {
                        respond {
                            somnusEmbed {
                                color = Constants.ERROR_COLOR
                                title = "Oh dear!"
                                description = "It looks like you already have a bed at your house!\n" +
                                    "If only there was some way to get rid of it..."
                            }
                        }
                    } else if (moneyPoints < purchase.cost) {
                        respond {
                            poorMessage(author, "a ${purchase.uiName}", purchase.cost)
                        }
                    } else {
                        moneyPoints -= purchase.cost
                        bedType = purchase
                        respond {
                            somnusEmbed {
                                color = Constants.GOOD_COLOR
                                title = "Thank you for your purchase!"
                                description = "You have bought a ${purchase.uiName} for only $${purchase.cost}."
                            }
                        }
                    }
                }
            }
        }
    }

    slash("wet", "pee") {
        description = "?"
        execute = { author, _, _ ->
            withPlayerData(author.id) {
                if (Math.random() < 0.5) {
                    furryPoints += 1.0
                }
                if (bedType == BedType.FLOOR) {
                    respond {
                        somnusEmbed {
                            title = "The floor is now stinkyer."
                        }
                    }
                } else {
                    respond {
                        somnusEmbed {
                            color = Constants.ERROR_COLOR
                            title = "Uh oh!"
                            description = "Your ${bedType.uiName} is now ruined!"
                        }
                    }
                    bedType = BedType.FLOOR
                }
            }
        }
    }

    slash("angel") {
        description = "Query your own or someone else's sleep angel."
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
                withAngelData(user.id) {
                    respond {
                        somnusEmbed(thumbnailUser = user) {
                            title = "${user.username}'s [${angelType.rarity.name}] ${angelType.uiName} Angel"
                            angelStats(this@withAngelData)
                        }
                    }
                }
            }
        }
    }

    slash("summonrates") {
        description = "Rarity information for summoning angels."
        execute = { _, _, _ ->
            respond {
                somnusEmbed {
                    AngelType.values().forEach {
                        if (it != AngelType.LUNA) {
                            title = "${prefix}summon Rates"
                            field {
                                name = "`${formatRarity(it.summonRate)}` - [${it.rarity.name}] ${it.uiName}"
                                value = "${it.uiEmoji} ${it.flavor}"
                                inline = false
                            }
                        }
                    }
                }
            }
        }
    }

    slash("summon") {
        description = "Summon a new sleep angel. Costs $${Constants.SUMMON_MONEY_COST} and ${Constants.SUMMON_SWAG_COST} swag points."
        execute = { author, _, _ ->
            withPlayerData(author.id) {
                withAngelData(author.id) {
                    if (moneyPoints < Constants.SUMMON_MONEY_COST) {
                        respond {
                            poorMessage(author, "summoning materials", Constants.SUMMON_MONEY_COST)
                        }
                    } else if (swagPoints < Constants.SUMMON_SWAG_COST) {
                        respond {
                            somnusEmbed {
                                color = Constants.ERROR_COLOR
                                title = "${author.username} doesn't have enough swag to summon a new sleep angel"
                                description = "At least ${Constants.SUMMON_SWAG_COST} swag points are required."
                            }
                        }
                    } else {
                        moneyPoints -= Constants.SUMMON_MONEY_COST
                        swagPoints -= Constants.SUMMON_SWAG_COST
                        summonAngel()

                        // If the player had at least twice the required money OR swag before summoning,
                        // we do a second hidden reroll, taking the better of the two rolls.
                        // Angels may find players with more money or faith a better match for themselves...
                        if (moneyPoints >= Constants.SUMMON_MONEY_COST || swagPoints >= Constants.SUMMON_SWAG_COST) {
                            val rerolledType = AngelType.summonType()
                            if (rerolledType.rarity.ordinal >= angelType.rarity.ordinal) {
                                summonAngel(rerolledType)
                            }
                        }

                        respond {
                            somnusEmbed {
                                title = "${author.username} has summoned a [${angelType.rarity.name}] ${angelType.uiName} Angel!"
                                angelStats(this@withAngelData)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun AngelData.summonAngel(type: AngelType = AngelType.summonType()) {
    angelType = type
    type.fillAngelData(this)
}
