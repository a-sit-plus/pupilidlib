package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import at.asitplus.wallet.utils.HeaderNames
import io.github.aakira.napier.Napier
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Executes the process of receiving a PupilId credential from the backend.
 */
class PupilIdIssuingService(
    /**
     * Base address of the server to contact, i.e. `https://wallet.a-sit.at/`
     */
    private val serverAddress: String,
    /**
     * Needed callback to sign the challenge in an JWS
     */
    private val jwsAdapter: JwsAdapter,
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

    suspend fun issueCredentials(message: String): ServiceResult {
        try {
            val url = URLBuilder(serverAddress).appendPathSegments(serverIssuePath).buildString()
            val initialPost = client.post(url) {
                setBody(message)
                expectSuccess = false
            }
            val authenticateHeader = initialPost.headers[HttpHeaders.WWWAuthenticate]
                ?: throw ResponseException(initialPost, initialPost.bodyAsText())
            val challenge = authenticateHeader.removePrefix("Challenge ")
            val payload = Json.encodeToString(mapOf("challenge" to challenge))
            val signedJws = jwsAdapter.createSignedJwsCallback(payload).getOrThrow()
            val responseHeaderValue = "Response $signedJws"
            val secondPost = client.post(url) {
                headers {
                    append(HttpHeaders.Authorization, responseHeaderValue)
                }
                setBody(message)
                expectSuccess = true
            }
            val response = secondPost.body<String>()

            return ServiceResult.Success(response, secondPost.headers[HeaderNames.X_AUTH_TOKEN])
        } catch (e: SerializationException) {
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error")
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: ResponseException) {
            val details = kotlin.runCatching { e.response.body<SpringErrorDocument>() }.getOrNull()
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error", details)
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: Throwable) {
            return ServiceResult.ErrorFromNativeCode(e)
                .also { Napier.w("Error from native code", throwable = e) }
        }
    }


    fun interface JwsAdapter {
        /**
         * Implementers: Create a signed JWS, containing an `x5c` header with the local device binding certificate
         */
        suspend fun createSignedJwsCallback(payload: String): KmmResult<String>
    }

}
