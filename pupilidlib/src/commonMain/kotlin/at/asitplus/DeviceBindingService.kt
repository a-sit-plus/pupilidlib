package at.asitplus

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

class DeviceBindingService(
    private val extAuthNonce: String,
    private val serverAddress: String,
    private val deviceAdapter: DeviceAdapter,
) {

    private val client = HttpClient() {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun createDeviceBinding(): Result {
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
        val csr = deviceAdapter.createKeyCsr(challenge)
        val bindingCsrRequest =
            BindingCsrRequest(challenge, csr, deviceAdapter.deviceName, deviceAdapter.loadAttestationCerts())
        val bindingCsrResponse = client.post("$serverAddress/binding/create") {
            contentType(ContentType.Application.Json)
            headers {
                append("X-Auth-Token", xAuthToken)
            }
            setBody(bindingCsrRequest)
        }.body<BindingCsrResponse>()
        deviceAdapter.storeCertificate(bindingCsrResponse.certificate)
        val bindingConfirmRequest = BindingConfirmRequest(true)
        val bindingConfirmResponse = client.post("$serverAddress/binding/create") {
            contentType(ContentType.Application.Json)
            headers {
                append("X-Auth-Token", xAuthToken)
            }
            setBody(bindingConfirmRequest)
        }.body<BindingConfirmResponse>()
        return Result.Success(bindingConfirmResponse.success)
    }

    sealed class Result {
        data class Success(val success: Boolean) : Result()
        data class ErrorFromServer(val error: String) : Result()
    }

}

// TODO turn into expect class?
interface DeviceAdapter {
    suspend fun createKeyCsr(challenge: ByteArray): ByteArray
    fun loadAttestationCerts(): List<ByteArray>
    fun storeCertificate(certificate: ByteArray)
    val deviceName: String
}

@Serializable
data class BindingParamsRequest(
    val deviceName: String,
)

@Serializable
data class BindingParamsResponse(
    val challenge: ByteArray,
)

@Serializable
data class BindingCsrRequest(
    val challenge: ByteArray,
    val csr: ByteArray,
    val deviceName: String,
    val attestationCerts: List<ByteArray>,
)

@Serializable
data class BindingCsrResponse(
    val certificate: ByteArray,
    val attestedPublicKey: ByteArray,
)

@Serializable
data class BindingConfirmRequest(
    val success: Boolean
)

@Serializable
data class BindingConfirmResponse(
    val success: Boolean
)