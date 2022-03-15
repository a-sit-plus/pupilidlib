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
    private val asn1Service: Asn1Service,
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
        // TODO Subject, Key, Algorithm
        val key = KeyAlgorithm.EC
        val csr = asn1Service.signAndCreateCsr(deviceAdapter.createKeyGetPublicKeyEncoded(key), "CN=foo", key)
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
    fun loadAttestationCerts(): List<ByteArray>
    fun storeCertificate(certificate: ByteArray)
    fun createKeyGetPublicKeyEncoded(key: KeyAlgorithm): ByteArray
    suspend fun sign(input: ByteArray, key: KeyAlgorithm, hash: HashAlgorithm): ByteArray
    val deviceName: String
}

enum class HashAlgorithm {
    SHA1,
    SHA256,
    SHA512,
}

enum class KeyAlgorithm {
    EC,
    RSA,
}

// TODO activate
//expect class DefaultDeviceAdapter : DeviceAdapter
