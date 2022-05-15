package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.commands.UserOption
import ai.arcblroth.somnus3.data.withPlayerData
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord

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
}
