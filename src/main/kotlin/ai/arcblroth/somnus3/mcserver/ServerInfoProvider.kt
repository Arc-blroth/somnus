package ai.arcblroth.somnus3.mcserver

@FunctionalInterface
interface ServerInfoProvider {
    suspend fun get(): ServerInfo
}
