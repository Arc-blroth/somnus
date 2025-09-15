package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.SlashCommandExecutionBuilder
import ai.arcblroth.somnus3.commands.TextBasedSlashCommandExecutionBuilder
import ai.arcblroth.somnus3.data.BedType
import ai.arcblroth.somnus3.data.PlayerData
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.User
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

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

fun SlashCommandExecutionBuilder.isTextCommand() = this is TextBasedSlashCommandExecutionBuilder

val SlashCommandExecutionBuilder.prefix get() = if (isTextCommand()) Constants.PREFIX else "/"

fun PlayerData.applyPowerEffects(
    modifier: Double,
    round: Boolean = false,
) = when (bedType) {
    BedType.GOLD -> ceil(modifier * 1.25).toInt()
    BedType.DEMON -> ceil(modifier * 1.5).toInt()
    else ->
        (
            if (round) {
                round(modifier)
            } else {
                floor(modifier)
            }
        ).toInt()
}
