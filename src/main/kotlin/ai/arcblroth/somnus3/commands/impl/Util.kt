package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.commands.SlashCommandExecutionBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.User

/**
 * Runs the given `block` with the given target user, falling back to the given default user if the target is null.
 *
 * If the target user cannot be resolved, the block will not be run and a [wrongUserMessage] will be sent instead.
 */
suspend inline fun SlashCommandExecutionBuilder.withOptionalUserArg(
    kord: Kord,
    target: Snowflake?,
    default: User,
    block: SlashCommandExecutionBuilder.(User) -> Unit,
) {
    target?.let {
        kord.getUser(it)?.let { user -> block(user) } ?: respond {
            wrongUserMessage(target.toString())
        }
    } ?: block(default)
}
