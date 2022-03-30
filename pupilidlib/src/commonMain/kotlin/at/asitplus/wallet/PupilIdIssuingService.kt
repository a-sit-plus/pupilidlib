package at.asitplus.wallet

import at.asitplus.wallet.lib.KmmResult
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.jws.DefaultJwsService
import at.asitplus.wallet.lib.jws.JwsHeader
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString

/**
 * @param engine can be passed to enable mocks in unit tests, to use the default engine, pass null
 */
class PupilIdIssuingService(
    private val serverAddress: String,
    private val cryptoService: CryptoService,
    private val cryptoAdapter: CryptoAdapter,
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
            val initialPost = client.post("$serverAddress/pupilid/issue") {
                contentType(ContentType.Text.Plain)
                setBody(message)
                expectSuccess = false
            }
            val authenticateHeader = initialPost.headers[HttpHeaders.WWWAuthenticate]
                ?: throw ResponseException(initialPost, initialPost.bodyAsText())
            val challenge = authenticateHeader.removePrefix("Challenge ")
            val payload = jsonSerializer.encodeToString(mapOf("challenge" to challenge))
            val certificateChain = cryptoAdapter.getCertificateChain().getOrNull()
                ?: throw IllegalArgumentException("No certificate chain from native code")
            val jwsHeader = JwsHeader(
                algorithm = cryptoService.jwsAlgorithm,
                keyId = cryptoService.keyId,
                certificateChain = certificateChain,
            )
            val signedJws = DefaultJwsService(cryptoService)
                .createSignedJws(jwsHeader, payload.encodeToByteArray())
                ?: throw IllegalArgumentException("No JWS from native code")
            val responseHeaderValue = "Response $signedJws"

            val secondPost = client.post("$serverAddress/pupilid/issue") {
                contentType(ContentType.Text.Plain)
                headers {
                    append(HttpHeaders.Authorization, responseHeaderValue)
                }
                setBody(message)
            }
            val response = secondPost.body<String>()

            return ServiceResult.Success(response)
        } catch (e: ResponseException) {
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error")
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: Throwable) {
            return ServiceResult.ErrorFromNativeCode(e)
                .also { Napier.w("Error from native code", throwable = e) }
        }
    }

    interface CryptoAdapter {
        fun getCertificateChain(): KmmResult<Array<ByteArray>>
    }

}