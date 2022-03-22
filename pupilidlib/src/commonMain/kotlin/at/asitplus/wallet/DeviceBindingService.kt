package at.asitplus.wallet

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import io.ktor.utils.io.errors.*

/**
 * @param engine should be `CIO.create()`
 */
class DeviceBindingService(
    private val extAuthNonce: String,
    private val serverAddress: String,
    private val deviceAdapter: DeviceAdapter,
    private val asn1Service: Asn1Service,
    engine: HttpClientEngine,
) {

    private val client = HttpClient(engine) {
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
            val publicKeyEncoded = deviceAdapter.getPublicKeyEncoded().getOrElse {
                return Result.ErrorFromNativeCode(it)
            }
            val csr = asn1Service.signAndCreateCsr(publicKeyEncoded, subject, keyType).getOrElse {
                return Result.ErrorFromNativeCode(it)
            }
            val attestationCerts = deviceAdapter.loadAttestationCerts().getOrElse {
                return Result.ErrorFromNativeCode(it)
            }
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
            return Result.Success(bindingConfirmResponse.success)
        } catch (e: ResponseException) {
            return Result.ErrorFromNetwork(e.message ?: "Error")
        } catch (e: Throwable) {
            return Result.ErrorFromNativeCode(e)
        }
    }

    sealed class Result {
        data class Success(val success: Boolean) : Result()
        data class ErrorFromNetwork(val error: String) : Result()
        data class ErrorFromNativeCode(val throwable: Any?) : Result()
    }

}


interface DeviceAdapter {
    suspend fun loadAttestationCerts(): Result<List<ByteArray>>
    fun storeCertificate(certificate: ByteArray): Result<Boolean>
    suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): Result<Boolean>
    suspend fun getPublicKeyEncoded(): Result<ByteArray>
    val deviceName: String
}

enum class HashAlgorithm {
    SHA1,
    SHA256,
    SHA512,
}

enum class KeyAlgorithm {
    EC,
    RSA;

    companion object {
        fun parse(it: String): KeyAlgorithm {
            if ("rsa" == it.toLowerCasePreservingASCIIRules())
                return RSA
            return EC
        }
    }
}

// TODO activate
//expect class DefaultDeviceAdapter : DeviceAdapter
