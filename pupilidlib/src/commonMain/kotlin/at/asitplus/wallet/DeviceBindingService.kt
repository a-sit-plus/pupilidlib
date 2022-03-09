package at.asitplus.wallet

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

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
        val bindingConfirmResponse = client.post("$serverAddress/binding/confirm") {
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

interface DeviceAdapter {
    suspend fun createKeyCsr(challenge: ByteArray): ByteArray
    fun loadAttestationCerts(): List<ByteArray>
    fun storeCertificate(certificate: ByteArray)
    val deviceName: String
}

// TODO activate
//expect class DefaultDeviceAdapter : DeviceAdapter
