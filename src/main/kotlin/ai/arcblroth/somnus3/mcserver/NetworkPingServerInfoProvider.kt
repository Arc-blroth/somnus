package ai.arcblroth.somnus3.mcserver

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Constants
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.serializer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class NetworkPingServerInfoProvider(val ip: String, val port: Int) : ServerInfoProvider {
    override suspend fun get(): ServerInfo {
        val stringResult = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(ip, port) {
            socketTimeout = 3000
        }.use { socket ->
            socket.openWriteChannel(autoFlush = true).run {
                val ping = ByteArrayOutputStream()
                val packet = ByteArrayOutputStream()
                packet.write(0) // Protocol
                packet.write(0) // End Varint
                packet.write(packData(ip.toByteArray(StandardCharsets.UTF_8))) // IP
                packet.write(port shr 8 and 0xff) // Port
                packet.write(port and 0xff)
                packet.write(1) // Request Status
                ping.write(packData(packet.toByteArray()))
                ping.write(1)
                ping.write(0)
                writeFully(ByteBuffer.wrap(ping.toByteArray()))
            }
            socket.openReadChannel().run {
                popInt()
                popInt()
                val length = popInt()
                val messageBytes = ByteArray(length)
                readFully(messageBytes, 0, length)
                String(messageBytes)
            }
        }

        val response = Constants.lenientJson.decodeFromString<PingResponse>(
            stringResult.replace(Regex("\u00A7[0-9a-fk-orA-FK-OR]"), "")
        )
        return ServerInfo(
            description = response.description?.text,
            favicon = response.favicon?.substring("data:image/png;base64,".length)?.decodeBase64Bytes(),
            ip = ip,
            modpack = "Version",
            version = response.version?.name,
            playersOnline = response.players?.online,
            playersMax = response.players?.max,
            playerSample = response.players?.sample?.mapNotNull { it.name }?.toTypedArray(),
        )
    }

    companion object {
        fun fromConfig(config: Config) = if (config.mcServerIP.isNullOrBlank()) {
            null
        } else {
            val addr = config.mcServerIP.split(":")
            NetworkPingServerInfoProvider(addr[0], addr[1].toInt())
        }

        private fun packData(d: ByteArray): ByteArray {
            val out = ByteArrayOutputStream()
            out.write(packVarint(d.size))
            out.write(d)
            return out.toByteArray()
        }

        private fun packVarint(d: Int): ByteArray {
            val out = ByteArrayOutputStream()
            var value = d
            var bitlength = 0
            while (value > 0) {
                bitlength++
                value = value shr 1
            }
            for (i in 0 until 1 + bitlength / 7) {
                out.write(0x40 * (if (i != bitlength / 7) 1 else 0) + (d shr 7 * i) % 128)
            }
            return out.toByteArray()
        }

        private suspend fun ByteReadChannel.popInt(): Int {
            var acc = 0
            var shift = 0
            var b = readByte().toInt()
            while (b and 0x80 != 0) {
                acc = acc or (b and 0x7f shl shift)
                shift += 7
                b = readByte().toInt()
            }
            return acc or (b shl shift)
        }
    }
}

@Serializable
private data class PingResponse(
    val version: Version?,
    val players: Players?,
    @Serializable(with = DescriptionSerializer::class)
    val description: Description?,
    val favicon: String?,
) {
    @Serializable
    data class Version(val name: String?, val protocol: Int?)

    @Serializable
    data class Players(val max: Int?, val online: Int?, val sample: List<Player>?)

    @Serializable
    data class Player(val name: String?, val id: String?)

    @Serializable
    data class Description(val text: String?)
}

private object DescriptionSerializer : JsonTransformingSerializer<PingResponse.Description>(serializer<PingResponse.Description>()) {
    override fun transformDeserialize(element: JsonElement) =
        if (element is JsonPrimitive && element.isString) JsonObject(mapOf("text" to element)) else element
}
