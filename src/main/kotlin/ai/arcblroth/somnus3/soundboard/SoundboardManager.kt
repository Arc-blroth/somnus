package ai.arcblroth.somnus3.soundboard

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.channel.connect
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.voice.AudioFrame
import dev.kord.voice.VoiceConnection
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.singleOrNull
import java.nio.file.Path
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(KordVoice::class)
class SoundboardManager {
    private data class PlayerAndConnection(val player: AudioPlayer, val connection: VoiceConnection, val channelId: Snowflake) {
        suspend fun stop() {
            player.destroy()
            connection.shutdown()
        }
    }

    private val lavaplayer = DefaultAudioPlayerManager().apply { AudioSourceManagers.registerLocalSource(this) }
    private val connections: MutableMap<Snowflake, PlayerAndConnection> = mutableMapOf()

    suspend fun play(voiceChannel: BaseVoiceChannelBehavior, path: Path) {
        val guildId = voiceChannel.guildId

        val track = suspendCoroutine {
            lavaplayer.loadItem(
                path.toString(),
                object : AudioLoadResultHandler {
                    override fun trackLoaded(track: AudioTrack) {
                        it.resume(track)
                    }

                    override fun playlistLoaded(playlist: AudioPlaylist) {
                        it.resume(playlist.tracks.first())
                    }

                    override fun noMatches() {
                        it.resumeWithException(IllegalStateException("requested clip has disappeared into the void"))
                    }

                    override fun loadFailed(e: FriendlyException) {
                        it.resumeWithException(e)
                    }
                }
            )
        }

        val previousConnection = connections[guildId]

        if (previousConnection != null && previousConnection.channelId == voiceChannel.id) {
            previousConnection.player.playTrack(track)
        } else {
            connections.remove(guildId)?.stop()

            val player = lavaplayer.createPlayer()
            player.playTrack(track)

            val connection = voiceChannel.connect {
                audioProvider { AudioFrame.fromData(player.provide()?.data) }
            }
            connections[guildId] = PlayerAndConnection(player, connection, voiceChannel.id)
        }
    }

    suspend fun update(event: VoiceStateUpdateEvent) {
        val voiceChannel = event.old?.getChannelOrNull()
        if (voiceChannel != null) {
            if (
                voiceChannel.voiceStates.filter {
                    it.userId != event.kord.selfId && event.kord.getUser(it.userId)?.isBot != true
                }.singleOrNull() == null
            ) {
                // how dare you leave poor Somnus alone
                connections.remove(voiceChannel.guildId)?.stop()
            }
        }
    }
}
