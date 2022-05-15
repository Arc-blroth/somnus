package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.commands.UserOption
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord

fun CommandRegistry.registerAdminCommands(kord: Kord, config: Config) {
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
                respond {
                    somnusEmbed(thumbnailUser = victim) {
                        title = "${victim.username} has Died?"
                    }
                }
            }
        }
    }
}
