package at.asitplus.wallet

import at.asitplus.wallet.lib.KmmResult
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*

/**
 * @param engine can be passed to enable mocks in unit tests, to use the default engine, pass null
 */
class DeviceBindingService(
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

    suspend fun createDeviceBinding(): ServiceResult {
        try {
            val bindingParamsRequest = BindingParamsRequest(deviceAdapter.deviceName)
            val post = client.post("$serverAddress/binding/start") {
                contentType(ContentType.Application.Json)
                headers {
                    append(HeaderNames.X_AUTH_EXT_NONCE, extAuthNonce)
                }
                setBody(bindingParamsRequest)
            }
            val bindingParamsResponse = post.body<BindingParamsResponse>()
            val xAuthToken = post.headers[HeaderNames.X_AUTH_TOKEN].toString()
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
                    append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
                }
                setBody(bindingCsrRequest)
            }.body<BindingCsrResponse>()
            deviceAdapter.storeCertificate(bindingCsrResponse.certificate)
            val bindingConfirmRequest = BindingConfirmRequest(true)
            val bindingConfirmResponse = client.post("$serverAddress/binding/confirm") {
                contentType(ContentType.Application.Json)
                headers {
                    append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
                }
                setBody(bindingConfirmRequest)
            }.body<BindingConfirmResponse>()
            return ServiceResult.Success(bindingConfirmResponse.success.toString())
        } catch (e: ResponseException) {
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error")
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: Throwable) {
            return ServiceResult.ErrorFromNativeCode(e)
                .also { Napier.w("Error from native code", throwable = e) }
        }
    }

}


interface DeviceAdapter {
    suspend fun loadAttestationCerts(): KmmResult<List<ByteArray>>
    fun storeCertificate(certificate: ByteArray): KmmResult<Boolean>
    suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean>
    fun getPublicKeyEncoded(): KmmResult<ByteArray>
    fun getIssuingMessage(): String?

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
