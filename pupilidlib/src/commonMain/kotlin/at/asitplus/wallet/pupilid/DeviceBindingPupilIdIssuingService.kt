package at.asitplus.wallet.pupilid

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class DeviceBindingPupilIdIssuingService internal constructor(

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
     * Implementers: Sign the `ByteArray` with the defined `KeyAlgorithm` and `HashAlgorithm`
     */
    signCallback: (ByteArray, KeyAlgorithm, HashAlgorithm) -> KmmResult<ByteArray>,
    /**
     * Implementation: Return the message of `IssueCredentialMessenger.startDirect()`
     */
    private val loadMessageCallback: () -> KmmResult<String>,
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
        }
    } ?: HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
    private val deviceBindingService = DeviceBindingService(
        serverAddress = serverAddress,
        extAuthNonce = extAuthNonce,
        deviceAdapter = deviceAdapter,
        signCallback = signCallback,
        engine = engine
    )

    suspend fun createDeviceBindingAndIssueCredentials(): ServiceResult {
        try {
            val deviceBindingResponse = deviceBindingService.getDeviceBindingResponse()
            val xAuthToken = deviceBindingResponse.headers[HeaderNames.X_AUTH_TOKEN].toString()
            val bindingConfirmBody = deviceBindingResponse.body<BindingConfirmResponse>()
            if (!bindingConfirmBody.success) {
                return ServiceResult.ErrorFromNetwork("No success for binding/confirm message")
            }

            val message = loadMessageCallback().getOrThrow()

            val initialPost = client.post("$serverAddress/pupilid/issue") {
                contentType(ContentType.Text.Plain)
                headers {
                    append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
                }
                setBody(message)
            }
            val response = initialPost.body<String>()

            return ServiceResult.Success(response)
        } catch (e: ResponseException) {
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error")
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: Throwable) {
            return ServiceResult.ErrorFromNativeCode(e)
                .also { Napier.w("Error from native code", throwable = e) }
        }
    }

}