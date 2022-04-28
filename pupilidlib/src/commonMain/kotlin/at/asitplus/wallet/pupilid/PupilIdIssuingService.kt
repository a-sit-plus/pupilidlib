package at.asitplus.wallet.pupilid

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
     * Can be passed to enable mocks in unit tests, to use the default engine, pass `null`
     */
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

    suspend fun issueCredentials(message: String): ServiceResult {
        try {
            val url = URLBuilder(serverAddress).appendPathSegments(serverIssuePath).buildString()
            val initialPost = client.post(url) {
                contentType(ContentType.Text.Plain)
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
                contentType(ContentType.Text.Plain)
                headers {
                    append(HttpHeaders.Authorization, responseHeaderValue)
                }
                setBody(message)
            }
            val response = secondPost.body<String>()

            return ServiceResult.Success(response, secondPost.headers[HeaderNames.X_AUTH_TOKEN])
        } catch (e: ResponseException) {
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error")
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
