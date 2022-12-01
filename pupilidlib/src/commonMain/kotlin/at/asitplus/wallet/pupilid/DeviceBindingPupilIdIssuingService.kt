package at.asitplus.wallet.pupilid

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerializationException
import kotlin.time.Duration.Companion.seconds

class DeviceBindingPupilIdIssuingService constructor(
    /**
     * Base address of the server to contact, i.e. `https://wallet.a-sit.at/`
     */
    private val serverAddress: String,
    /**
     * Nonce from scanned QR Code, used for authentication
     */
    extAuthNonce: String,
    /**
     * Needed to load information from native implementations
     */
    deviceAdapter: DeviceAdapter,
    /**
     * Needed to create signature of the CSR
     */
    cryptoAdapter: Asn1Service.CryptoAdapter,
    /**
     * Needed to get the message to send from native callers
     */
    private val callback: Callback,
    /**
     * Path of the endpoint for issuing credentials, defaults to `listOf("pupilid", "issue")`
     */
    private val serverIssuePath: List<String> = listOf("pupilid", "issue"),

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
     * Can be passed to enable mocks in unit tests, to use the default engine, pass `null`
     */
    engine: HttpClientEngine? = null
) {

    private val client: HttpClient = engine?.let {
        HttpClient(it) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                connectTimeoutMillis = connectionTimeoutSeconds.seconds.inWholeMilliseconds
                socketTimeoutMillis = socketTimeoutSeconds.seconds.inWholeMilliseconds
            }
        }
    } ?: HttpClient {
        install(ContentNegotiation) {
            json()
        }
        install(HttpTimeout) {
            connectTimeoutMillis = connectionTimeoutSeconds.seconds.inWholeMilliseconds
            socketTimeoutMillis = socketTimeoutSeconds.seconds.inWholeMilliseconds
        }
    }
    private val deviceBindingService = DeviceBindingService(
        serverAddress = serverAddress,
        extAuthNonce = extAuthNonce,
        deviceAdapter = deviceAdapter,
        cryptoAdapter = cryptoAdapter,
        engine = engine,
    )

    suspend fun createDeviceBindingAndIssueCredentials(): ServiceResult {
        try {
            val url = URLBuilder(serverAddress).appendPathSegments(serverIssuePath).buildString()
            val deviceBindingResponse = deviceBindingService.getDeviceBindingResponse()
            var xAuthToken = deviceBindingResponse.xAuthToken
            val bindingConfirmBody = deviceBindingResponse.body
            if (!bindingConfirmBody.success) {
                return ServiceResult.ErrorFromNetwork("No success for binding/confirm message")
            }

            val message = callback.loadMessage().getOrThrow()

            val issuePost = client.post(url) {
                headers {
                    append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
                }
                setBody(message)
                expectSuccess = true
            }
            val response = issuePost.body<String>()
            xAuthToken =
                issuePost.headers[HeaderNames.X_AUTH_TOKEN]?.ifBlank { xAuthToken } ?: xAuthToken
            return ServiceResult.Success(response, xAuthToken)
        } catch (e: SerializationException) {
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error")
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: ResponseException) {
            val details = kotlin.runCatching { e.response.body<SpringErrorDocument>() }.getOrNull()
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error", details)
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: CryptoException) {
            return ServiceResult.SignatureInvalid
                .also { Napier.w("binding not signed by trusted issuer", throwable = e) }
        } catch (e: Throwable) {
            return ServiceResult.ErrorFromNativeCode(e)
                .also { Napier.w("Error from native code", throwable = e) }
        }
    }

    fun interface Callback {
        /**
         * Implementers: Return the message of `IssueCredentialMessenger.startDirect()`
         */
        suspend fun loadMessage(): KmmResult<String>
    }

}