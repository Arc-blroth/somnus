package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.commands.BooleanOption
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.data.withPreferencesData
import dev.kord.core.Kord

fun CommandRegistry.registerMetaCommands(kord: Kord, config: Config) {
    slash("somnus", "help") {
        description = "son of night, brother of death"
        execute = { _, _, _ ->
            respond {
                somnusEmbed {
                    title = "Somnus (Kotlin)"
                    description = """
                        God of sleep
                        _son of night, brother of death_
                        
                        **Basic Commands**
                        `!help` - Prints this help message.
                        `!stats` - Prints current stats.
                        `!dig` - Spend some sleep points working at Ryancoal Industries and earn minimum wage. You can increase how much you earn through knowledge points.
                        `!ramen` - replenish HP by eating ramen. Ramen costs $18 per cup.
                        `!msg` - Forget the noodles and consume some pure MSG:tm:. Can both heal and hurt you. Costs $45 per pack.
                        `!learn` - Spend 10 sleep points and read some of those textbooks that you've been neglecting. Increases knowledge points.
                        `!game` - Take a break from learning and mining and GAME! Replenishes HP but might kill a few brain cells...
                        `!worship` - Increase your swag levels by praying to the god of destruction of the 47th universe.
                        `!showDeathMessages` - Toggle whether or not to show death messages. On by default.
                    """.trimIndent()
                }
            }
        }
    }

    slash("showdeathmessages") {
        description = "Toggle whether or not to show death messages. On by default."
        options = listOf(
            BooleanOption(
                name = "value",
                description = "Show death messages?",
                optional = true,
            )
        )
        execute = { author, _, options ->
            withPreferencesData(author.id) {
                showDeathMessages = options["value"] as Boolean? ?: !showDeathMessages
                respond {
                    content = "Death messages have been turned **${(if (showDeathMessages) "on" else "off")}**."
                }
            }
        }
    }
}
