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

    suspend fun createDeviceBinding(): Result {
        try {
            val bindingParamsRequest = BindingParamsRequest(deviceAdapter.deviceName)
            val post = client.post("$serverAddress/binding/start") {
                contentType(ContentType.Application.Json)
                headers {
                    append("X-Auth-ExtNonce", extAuthNonce)
                }
                setBody(bindingParamsRequest)
            }
            val bindingParamsResponse = post.body<BindingParamsResponse>()
            val xAuthToken = post.headers["X-Auth-Token"].toString()
            val challenge = bindingParamsResponse.challenge
            val subject = bindingParamsResponse.subject
            val keyType = KeyAlgorithm.parse(bindingParamsResponse.keyType)
            deviceAdapter.createKey(keyType, challenge)
            val publicKeyEncoded = deviceAdapter.getPublicKeyEncoded().getOrThrow()
            val csr = asn1Service.signAndCreateCsr(publicKeyEncoded, subject, keyType).getOrThrow()
            val attestationCerts = deviceAdapter.loadAttestationCerts().getOrThrow()
            val bindingCsrRequest =
                BindingCsrRequest(challenge, csr, deviceAdapter.deviceName, attestationCerts)
            val bindingCsrResponse = client.post("$serverAddress/binding/create") {
                contentType(ContentType.Application.Json)
                headers {
                    append("X-Auth-Token", xAuthToken)
                }
                setBody(bindingCsrRequest)
            }.body<BindingCsrResponse>()
            deviceAdapter.storeCertificate(bindingCsrResponse.certificate)
            val bindingConfirmRequest = BindingConfirmRequest(true)
            val bindingConfirmResponse = client.post("$serverAddress/binding/confirm") {
                contentType(ContentType.Application.Json)
                headers {
                    append("X-Auth-Token", xAuthToken)
                }
                setBody(bindingConfirmRequest)
            }.body<BindingConfirmResponse>()
            if (!bindingConfirmResponse.success) {
                return Result.ErrorFromNetwork("No success for binding/confirm message")
            }

            val message = deviceAdapter.getIssuingMessage()
                ?: throw IllegalArgumentException("No issuing message from native code")

            val initialPost = client.post("$serverAddress/pupilid/issue") {
                contentType(ContentType.Text.Plain)
                headers {
                    append("X-Auth-Token", xAuthToken)
                }
                setBody(message)
            }
            val response = initialPost.body<String>()

            return Result.Success(response)
        } catch (e: ResponseException) {
            return Result.ErrorFromNetwork(e.message ?: "Error")
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: Throwable) {
            return Result.ErrorFromNativeCode(e)
                .also { Napier.w("Error from native code", throwable = e) }
        }
    }

    sealed class Result {
        data class Success(val message: String) : Result()
        data class ErrorFromNetwork(val error: String) : Result()
        data class ErrorFromNativeCode(val throwable: Any?) : Result()
    }

}