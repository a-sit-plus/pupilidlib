package at.asitplus.wallet

import at.asitplus.KmmResult
import at.asitplus.wallet.pupilid.HttpClientBuilder
import at.asitplus.wallet.pupilid.PupilIdIssuingService
import at.asitplus.wallet.pupilid.ServiceResult
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.kotest.matchers.types.shouldBeInstanceOf
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
            httpClientBuilder = HttpClientBuilder(engine = mockEngine, enableLogging = true),
        )
        Napier.base(DebugAntilog())

        runBlocking {
            val result = service.issueCredentials("foo")

            result.shouldBeInstanceOf<ServiceResult.Success>()
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
            httpClientBuilder = HttpClientBuilder(engine = mockEngine),
        )

        runBlocking {
            val result = service.issueCredentials("foo")

            result.shouldBeInstanceOf<ServiceResult.ErrorFromNetwork>()
        }
    }


}