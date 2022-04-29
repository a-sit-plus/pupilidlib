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
import io.ktor.util.*
import kotlinx.serialization.SerializationException

/**
 * @param engine can be passed to enable mocks in unit tests, to use the default engine, pass null
 */
class DeviceBindingService(
    /**
     * Base address of the server to contact, i.e. `https://wallet.a-sit.at/`
     */
    private val serverAddress: String,
    /**
     * Nonce from scanned QR Code, used for authentication
     */
    private val extAuthNonce: String,
    /**
     * Needed to load information from native implementations
     */
    private val deviceAdapter: DeviceAdapter,
    /**
     * Needed to create signature of the CSR
     */
    cryptoAdapter: Asn1Service.CryptoAdapter,
    /**
     * Can be passed to enable mocks in unit tests, to use the default engine, pass `null`
     */
    engine: HttpClientEngine? = null
) {

    private val asn1Service = Asn1Service(cryptoAdapter)

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

    @Throws(ResponseException::class, Throwable::class)
    internal suspend fun getDeviceBindingResponse(): HttpSessionResponse {
        val bindingParamsRequest = BindingParamsRequest(deviceAdapter.deviceName)
        val startResponse =
            client.post(URLBuilder(serverAddress).appendPathSegments("binding", "start").buildString()) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HeaderNames.X_AUTH_EXT_NONCE, extAuthNonce)
                }
                setBody(bindingParamsRequest)
                expectSuccess = true
            }
        val bindingParamsResponse = startResponse.body<BindingParamsResponse>()
        var xAuthToken = startResponse.headers[HeaderNames.X_AUTH_TOKEN].toString()
        val challenge = bindingParamsResponse.challenge
        val subject = bindingParamsResponse.subject
        val keyType = KeyAlgorithm.parse(bindingParamsResponse.keyType)
        deviceAdapter.createKey(keyType, challenge)
        val publicKeyEncoded = deviceAdapter.getPublicKeyEncoded().getOrThrow()
        val csr = asn1Service.signAndCreateCsr(publicKeyEncoded, subject, keyType).getOrThrow()
        val attestationCerts = deviceAdapter.loadAttestationCerts().getOrThrow()
        val bindingCsrRequest =
            BindingCsrRequest(challenge, csr, deviceAdapter.deviceName, attestationCerts)
        val createResponse =
            client.post(URLBuilder(serverAddress).appendPathSegments("binding", "create").buildString()) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
                }
                setBody(bindingCsrRequest)
                expectSuccess = true
            }
        xAuthToken = createResponse.headers[HeaderNames.X_AUTH_TOKEN] ?: xAuthToken
        val bindingCsrResponse = createResponse.body<BindingCsrResponse>()
        deviceAdapter.storeCertificate(bindingCsrResponse.certificate)
        val bindingConfirmRequest = BindingConfirmRequest(true)
        val confirmResponse =
            client.post(URLBuilder(serverAddress).appendPathSegments("binding", "confirm").buildString()) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
                }
                setBody(bindingConfirmRequest)
                expectSuccess = true
            }
        xAuthToken = confirmResponse.headers[HeaderNames.X_AUTH_TOKEN] ?: xAuthToken
        return HttpSessionResponse(confirmResponse.body(), xAuthToken)
    }

    suspend fun createDeviceBinding(): ServiceResult = try {
        val bindingConfirmResponse = getDeviceBindingResponse()
        val body = bindingConfirmResponse.body
        ServiceResult.Success(
            body.success.toString(),
            bindingConfirmResponse.xAuthToken,
        )
    } catch (e: SerializationException) {
        ServiceResult.ErrorFromNetwork(e.message ?: "Error")
            .also { Napier.w("Error from network", throwable = e) }
    } catch (e: ResponseException) {
        ServiceResult.ErrorFromNetwork(e.message ?: "Error")
            .also { Napier.w("Error from network", throwable = e) }
    } catch (e: Throwable) {
        ServiceResult.ErrorFromNativeCode(e)
            .also { Napier.w("Error from native code", throwable = e) }
    }

}

data class HttpSessionResponse(
    val body: BindingConfirmResponse,
    val xAuthToken: String
)

interface DeviceAdapter {
    suspend fun loadAttestationCerts(): KmmResult<List<ByteArray>>
    fun storeCertificate(certificate: ByteArray): KmmResult<Boolean>
    suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean>
    fun getPublicKeyEncoded(): KmmResult<ByteArray>
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
