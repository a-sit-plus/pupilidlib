package at.asitplus.wallet

import at.asitplus.wallet.lib.KmmResult
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class PupilIdIssuingServiceTest {

    private val randomChallenge = Random.nextInt().toString(16)
    private val randomResponse = Random.nextInt().toString(16)
    private val mockEngine = MockEngine { request ->
        when {
            request.url.encodedPath.endsWith("/pupilid/issue") && !request.headers.contains(HttpHeaders.Authorization) -> {
                val headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/text"),
                    HttpHeaders.WWWAuthenticate to listOf("Challenge $randomChallenge")
                )
                respond(randomResponse, HttpStatusCode.Unauthorized, headers)
            }
            request.url.encodedPath.endsWith("/pupilid/issue") && request.headers.contains(HttpHeaders.Authorization) -> {
                val headers = headersOf(HttpHeaders.ContentType, "application/text")
                respond(randomResponse, HttpStatusCode.OK, headers)
            }
            else -> {
                error("Unhandled call")
            }
        }
    }

    @Test
    fun success() {
        val noopCryptoAdapter = object : PupilIdIssuingService.CryptoAdapter {
            override fun getCertificateChain(): KmmResult<Array<ByteArray>> = KmmResult.success(arrayOf())
        }
        val service = PupilIdIssuingService(
            serverAddress = "http://localhost/",
            cryptoService = DefaultCryptoService(),
            cryptoAdapter = noopCryptoAdapter,
            engine = mockEngine,
        )

        runBlocking {
            val result = service.issueCredentials("foo")

            assertSuccess(result)
        }
    }

    private fun assertSuccess(result: ServiceResult) {
        when (result) {
            is ServiceResult.ErrorFromNativeCode -> fail(result.throwable?.toString() ?: "NULL ERROR")
            is ServiceResult.ErrorFromNetwork -> fail(result.error)
            is ServiceResult.Success -> assertTrue(true)
        }
    }
}