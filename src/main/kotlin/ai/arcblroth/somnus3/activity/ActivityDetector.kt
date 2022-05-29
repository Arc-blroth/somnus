package ai.arcblroth.somnus3.activity

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Activity
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.user.PresenceUpdateEvent
import kotlinx.datetime.Instant
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap

class ActivityDetector(
    private val config: Map<Snowflake, TextChannel>,
    private val messageTemplate: String,
    private val activityFilter: (Activity) -> Boolean,
) {
    private val cache = TreeMap<Snowflake, ConcurrentHashMap<Snowflake, ActivityDetectorCacheEntry>>()

    init {
        config.keys.forEach { cache[it] = ConcurrentHashMap() }
    }

    suspend fun update(event: PresenceUpdateEvent) {
        val logChannel = config[event.guildId]
        if (logChannel != null) {
            val memberId = event.member.id
            val guildCache = cache[event.guildId]!!
            val cachedEntry = guildCache[memberId]
            val activity = event.presence.activities.find(activityFilter)
            if (activity != null) {
                val startTime = activity.start
                if (cachedEntry != null && startTime != null) {
                    // Note: Not sure if this is a bug in kotlin's Instant implementation, certain
                    // applications, or the Discord API itself, but the `startTime` returned from
                    // the API sometimes differs by up to a millisecond between updates. Thus, we
                    // err on the side of caution and swallow any presence update with a start time
                    // that differs from our cached start time by less than 1 second.
                    val cachedTime = cachedEntry.startTime
                    if (cachedTime != null) {
                        if ((cachedTime - startTime).absoluteValue.inWholeSeconds > 1) {
                            cachedEntry.startTime = startTime
                        } else {
                            // presence was identical
                            return
                        }
                    }
                }
                // either no time information or presence has changed
                val newContent = messageTemplate.format(event.member.asMember().displayName)
                if (cachedEntry == null || cachedEntry.invalid) {
                    val message = logChannel.createMessage(newContent)
                    guildCache[memberId] = ActivityDetectorCacheEntry(startTime, message)
                } else {
                    cachedEntry.count++
                    cachedEntry.message.edit { content = "$newContent (x${cachedEntry.count})" }
                }
            }
        }
    }

    fun invalidate(event: MessageCreateEvent) {
        val logChannel = config[event.guildId]
        if (logChannel != null && event.message.channelId == logChannel.id) {
            val guildCache = cache[event.guildId]!!
            guildCache.values.forEach { it.invalid = true }
        }
    }
}

data class ActivityDetectorCacheEntry(
    var startTime: Instant?,
    val message: Message,
    var count: Long = 1,
    var invalid: Boolean = false
)
