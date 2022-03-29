package at.asitplus.wallet

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

/**
 * @param engine can be passed to enable mocks in unit tests, to use the default engine, pass null
 */
class DeviceBindingPupilIdIssuingService(
    private val extAuthNonce: String,
    private val serverAddress: String,
    private val deviceAdapter: DeviceAdapter,
    private val asn1Service: Asn1Service,
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
        extAuthNonce = extAuthNonce,
        serverAddress = serverAddress,
        deviceAdapter = deviceAdapter,
        asn1Service = asn1Service,
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

            val message = deviceAdapter.getIssuingMessage()
                ?: throw IllegalArgumentException("No issuing message from native code")

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