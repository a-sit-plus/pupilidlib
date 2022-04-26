package at.asitplus.wallet

import at.asitplus.wallet.Assertions.assertErrorFromNetwork
import at.asitplus.wallet.Assertions.assertSuccess
import at.asitplus.wallet.pupilid.KmmResult
import at.asitplus.wallet.pupilid.PupilIdIssuingService
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test

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
        val service = PupilIdIssuingService(
            serverAddress = "http://localhost/",
            jwsAdapter = { payload -> KmmResult.success(payload) },
            engine = mockEngine,
        )

        runBlocking {
            val result = service.issueCredentials("foo")

            assertSuccess(result)
        }
    }

    @Test
    fun unexpectedResponse() {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/pupilid/issue") && !request.headers.contains(HttpHeaders.Authorization) -> {
                    val headers = headersOf(
                        HttpHeaders.ContentType to listOf("application/text")
                    )
                    respond(randomResponse, HttpStatusCode.InternalServerError, headers)
                }
                else -> {
                    error("Unhandled call")
                }
            }
        }
        val service = PupilIdIssuingService(
            serverAddress = "http://localhost/",
            jwsAdapter = { payload -> KmmResult.success(payload) },
            engine = mockEngine,
        )

        runBlocking {
            val result = service.issueCredentials("foo")

            assertErrorFromNetwork(result)
        }
    }


}