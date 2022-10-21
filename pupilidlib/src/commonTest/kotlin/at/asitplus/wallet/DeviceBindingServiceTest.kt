package at.asitplus.wallet

import at.asitplus.wallet.pupilid.AttestedPublicKey
import at.asitplus.wallet.pupilid.BindingConfirmResponse
import at.asitplus.wallet.pupilid.BindingCsrResponse
import at.asitplus.wallet.pupilid.BindingParamsResponse
import at.asitplus.wallet.pupilid.DeviceBindingService
import at.asitplus.wallet.pupilid.ServiceResult
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
                val csrResponse =
                    BindingCsrResponse(byteArrayOf(), AttestedPublicKey(attestedPublicKeyKid).serialize())
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

            result.shouldBeInstanceOf<ServiceResult.Success>()
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

            result.shouldBeInstanceOf<ServiceResult.ErrorFromNativeCode>()
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

            result.shouldBeInstanceOf<ServiceResult.ErrorFromNetwork>()
        }
    }

    @Test
    fun springBootErrorResponse() {
        val randomStatus = Random.nextInt(100, 599)
        val randomErrorMessage = Random.nextBytes(32).encodeBase64()
        val mockEngine = MockEngine {
            respond(
                """
                {
                    "timestamp":"2022-05-20T12:18:42.055+00:00",
                    "status":$randomStatus,
                    "error":"Unauthorized",
                    "path":"/binding/create",
                    "message": "$randomErrorMessage"
                }
                """.trimIndent(), HttpStatusCode.Unauthorized, jsonHeaders
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

            result.shouldBeInstanceOf<ServiceResult.ErrorFromNetwork>()
            val details = result.details
            details.shouldNotBeNull()
            details.message shouldBe randomErrorMessage
            details.status shouldBe randomStatus
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

            result.shouldBeInstanceOf<ServiceResult.ErrorFromNativeCode>()
        }
    }

}

