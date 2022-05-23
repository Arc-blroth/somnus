package ai.arcblroth.somnus3

import dev.kord.common.entity.ActivityType
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Paths

const val CONFIG_FILE = ".data/config.json"

// a map of guild -> log channel
typealias ActivityDetectorConfig = Map<Snowflake, Snowflake>

@Serializable
data class Config(
    // bot status
    val status: String = "Pokémon Mystery Dungeon",
    val statusType: ActivityType = ActivityType.Game,

    // ip address to use when pinging the minecraft server
    val mcServerIP: String? = null,

    // the list of all servers that Somnus will function in
    val allowedServers: List<Snowflake> = listOf(),

    // a map of server -> { name, url } for the !worship command
    val worshipConfig: Map<Snowflake, WorshipConfig> = mapOf(),

    // all users allowed to execute "sudo" commands
    val sudoers: List<Snowflake> = listOf(),

    // users that all users are allowed to !kill, regardless of sudo permissions
    val noSudoRequiredKillVictims: List<Snowflake> = listOf(),

    // activity detector options
    val enableActivityDetectors: Boolean = false,
    val leagueDetectorConfig: ActivityDetectorConfig? = null,
    val leagueAppIds: List<Snowflake> = listOf(Snowflake(356869127241072640L), Snowflake(401518684763586560L)),
    val intellijDetectorConfig: ActivityDetectorConfig? = null,
    val intellijAppIds: List<Snowflake> = listOf(Snowflake(547842383207858178L)),
) {
    @Serializable
    data class WorshipConfig(
        val name: String,
        val url: String,
        val furry: Boolean = false,
    )

    init {
        if (enableActivityDetectors) {
            requireNotNull(leagueDetectorConfig) { "A valid config must be specified for the League detector to work!" }
            requireNotNull(intellijDetectorConfig) { "A valid config must be specified for the IntelliJ detector to work!" }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun loadConfig(): Config {
    val logger = LoggerFactory.getLogger("Config")
    val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }
    val file = Paths.get(System.getProperty("user.dir"), CONFIG_FILE).toFile()

    // Read config from disk
    val config = if (!file.exists()) {
        Config()
    } else {
        try {
            file.inputStream().use { json.decodeFromStream(it) }
        } catch (e: IOException) {
            logger.error("Couldn't load config file, using defaults")
            return Config()
        }
    }

    // Write config back to disk
    // This ensures new properties are represented in the config file
    try {
        file.outputStream().use { json.encodeToStream(config, it) }
    } catch (e: IOException) {
        logger.warn("Couldn't update config file")
    }

    return config
}
