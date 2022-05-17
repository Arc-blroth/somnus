package ai.arcblroth.somnus3.commands

import ai.arcblroth.somnus3.panel.InteractivePanelBuilder
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.create.MessageCreateBuilder

typealias SlashCommandAction = suspend SlashCommandExecutionBuilder.(author: User, guild: Guild?, options: Map<String, Any?>) -> Unit

@SomnusCommandsDsl
class SlashCommandBuilder {
    var description: String? = null
    var options: List<Option<*>> = listOf()
    var execute: SlashCommandAction? = null
}

@SomnusCommandsDsl
interface SlashCommandExecutionBuilder {
    fun respond(builder: MessageCreateBuilder.() -> Unit)
    fun respondPanel(builder: InteractivePanelBuilder.() -> Unit)
}

internal data class SlashCommand(
    val name: String,
    val description: String,
    val options: List<Option<*>>,
    val numRequiredOptions: Int,
    val execute: SlashCommandAction,
    val isAlias: Boolean = false,
)
