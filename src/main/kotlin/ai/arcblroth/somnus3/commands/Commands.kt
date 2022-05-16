package ai.arcblroth.somnus3.commands

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.commands.impl.registerAdminCommands
import ai.arcblroth.somnus3.commands.impl.registerCounterCommands
import ai.arcblroth.somnus3.commands.impl.registerGameCommands
import ai.arcblroth.somnus3.commands.impl.registerMetaCommands
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.User

class Commands internal constructor(private val registry: CommandRegistry) {
    suspend fun handleMessage(message: Message, author: User, commandPrefix: String, tokens: List<String>) {
        registry.handleMessage(message, author, commandPrefix, tokens)
    }
}

suspend fun registerCommandCallbacks(kord: Kord, config: Config): Commands {
    val registry = CommandRegistry.registerCommands(kord, config) {
        registerMetaCommands(kord, config)
        registerGameCommands(kord, config)
        registerAdminCommands(kord, config)
        registerCounterCommands(kord, config)
    }
    return Commands(registry)
}
