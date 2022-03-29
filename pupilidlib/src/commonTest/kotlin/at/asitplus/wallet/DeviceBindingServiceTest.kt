package at.asitplus.wallet

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class DeviceBindingServiceTest {

    @Test
    fun success() {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/binding/start") -> {
                    respond(
                        Json.encodeToString(BindingParamsResponse(byteArrayOf(), "subject", "EC")),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                request.url.encodedPath.endsWith("/binding/create") -> {
                    respond(
                        Json.encodeToString(BindingCsrResponse(byteArrayOf(), "attested-public-key")),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                request.url.encodedPath.endsWith("/binding/confirm") -> {
                    respond(
                        Json.encodeToString(BindingConfirmResponse(true)),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> {
                    error("Unhandled call")
                }
            }
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = DummyDeviceAdapter,
            asn1Service = Asn1Service(NoopCryptoAdapter),
            engine = mockEngine
        )

        runBlocking {
            val result = service.createDeviceBinding()

            assertSuccess(result)
        }
    }

    @Test
    fun unknownResponse() {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"foo":"bar"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = DummyDeviceAdapter,
            asn1Service = Asn1Service(NoopCryptoAdapter),
            engine = mockEngine
        )

        runBlocking {
            val result = service.createDeviceBinding()

            assertErrorFromNativeCode(result)
        }
    }

    @Test
    fun serverErrorResponse() {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"foo":"bar"}"""),
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = DummyDeviceAdapter,
            asn1Service = Asn1Service(NoopCryptoAdapter),
            engine = mockEngine
        )

        runBlocking {
            val result = service.createDeviceBinding()

            assertErrorFromNetwork(result)
        }
    }

    @Test
    fun errorFromNativeCode() {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"foo":"bar"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = ErrorThrowingDeviceAdapter,
            asn1Service = Asn1Service(NoopCryptoAdapter),
            engine = mockEngine
        )

        runBlocking {
            val result = service.createDeviceBinding()

            assertErrorFromNativeCode(result)
        }
    }

    private fun assertErrorFromNativeCode(result: ServiceResult) {
        when (result) {
            is ServiceResult.ErrorFromNativeCode -> assertTrue(true)
            is ServiceResult.ErrorFromNetwork -> fail(result.error)
            is ServiceResult.Success -> fail("Unexpected success")
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
            is ServiceResult.ErrorFromNativeCode -> fail(result.throwable?.toString() ?: "NULL ERROR")
            is ServiceResult.ErrorFromNetwork -> fail(result.error)
            is ServiceResult.Success -> assertTrue(true)
        }
    }
}