package ai.arcblroth.somnus3.mcserver

@Suppress("ArrayInDataClass")
data class ServerInfo(
    val description: String? = null,
    val favicon: ByteArray? = null,
    val ip: String? = null,
    val modpack: String? = null,
    val version: String? = null,
    val uptime: String? = null,
    val playersOnline: Int? = null,
    val playersMax: Int? = null,
    val playerSample: Array<String>? = null,
    val difficulty: String? = null,
    val inGameTime: String? = null,
)
