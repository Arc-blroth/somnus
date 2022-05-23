package ai.arcblroth.somnus3.activity

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Activity
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.user.PresenceUpdateEvent
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap

class ActivityDetector(
    private val config: Map<Snowflake, TextChannel>,
    private val messageTemplate: String,
    private val activityFilter: (Activity) -> Boolean,
) {
    private val cache = ConcurrentHashMap<Snowflake, Instant>()

    suspend fun update(event: PresenceUpdateEvent) {
        val logChannel = config[event.guildId]
        if (logChannel != null) {
            val activity = event.presence.activities.find(activityFilter)
            if (activity != null) {
                val startTime = activity.start
                if (startTime != null) {
                    val memberId = event.member.id
                    if (!cache.containsKey(memberId) || cache[memberId] != startTime) {
                        cache[memberId] = startTime
                    } else {
                        // presence was identical
                        return
                    }
                }
                // either no time information or presence has changed
                logChannel.createMessage(messageTemplate.format(event.member.asMember().displayName))
            }
        }
    }
}
