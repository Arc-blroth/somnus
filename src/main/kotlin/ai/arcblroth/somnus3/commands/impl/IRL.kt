package ai.arcblroth.somnus3.commands.impl

import ai.arcblroth.somnus3.Config
import ai.arcblroth.somnus3.Constants
import ai.arcblroth.somnus3.commands.CommandRegistry
import ai.arcblroth.somnus3.mcserver.ServerInfoProvider
import ai.arcblroth.somnus3.request
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.Kord
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.statement.*
import io.ktor.utils.io.ByteReadChannel
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun CommandRegistry.registerIRLCommands(
    kord: Kord,
    config: Config,
    serverInfoProvider: ServerInfoProvider?,
) {
    slash("server") {
        description = "Check if the Minecraft server is running."
        execute = { _, _, _ ->
            if (serverInfoProvider != null) {
                try {
                    val info = serverInfoProvider.get()
                    respond {
                        somnusEmbed {
                            color = Constants.GOOD_COLOR
                            title = "Server is up!"
                            description = info.description
                            info.favicon?.let {
                                thumbnail { url = "attachment://favicon.png" }
                                this@respond.addFile(
                                    "favicon.png",
                                    ChannelProvider {
                                        ByteReadChannel(it)
                                    },
                                )
                            }
                            info.ip?.let { field("IP", false) { it } }
                            info.version?.let { field(info.modpack ?: "Version", true) { it } }
                            info.uptime?.let { field("Uptime", true) { it } }
                            info.playerSample?.let {
                                val sample =
                                    if (it.isNotEmpty()) {
                                        it.joinToString("\n")
                                    } else {
                                        "No players right now :("
                                    }
                                field("Players (${info.playersOnline ?: "?"}/${info.playersMax ?: "?"}", false) {
                                    sample
                                }
                            }
                            info.difficulty?.let { field("Difficulty", true) { it } }
                            info.inGameTime?.let { field("In-Game Time", true) { it } }
                        }
                    }
                } catch (e: Exception) {
                    respond {
                        somnusEmbed {
                            color = Constants.ERROR_COLOR
                            title = "Server is down :("
                            description = "${e.javaClass.simpleName}: ${e.message}"
                        }
                    }
                }
            } else {
                respond {
                    content = "Somnus isn't configured to check any servers currently."
                }
            }
        }
    }

    slash("fire") {
        description = "Query what fires are burning in California right now, sorted by most recent."
        execute = { _, _, _ ->
            val response = request("https://www.fire.ca.gov/umbraco/api/IncidentApi/List?inactive=false")
            val data = Constants.lenientJson.decodeFromString<List<CalfireFireData>>(response.bodyAsText())
            if (data.isEmpty()) {
                respond {
                    somnusEmbed {
                        color = Constants.GOOD_COLOR
                        title = "No nearby fires!"
                        footer { text = Constants.FIRE_DISCLAIMER_FOOTER }
                    }
                }
            } else {
                val responseTime = Instant.fromEpochMilliseconds(response.responseTime.timestamp)
                respondPanel {
                    style(ButtonStyle.Danger)
                    data.sortedByDescending { it.updated }.forEach {
                        page {
                            color = Constants.ERROR_COLOR
                            title = it.name ?: "? Fire"
                            field(it.location ?: "Location", false) { "${it.latitude ?: "?"}, ${it.longitude ?: "?"}" }
                            field("Type", true) { it.type ?: "Fire" }
                            field("Started", true) { it.startedDateOnly ?: "unknown" }
                            field("Acres Burned", false) { it.acresBurned?.toString() ?: "unknown" }
                            field("Percent Contained", false) { "${it.percentContained ?: "unknown"}%" }
                            field(Constants.BLANK_FIELD, false) { Constants.BLANK_FIELD }
                            footer {
                                text = Constants.FIRE_DISCLAIMER_FOOTER
                                icon = "https://www.fire.ca.gov/images/template2014/apple-touch-icon-144x144.png"
                            }
                            timestamp = responseTime
                        }
                    }
                }
            }
        }
    }
}

@Serializable
data class CalfireFireData(
    @SerialName("Name") val name: String?,
    @SerialName("Updated") val updated: Instant?,
    @SerialName("Started") val started: Instant?,
    @SerialName("AdminUnit") val adminUnit: String?,
    @SerialName("County") val county: String?,
    @SerialName("Location") val location: String?,
    @SerialName("AcresBurned") val acresBurned: Double?,
    @SerialName("PercentContained") val percentContained: Double?,
    @SerialName("Longitude") val longitude: Double?,
    @SerialName("Latitude") val latitude: Double?,
    @SerialName("Type") val type: String?,
    @SerialName("UniqueId") val uniqueId: String?,
    @SerialName("Url") val url: String?,
    @SerialName("StartedDateOnly") val startedDateOnly: String?,
)
