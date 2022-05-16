package at.asitplus.wallet

import at.asitplus.wallet.Assertions.assertErrorFromNativeCode
import at.asitplus.wallet.Assertions.assertErrorFromNetwork
import at.asitplus.wallet.Assertions.assertSuccess
import at.asitplus.wallet.pupilid.AttestedPublicKey
import at.asitplus.wallet.pupilid.BindingConfirmResponse
import at.asitplus.wallet.pupilid.BindingCsrResponse
import at.asitplus.wallet.pupilid.BindingParamsResponse
import at.asitplus.wallet.pupilid.DeviceBindingService
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test

class DeviceBindingServiceTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val attestedPublicKeyKid = Random.nextBytes(32).encodeBase64()

    val successMockEngine = MockEngine { request ->
        when {
            request.url.encodedPath.endsWith("/binding/start") -> {
                val content = Json.encodeToString(BindingParamsResponse(byteArrayOf(), "subject", "EC"))
                respond(content, HttpStatusCode.OK, jsonHeaders)
            }
            request.url.encodedPath.endsWith("/binding/create") -> {
                val csrResponse = BindingCsrResponse(byteArrayOf(), AttestedPublicKey(attestedPublicKeyKid, 1).serialize())
                val content = Json.encodeToString(csrResponse)
                respond(content, HttpStatusCode.OK, jsonHeaders)
            }
            request.url.encodedPath.endsWith("/binding/confirm") -> {
                val content = Json.encodeToString(BindingConfirmResponse(true))
                respond(content, HttpStatusCode.OK, jsonHeaders)
            }
            else -> {
                error("Unhandled call")
            }
        }
    }

    @Test
    fun success() {
        val service = DeviceBindingService(
            serverAddress = "http://localhost/",
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            deviceAdapter = AssertKidDeviceAdapter(attestedPublicKeyKid),
            cryptoAdapter = NoopCryptoAdapter,
            engine = successMockEngine
        )

        runBlocking {
            val result = service.createDeviceBinding()

            assertSuccess(result)
        }
    }

    @Test
    fun unknownResponse() {
        val mockEngine = MockEngine {
            respond(ByteReadChannel("""{"foo":"bar"}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = DummyDeviceAdapter,
            cryptoAdapter = NoopCryptoAdapter,
            engine = mockEngine
        )

        runBlocking {
            val result = service.createDeviceBinding()

            assertErrorFromNetwork(result)
        }
    }

    @Test
    fun serverErrorResponse() {
        val mockEngine = MockEngine {
            respond(ByteReadChannel("""{"foo":"bar"}"""), HttpStatusCode.InternalServerError, jsonHeaders)
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = DummyDeviceAdapter,
            cryptoAdapter = NoopCryptoAdapter,
            engine = mockEngine
        )

        runBlocking {
            val result = service.createDeviceBinding()

            assertErrorFromNetwork(result)
        }
    }

    @Test
    fun springBootErrorResponse() {
        val mockEngine = MockEngine {
            respond(
                """
                {
                 "timestamp": 1500597044204,
                 "status": 400,
                 "error": "Bad Request",
                 "exception": "org.springframework.http.converter.HttpMessageNotReadableException",
                 "message": "JSON parse error",
                 "path": "/foo"
                }
            """.trimIndent(), HttpStatusCode.BadRequest, jsonHeaders
            )
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = DummyDeviceAdapter,
            cryptoAdapter = NoopCryptoAdapter,
            engine = mockEngine
        )

        runBlocking {
            val result = service.createDeviceBinding()

            assertErrorFromNetwork(result)
        }
    }

    @Test
    fun errorFromNativeCode() {
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = ErrorThrowingDeviceAdapter,
            cryptoAdapter = NoopCryptoAdapter,
            engine = successMockEngine
        )

        runBlocking {
            val result = service.createDeviceBinding()

            assertErrorFromNativeCode(result)
        }
    }

}

