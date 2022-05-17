@file:JvmName("SomnusMain")

package ai.arcblroth.somnus3

import ai.arcblroth.somnus3.data.initDatabase
import ai.arcblroth.somnus3.mcserver.NetworkPingServerInfoProvider
import kotlin.system.exitProcess

/**
 *           Somnus (sleep)
 *    son of night, brother of death
 *
 *           by Arc'blroth
 *
 * ported to Kotlin as part of code cleanup
 */
suspend fun main() {
    val config: Config
    try {
        initDatabase()
        config = loadConfig()
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(-1)
    }

    Somnus(config, NetworkPingServerInfoProvider.fromConfig(config)).start()
}
