package at.asitplus.wallet

import at.asitplus.wallet.pupilid.BindingConfirmResponse
import at.asitplus.wallet.pupilid.BindingCsrResponse
import at.asitplus.wallet.pupilid.BindingParamsResponse
import at.asitplus.wallet.pupilid.DeviceBindingPupilIdIssuingService
import at.asitplus.wallet.pupilid.HeaderNames
import at.asitplus.wallet.pupilid.KmmResult
import at.asitplus.wallet.pupilid.ServiceResult
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class DeviceBindingPupilIdIssuingServiceTest {

    private val xAuthToken = Random.nextInt().toString(16)
    private val xAuthExtNonce = Random.nextInt().toString(16)
    private val randomResponse = Random.nextInt().toString(16)
    private val jsonHeadersWithXAuthToken = headersOf(
        HttpHeaders.ContentType to listOf("application/json"),
        HeaderNames.X_AUTH_TOKEN to listOf(xAuthToken)
    )

    @Test
    fun success() {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/binding/start") && request.headers[HeaderNames.X_AUTH_EXT_NONCE] == xAuthExtNonce -> {
                    val content =
                        Json.encodeToString(BindingParamsResponse(byteArrayOf(), "subject", "EC"))
                    respond(content, HttpStatusCode.OK, jsonHeadersWithXAuthToken)
                }
                request.url.encodedPath.endsWith("/binding/create") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    val content = Json.encodeToString(
                        BindingCsrResponse(
                            byteArrayOf(),
                            "attested-public-key"
                        )
                    )
                    respond(content, HttpStatusCode.OK, jsonHeadersWithXAuthToken)
                }
                request.url.encodedPath.endsWith("/binding/confirm") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    val content = Json.encodeToString(BindingConfirmResponse(true))
                    respond(content, HttpStatusCode.OK, jsonHeadersWithXAuthToken)
                }
                request.url.encodedPath.endsWith("/pupilid/issue") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    val headers = headersOf(HttpHeaders.ContentType, "application/text")
                    respond(randomResponse, HttpStatusCode.OK, headers)
                }
                else -> {
                    error("Unhandled call")
                }
            }
        }
        val service = DeviceBindingPupilIdIssuingService(
            serverAddress = "http://localhost/",
            extAuthNonce = xAuthExtNonce,
            deviceAdapter = DummyDeviceAdapter,
            cryptoAdapter = NoopCryptoAdapter,
            callback = { KmmResult.success("foo") },
            engine = mockEngine,
        )

        runBlocking {
            val result = service.createDeviceBindingAndIssueCredentials()

            assertSuccess(result)
        }
    }

    @Test
    fun errorOnIssue() {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/binding/start") && request.headers[HeaderNames.X_AUTH_EXT_NONCE] == xAuthExtNonce -> {
                    val content =
                        Json.encodeToString(BindingParamsResponse(byteArrayOf(), "subject", "EC"))
                    respond(content, HttpStatusCode.OK, jsonHeadersWithXAuthToken)
                }
                request.url.encodedPath.endsWith("/binding/create") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    val content = Json.encodeToString(
                        BindingCsrResponse(
                            byteArrayOf(),
                            "attested-public-key"
                        )
                    )
                    respond(content, HttpStatusCode.OK, jsonHeadersWithXAuthToken)
                }
                request.url.encodedPath.endsWith("/binding/confirm") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    val content = Json.encodeToString(BindingConfirmResponse(true))
                    respond(content, HttpStatusCode.OK, jsonHeadersWithXAuthToken)
                }
                request.url.encodedPath.endsWith("/pupilid/issue") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    respond("", HttpStatusCode.Unauthorized, jsonHeadersWithXAuthToken)
                }
                else -> {
                    error("Unhandled call")
                }
            }
        }
        val service = DeviceBindingPupilIdIssuingService(
            serverAddress = "http://localhost/",
            extAuthNonce = xAuthExtNonce,
            deviceAdapter = DummyDeviceAdapter,
            cryptoAdapter = NoopCryptoAdapter,
            callback = { KmmResult.success("foo") },
            engine = mockEngine,
        )

        runBlocking {
            val result = service.createDeviceBindingAndIssueCredentials()

            assertErrorFromNetwork(result)
        }
    }

    private fun assertErrorFromNetwork(result: ServiceResult) {
        when (result) {
            is ServiceResult.ErrorFromNativeCode -> fail(result.throwable?.toString() ?: "NULL ERROR")
            is ServiceResult.ErrorFromNetwork -> assertTrue(true)
            is ServiceResult.Success -> fail("Unexpected success")
        }
    }

    private fun assertSuccess(result: ServiceResult) {
        when (result) {
            is ServiceResult.ErrorFromNativeCode -> fail(
                result.throwable?.toString() ?: "NULL ERROR"
            )
            is ServiceResult.ErrorFromNetwork -> fail(result.error)
            is ServiceResult.Success -> assertTrue(true)
        }
    }
}