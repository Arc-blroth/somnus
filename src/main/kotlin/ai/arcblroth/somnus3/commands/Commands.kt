package ai.arcblroth.somnus3.commands

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Somnus
import ai.arcblroth.somnus3.commands.impl.*
import ai.arcblroth.somnus3.mcserver.ServerInfoProvider
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.User

class Commands internal constructor(private val registry: CommandRegistry) {
    suspend fun handleMessage(message: Message, author: User, commandPrefix: String, tokens: List<String>) {
        registry.handleMessage(message, author, commandPrefix, tokens)
    }
}

suspend fun registerCommandCallbacks(
    kord: Kord,
    somnus: Somnus,
    config: Config,
    serverInfoProvider: ServerInfoProvider?
): Commands {
    val registry = CommandRegistry.registerCommands(kord, somnus, config) {
        registerMetaCommands(kord, config)
        registerGameCommands(kord, config)
        registerAdminCommands(kord, config)
        registerCounterCommands(kord, config)
        registerIRLCommands(kord, config, serverInfoProvider)
    }
    return Commands(registry)
}
