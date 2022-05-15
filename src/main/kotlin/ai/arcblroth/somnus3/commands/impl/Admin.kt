package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.commands.UserOption
import ai.arcblroth.somnus3.data.withPlayerData
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord

fun CommandRegistry.registerAdminCommands(kord: Kord, config: Config) {
    slash("kill", user = false) {
        description = "Murder."
        options = listOf(
            UserOption(
                name = "victim",
                description = "Victim of this murder.",
                optional = true,
                onParseFailure = ::wrongUserMessage
            )
        )
        execute = { author, _, options ->
            withOptionalUserArg(kord, options["victim"] as Snowflake?, author) { victim ->
                // check permissions
                if (author.id == victim.id || victim.id in config.noSudoRequiredKillVictims || author.id in config.sudoers) {
                    withPlayerData(victim.id) {
                        onDeath(victim, this, false)
                    }
                } else {
                    respond {
                        sudoFailMessage(author)
                    }
                }
            }
        }
    }

    slash("bean") {
        description = "What happened with the bois at 3AM last night."
        options = listOf(
            UserOption(
                name = "victim",
                description = "Target of this beaning.",
                optional = true,
                onParseFailure = ::wrongUserMessage
            )
        )
        execute = { author, _, options ->
            withOptionalUserArg(kord, options["victim"] as Snowflake?, author) { victim ->
                withPlayerData(victim.id) {
                    respond {
                        deathMessage(victim, this@withPlayerData, true)
                    }
                }
            }
        }
    }
}
