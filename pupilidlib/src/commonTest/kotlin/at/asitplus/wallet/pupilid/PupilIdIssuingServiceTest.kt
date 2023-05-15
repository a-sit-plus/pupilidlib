package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

class PupilIdIssuingServiceTest : StringSpec({

    val randomChallenge = Random.nextInt().toString(16)
    val randomResponse = Random.nextInt().toString(16)

    val mockEngine = MockEngine { request ->
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

    "success" {
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

    "unexpectedResponse" {
        val newMockEngine = MockEngine { request ->
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
            httpClientBuilder = HttpClientBuilder(engine = newMockEngine),
        )

        runBlocking {
            val result = service.issueCredentials("foo")

            result.shouldBeInstanceOf<ServiceResult.ErrorFromNetwork>()
        }
    }

})