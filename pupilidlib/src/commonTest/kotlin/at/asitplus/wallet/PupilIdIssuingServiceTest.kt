package at.asitplus.wallet

import at.asitplus.wallet.lib.agent.DefaultCryptoService
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class PupilIdIssuingServiceTest {

    @Test
    fun success() {
        var requestCounter = 0
        val randomResponse = Random.nextInt().toString(16)
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/pupilid/issue") && requestCounter == 0 -> {
                    requestCounter++
                    val headers = headersOf(
                        HttpHeaders.ContentType to listOf("application/text"),
                        HttpHeaders.WWWAuthenticate to listOf("Challenge bar")
                    )
                    respond(randomResponse, HttpStatusCode.Unauthorized, headers)
                }
                request.url.encodedPath.endsWith("/pupilid/issue") && requestCounter == 1 -> {
                    requestCounter++
                    respond(randomResponse, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/text"))
                }
                else -> {
                    error("Unhandled call")
                }
            }
        }
        val cryptoAdapter = object : PupilIdIssuingService.CryptoAdapter {
            override fun getCertificateChain(): KmmResult<Array<ByteArray>> {
                return KmmResult.success(arrayOf())
            }
        }
        val service = PupilIdIssuingService(
            serverAddress = "http://localhost/",
            cryptoService = DefaultCryptoService(),
            cryptoAdapter = cryptoAdapter,
            engine = mockEngine,
        )

        runBlocking {
            val result = service.issueCredentials("foo")

            assertSuccess(result)
        }
    }

    private fun assertSuccess(result: PupilIdIssuingService.Result) {
        when (result) {
            is PupilIdIssuingService.Result.ErrorFromNativeCode -> fail(result.throwable?.toString() ?: "NULL ERROR")
            is PupilIdIssuingService.Result.ErrorFromNetwork -> fail(result.error)
            is PupilIdIssuingService.Result.Success -> assertTrue(true)
        }
    }
}