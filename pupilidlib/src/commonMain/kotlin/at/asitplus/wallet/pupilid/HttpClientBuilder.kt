package at.asitplus.wallet.pupilid

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration.Companion.seconds

class HttpClientBuilder(
    /**
     * Max time to establish a connections.
     * Connection establishment will fail if exceeded.
     * Default: 20
     */
    connectionTimeoutSeconds: Int = 10,
    /**
     * Max time (in seconds) between transmission of data packet.
     * Connection will be reset, if exceeded
     * Default: 20
     */
    socketTimeoutSeconds: Int = 10,
    /**
     * User Agent header to set on every request, should contain
     * the app name and the version, e.g. "com.example.wallet iOS 1.0.4"
     */
    userAgent: String? = null,
    /**
     * Whether to enable logging calls with Napier
     */
    enableLogging: Boolean = false,
    /**
     * Can be passed to enable mocks in unit tests, to use the default engine, pass `null`
     */
    engine: HttpClientEngine? = null,
    /**
     * Can be passed to extend the setup of the HttpClient
     */
    extendSetup: (HttpClientConfig<*>.() -> Unit)? = null
) {

    val client: HttpClient

    init {
        val setup: HttpClientConfig<*>.() -> Unit = { ->
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                connectTimeoutMillis = connectionTimeoutSeconds.seconds.inWholeMilliseconds
                socketTimeoutMillis = socketTimeoutSeconds.seconds.inWholeMilliseconds
            }
            defaultRequest {
                userAgent.let { header(HttpHeaders.UserAgent, it) }
            }
            if (enableLogging) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            Napier.v(message = message, tag = "ktor")
                        }
                    }
                    level = LogLevel.INFO
                }
            }
            extendSetup?.let { it() }
        }
        client = engine?.let { HttpClient(it) { setup() } } ?: HttpClient { setup() }
    }


}
