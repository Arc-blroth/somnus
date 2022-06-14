package ai.arcblroth.somnus3

import dev.kord.core.Kord
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job

val SOMNUS_VERSION = Somnus::class.java.`package`.implementationVersion?.removeSuffix("-SNAPSHOT")
private val USER_AGENT = "Somnus/${SOMNUS_VERSION ?: "3.0"}"

suspend fun request(url: String) = HttpClient().request {
    userAgent(USER_AGENT)
    url(url)
}

/**
 * Constructs a coroutine scope in Kord's default coroutine context.
 * @see dev.kord.core.event.kordCoroutineScope
 */
val Kord.coroutineScope get() = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext.job))
