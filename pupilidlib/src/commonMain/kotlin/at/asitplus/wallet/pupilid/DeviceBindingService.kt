package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import at.asitplus.wallet.utils.Asn1Service
import at.asitplus.wallet.utils.HeaderNames
import io.github.aakira.napier.Napier
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.SerializationException

/**
 * Executes the device binding process on a backend.
 */
class DeviceBindingService(
    /**
     * Base address of the server to contact, i.e. `https://wallet.a-sit.at/`
     */
    private val serverAddress: String,
    /**
     * Authentication method at backend, see [AuthMethod]
     */
    private val authMethod: AuthMethod,
    /**
     * Needed to load information from native implementations
     */
    private val deviceAdapter: DeviceAdapter,
    /**
     * Needed to create signature of the CSR
     */
    cryptoAdapter: Asn1Service.CryptoAdapter,
    /**
     * Use this to configure an HTTP client once
     */
    httpClientBuilder: HttpClientBuilder,
) {

    /**
     * Selects which auth method to use at the backend for the initial call.
     */
    sealed class AuthMethod {
        /**
         * Sends `nonce` as `X-Auth-ExtNonce` ([HeaderNames.X_AUTH_EXT_NONCE]) to the backend.
         * Use this when initializing the Wallet App with a scanned QR Code.
         */
        data class ExternalNonce(val nonce: String) : AuthMethod()

        /**
         * Sends `token` as `X-Auth-Token` ([HeaderNames.X_AUTH_TOKEN]) to the backend.
         * Use this when initializing the Wallet App after ID Austria authn, i.e. when a session at the backend exists.
         */
        data class XAuthToken(val token: String) : AuthMethod()
    }

    private val asn1Service = Asn1Service(cryptoAdapter)
    private val client = httpClientBuilder.client

    @Throws(ResponseException::class, Throwable::class)
    internal suspend fun getDeviceBindingResponse(): HttpSessionResponse {
        val bindingParamsRequest = BindingParamsRequest(deviceAdapter.deviceName)
        val startResponse =
            client.post(URLBuilder(serverAddress).appendPathSegments("binding", "start").buildString()) {
                contentType(ContentType.Application.Json)
                headers {
                    when (authMethod) {
                        is AuthMethod.ExternalNonce -> append(HeaderNames.X_AUTH_EXT_NONCE, authMethod.nonce)
                        is AuthMethod.XAuthToken -> append(HeaderNames.X_AUTH_TOKEN, authMethod.token)
                    }
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
        val attestationCerts = deviceAdapter.loadAttestationCerts(challenge, publicKeyEncoded).getOrThrow()
        val bindingCsrRequest = BindingCsrRequest(challenge, csr, deviceAdapter.deviceName, attestationCerts)
        val createResponse =
            client.post(URLBuilder(serverAddress).appendPathSegments("binding", "create").buildString()) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
                }
                setBody(bindingCsrRequest)
                expectSuccess = true

            }
        xAuthToken = createResponse.headers[HeaderNames.X_AUTH_TOKEN]?.ifBlank { xAuthToken } ?: xAuthToken
        val bindingCsrResponse = createResponse.body<BindingCsrResponse>()
        deviceAdapter.storeCertificate(bindingCsrResponse.certificate, bindingCsrResponse.attestedPublicKey)
            .getOrThrow()
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
        xAuthToken = confirmResponse.headers[HeaderNames.X_AUTH_TOKEN]?.ifBlank { xAuthToken } ?: xAuthToken
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
        val details = kotlin.runCatching { e.response.body<SpringErrorDocument>() }.getOrNull()
        ServiceResult.ErrorFromNetwork(e.message ?: "Error", details)
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
    suspend fun loadAttestationCerts(challenge: ByteArray, clientData: ByteArray): KmmResult<List<ByteArray>>
    fun storeCertificate(certificate: ByteArray, attestedPublicKey: String?): KmmResult<Boolean>
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
