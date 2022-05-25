package ai.arcblroth.somnus3.activity

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Activity
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.user.PresenceUpdateEvent
import kotlinx.datetime.Instant
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap

class ActivityDetector(
    private val config: Map<Snowflake, TextChannel>,
    private val messageTemplate: String,
    private val activityFilter: (Activity) -> Boolean,
) {
    private val cache = TreeMap<Snowflake, ConcurrentHashMap<Snowflake, Instant>>()

    init {
        config.keys.forEach { cache[it] = ConcurrentHashMap() }
    }

    suspend fun update(event: PresenceUpdateEvent) {
        val logChannel = config[event.guildId]
        if (logChannel != null) {
            val cacheEntry = cache[event.guildId]!!
            val activity = event.presence.activities.find(activityFilter)
            if (activity != null) {
                val startTime = activity.start
                if (startTime != null) {
                    val memberId = event.member.id
                    // Note: Not sure if this is a bug in kotlin's Instant implementation, certain
                    // applications, or the Discord API itself, but the `startTime` returned from
                    // the API sometimes differs by up to a millisecond between updates. Thus, we
                    // err on the side of caution and swallow any presence update with a start time
                    // that differs from our cached start time by less than 1 second.
                    val cachedTime = cacheEntry[memberId]
                    if (cachedTime == null || (cachedTime - startTime).absoluteValue.inWholeSeconds > 1) {
                        cacheEntry[memberId] = startTime
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
