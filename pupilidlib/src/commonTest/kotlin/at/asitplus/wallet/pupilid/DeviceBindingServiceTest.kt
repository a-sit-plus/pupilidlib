package at.asitplus.wallet.pupilid

import at.asitplus.wallet.pupilid.testmocks.AssertKidDeviceAdapter
import at.asitplus.wallet.pupilid.testmocks.DummyDeviceAdapter
import at.asitplus.wallet.pupilid.testmocks.ErrorThrowingDeviceAdapter
import at.asitplus.wallet.pupilid.testmocks.NoopCryptoAdapter
import io.kotest.core.spec.style.StringSpec
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

class DeviceBindingServiceTest : StringSpec({

    val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    val attestedPublicKeyKid = Random.nextBytes(32).encodeBase64()

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

    "success" {
        val service = DeviceBindingService(
            serverAddress = "http://localhost/",
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            deviceAdapter = AssertKidDeviceAdapter(attestedPublicKeyKid),
            cryptoAdapter = NoopCryptoAdapter,
            httpClientBuilder = HttpClientBuilder(engine = successMockEngine),
        )

        runBlocking {
            val result = service.createDeviceBinding()

            result.shouldBeInstanceOf<ServiceResult.Success>()
        }
    }

    "unknownResponse" {
        val mockEngine = MockEngine {
            respond(ByteReadChannel("""{"foo":"bar"}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = DummyDeviceAdapter,
            cryptoAdapter = NoopCryptoAdapter,
            httpClientBuilder = HttpClientBuilder(engine = mockEngine),
        )

        runBlocking {
            val result = service.createDeviceBinding()

            result.shouldBeInstanceOf<ServiceResult.ErrorFromNativeCode>()
        }
    }

    "serverErrorResponse" {
        val mockEngine = MockEngine {
            respond(ByteReadChannel("""{"foo":"bar"}"""), HttpStatusCode.InternalServerError, jsonHeaders)
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = DummyDeviceAdapter,
            cryptoAdapter = NoopCryptoAdapter,
            httpClientBuilder = HttpClientBuilder(engine = mockEngine),
        )

        runBlocking {
            val result = service.createDeviceBinding()

            result.shouldBeInstanceOf<ServiceResult.ErrorFromNetwork>()
        }
    }

    "springBootErrorResponse" {
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
            httpClientBuilder = HttpClientBuilder(engine = mockEngine),
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

    "errorFromNativeCode" {
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = ErrorThrowingDeviceAdapter,
            cryptoAdapter = NoopCryptoAdapter,
            httpClientBuilder = HttpClientBuilder(engine = successMockEngine),
        )

        runBlocking {
            val result = service.createDeviceBinding()

            result.shouldBeInstanceOf<ServiceResult.ErrorFromNativeCode>()
        }
    }

})