package ai.arcblroth.somnus3.commands

import dev.kord.core.entity.User
import dev.kord.rest.builder.message.create.MessageCreateBuilder

typealias SlashCommandAction = suspend SlashCommandExecutionBuilder.(author: User, options: Map<String, Any>) -> Unit

@SomnusCommandsDsl
class SlashCommandBuilder {
    var name: String? = null
    var description: String? = null
    var options: List<Option<*>> = listOf()
    var execute: SlashCommandAction? = null
}

@SomnusCommandsDsl
interface SlashCommandExecutionBuilder {
    fun respond(builder: MessageCreateBuilder.() -> Unit)
}

internal data class SlashCommand(
    val name: String,
    val description: String,
    val options: List<Option<*>>,
    val execute: SlashCommandAction,
)
