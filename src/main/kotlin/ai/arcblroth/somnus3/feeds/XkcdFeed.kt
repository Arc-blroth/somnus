package ai.arcblroth.somnus3.feeds

import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.coroutineScope
import ai.arcblroth.somnus3.request
import ai.arcblroth.somnus3.withPersistentConfig
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.TextChannel
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes

object XkcdFeed {
    private val LOGGER = LoggerFactory.getLogger("XkcdFeed")

    suspend fun start(kord: Kord, config: Map<Snowflake, TextChannel>) = kord.coroutineScope.launch {
        while (true) {
            try {
                val response = request("https://xkcd.com/info.0.json")
                val data = Constants.lenientJson.decodeFromString<XkcdInfo>(response.bodyAsText())
                withPersistentConfig {
                    if (data.num > lastXkcd) {
                        lastXkcd = data.num
                        config.forEach { (_, channel) ->
                            channel.createEmbed {
                                color = Color(0x96a8c8)
                                title = "xkcd: ${data.title}"
                                url = "https://xkcd.com/${data.num}"
                                image = data.img
                                footer {
                                    text = data.alt
                                }
                                timestamp = response.lastModified()?.time?.let { Instant.fromEpochMilliseconds(it) }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                LOGGER.warn("Couldn't fetch xkcd data!", e)
            } catch (e: SerializationException) {
                LOGGER.warn("Couldn't deserialize xkcd data!", e)
            }
            delay(5.minutes)
        }
    }
}

@Serializable
data class XkcdInfo(
    val day: Int,
    val month: Int,
    val year: Long,
    val num: Long,
    val title: String,
    @SerialName("safe_title") val safeTitle: String,
    val img: String,
    val alt: String,
    val transcript: String,
    val link: String,
    val news: String,
)
