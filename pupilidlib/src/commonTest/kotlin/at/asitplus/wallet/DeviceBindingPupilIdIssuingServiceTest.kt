package at.asitplus.wallet

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

    @Test
    fun success() {
        val xAuthToken = Random.nextInt().toString(16)
        val xAuthExtNonce = Random.nextInt().toString(16)
        val randomResponse = Random.nextInt().toString(16)
        val mockEngine = MockEngine { request ->
            val jsonHeadersWithXAuthToken = headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                HeaderNames.X_AUTH_TOKEN to listOf(xAuthToken)
            )
            when {
                request.url.encodedPath.endsWith("/binding/start") && request.headers[HeaderNames.X_AUTH_EXT_NONCE] == xAuthExtNonce -> {
                    val content = Json.encodeToString(BindingParamsResponse(byteArrayOf(), "subject", "EC"))
                    respond(content, HttpStatusCode.OK, jsonHeadersWithXAuthToken)
                }
                request.url.encodedPath.endsWith("/binding/create") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    val content = Json.encodeToString(BindingCsrResponse(byteArrayOf(), "attested-public-key"))
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
            asn1Service = Asn1Service(NoopCryptoAdapter),
            engine = mockEngine,
        )

        runBlocking {
            val result = service.createDeviceBindingAndIssueCredentials()

            assertSuccess(result)
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