package at.asitplus.wallet

import at.asitplus.KmmResult
import at.asitplus.wallet.pupilid.AttestedPublicKey
import at.asitplus.wallet.pupilid.BindingConfirmResponse
import at.asitplus.wallet.pupilid.BindingCsrResponse
import at.asitplus.wallet.pupilid.BindingParamsResponse
import at.asitplus.wallet.pupilid.DeviceBindingPupilIdIssuingService
import at.asitplus.wallet.pupilid.HeaderNames
import at.asitplus.wallet.pupilid.HttpClientBuilder
import at.asitplus.wallet.pupilid.ServiceResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test

class DeviceBindingPupilIdIssuingServiceTest {

    private val xAuthToken = Random.nextInt().toString(16)
    private val xAuthExtNonce = Random.nextInt().toString(16)
    private val randomResponse = Random.nextInt().toString(16)
    private val attestedPublicKeyKid = Random.nextBytes(32).encodeBase64()
    private val jsonHeaders = headersOf(
        HttpHeaders.ContentType to listOf("application/json"),
    )
    private val jsonHeadersWithXAuthToken = headersOf(
        HttpHeaders.ContentType to listOf("application/json"),
        HeaderNames.X_AUTH_TOKEN to listOf(xAuthToken)
    )
    private val textHeaders = headersOf(HttpHeaders.ContentType, "application/text")

    @Test
    fun success() {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/binding/start") && request.headers[HeaderNames.X_AUTH_EXT_NONCE] == xAuthExtNonce -> {
                    val content = Json.encodeToString(BindingParamsResponse(byteArrayOf(), "subject", "EC"))
                    respond(content, HttpStatusCode.OK, jsonHeadersWithXAuthToken)
                }
                request.url.encodedPath.endsWith("/binding/create") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    val csrResponse =
                        BindingCsrResponse(byteArrayOf(), AttestedPublicKey(attestedPublicKeyKid).serialize())
                    val content = Json.encodeToString(csrResponse)
                    respond(content, HttpStatusCode.OK, jsonHeaders)
                }
                request.url.encodedPath.endsWith("/binding/confirm") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    val content = Json.encodeToString(BindingConfirmResponse(true))
                    respond(content, HttpStatusCode.OK, jsonHeaders)
                }
                request.url.encodedPath.endsWith("/pupilid/issue") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    respond(randomResponse, HttpStatusCode.OK, textHeaders)
                }
                else -> {
                    error("Unhandled call")
                }
            }
        }
        val service = DeviceBindingPupilIdIssuingService(
            serverAddress = "http://localhost/",
            extAuthNonce = xAuthExtNonce,
            deviceAdapter = AssertKidDeviceAdapter(attestedPublicKeyKid),
            cryptoAdapter = NoopCryptoAdapter,
            callback = { KmmResult.success("foo") },
            httpClientBuilder = HttpClientBuilder(engine = mockEngine),
        )

        runBlocking {
            val result = service.createDeviceBindingAndIssueCredentials()

            result.shouldBeInstanceOf<ServiceResult.Success>()
            result.xAuthToken shouldBe xAuthToken
        }
    }

    @Test
    fun errorOnIssue() {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/binding/start") && request.headers[HeaderNames.X_AUTH_EXT_NONCE] == xAuthExtNonce -> {
                    val content = Json.encodeToString(BindingParamsResponse(byteArrayOf(), "subject", "EC"))
                    respond(content, HttpStatusCode.OK, jsonHeadersWithXAuthToken)
                }
                request.url.encodedPath.endsWith("/binding/create") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    val content = Json.encodeToString(BindingCsrResponse(byteArrayOf(), "attested-public-key"))
                    respond(content, HttpStatusCode.OK, jsonHeaders)
                }
                request.url.encodedPath.endsWith("/binding/confirm") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    val content = Json.encodeToString(BindingConfirmResponse(true))
                    respond(content, HttpStatusCode.OK, jsonHeaders)
                }
                request.url.encodedPath.endsWith("/pupilid/issue") && request.headers[HeaderNames.X_AUTH_TOKEN] == xAuthToken -> {
                    respond("", HttpStatusCode.Unauthorized, textHeaders)
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
            httpClientBuilder = HttpClientBuilder(engine = mockEngine),
        )

        runBlocking {
            val result = service.createDeviceBindingAndIssueCredentials()

            result.shouldBeInstanceOf<ServiceResult.ErrorFromNetwork>()
        }
    }

}