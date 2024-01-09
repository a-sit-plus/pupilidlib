package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import at.asitplus.wallet.pupilid.DeviceBindingService.AuthMethod
import at.asitplus.wallet.utils.Asn1Service
import at.asitplus.wallet.utils.HeaderNames
import io.github.aakira.napier.Napier
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException

/**
 * Executes the device binding process, immediately followed by requesting a PupilId Credential.
 *
 * @see [DeviceBindingService]
 * @see [PupilIdIssuingService]
 */
class DeviceBindingPupilIdIssuingService(
    /**
     * Base address of the server to contact, i.e. `https://wallet.a-sit.at/`
     */
    private val serverAddress: String,
    /**
     * Authentication method at backend, see [AuthMethod]
     */
    authMethod: AuthMethod,
    /**
     * Needed to load information from native implementations
     */
    deviceAdapter: DeviceAdapter,
    /**
     * Needed to create signature of the CSR
     */
    cryptoAdapter: Asn1Service.CryptoAdapter,
    /**
     * Needed to get the message to send from native callers
     */
    private val callback: Callback,
    /**
     * Path of the endpoint for issuing credentials, defaults to `listOf("pupilid", "issue")`
     */
    private val serverIssuePath: List<String> = listOf("pupilid", "issue"),
    /**
     * Use this to configure an HTTP client once
     */
    httpClientBuilder: HttpClientBuilder,
) {

    private val client = httpClientBuilder.client
    private val deviceBindingService = DeviceBindingService(
        serverAddress = serverAddress,
        authMethod = authMethod,
        deviceAdapter = deviceAdapter,
        cryptoAdapter = cryptoAdapter,
        httpClientBuilder = httpClientBuilder,
    )

    suspend fun createDeviceBindingAndIssueCredentials(): ServiceResult {
        try {
            val url = URLBuilder(serverAddress).appendPathSegments(listOf("pupilid", "issue")).buildString()
            Napier.d("Server Address = $serverAddress")
            Napier.d("Server Issue Path = $serverIssuePath")
            Napier.d("url = $url")

            val deviceBindingResponse = deviceBindingService.getDeviceBindingResponse()
            Napier.d("deviceBindingResponse = $deviceBindingResponse")

            var xAuthToken = deviceBindingResponse.xAuthToken
            Napier.d("xAuthToken = $xAuthToken")

            val bindingConfirmBody = deviceBindingResponse.body
            Napier.d("bindingConfirmBody = $bindingConfirmBody")

            if (!bindingConfirmBody.success) {
                return ServiceResult.ErrorFromNetwork("No success for binding/confirm message")
            }

            val message = callback.loadMessage().getOrThrow()
            Napier.d("message = $message")

            val issuePost = client.post(url) {
                headers {
                    append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
                }
                setBody(message)
                expectSuccess = true
            }
            Napier.d("issuePost = $issuePost")

            val response = issuePost.body<String>()

            Napier.d("Response = $response")

            xAuthToken =
                issuePost.headers[HeaderNames.X_AUTH_TOKEN]?.ifBlank { xAuthToken } ?: xAuthToken

            Napier.d("xAuthToken = $xAuthToken")


            return ServiceResult.Success(response, xAuthToken)
        } catch (e: SerializationException) {
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error")
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: ResponseException) {
            val details = kotlin.runCatching { e.response.body<SpringErrorDocument>() }.getOrNull()
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error", details)
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: CryptoException) {
            return ServiceResult.SignatureInvalid
                .also { Napier.w("binding not signed by trusted issuer", throwable = e) }
        } catch (e: Throwable) {
            return ServiceResult.ErrorFromNativeCode(e)
                .also { Napier.w("Error from native code", throwable = e) }
        }
    }

    fun interface Callback {
        /**
         * Implementers: Return the message of `IssueCredentialMessenger.startDirect()`
         */
        suspend fun loadMessage(): KmmResult<String>
    }

}