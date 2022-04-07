package at.asitplus.wallet

import at.asitplus.wallet.pupilid.BindingConfirmResponse
import at.asitplus.wallet.pupilid.BindingCsrResponse
import at.asitplus.wallet.pupilid.BindingParamsResponse
import at.asitplus.wallet.pupilid.DeviceBindingService
import at.asitplus.wallet.pupilid.KmmResult
import at.asitplus.wallet.pupilid.ServiceResult
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

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun success() {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/binding/start") -> {
                    val content = Json.encodeToString(BindingParamsResponse(byteArrayOf(), "subject", "EC"))
                    respond(content, HttpStatusCode.OK, jsonHeaders)
                }
                request.url.encodedPath.endsWith("/binding/create") -> {
                    val content = Json.encodeToString(BindingCsrResponse(byteArrayOf(), "attested-public-key"))
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
        val service = DeviceBindingService(
            serverAddress = "http://localhost/",
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            deviceAdapter = DummyDeviceAdapter,
            signCallback = { _, _, _ -> KmmResult.success(byteArrayOf()) },
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
            respond(ByteReadChannel("""{"foo":"bar"}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = DummyDeviceAdapter,
            signCallback = { _, _, _ -> KmmResult.success(byteArrayOf()) },
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
            respond(ByteReadChannel("""{"foo":"bar"}"""), HttpStatusCode.InternalServerError, jsonHeaders)
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = DummyDeviceAdapter,
            signCallback = { _, _, _ -> KmmResult.success(byteArrayOf()) },
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
            respond(ByteReadChannel("""{"foo":"bar"}"""), HttpStatusCode.OK, jsonHeaders)
        }
        val service = DeviceBindingService(
            extAuthNonce = "ext-auth-nonce-" + Random.nextInt(),
            serverAddress = "http://localhost/",
            deviceAdapter = ErrorThrowingDeviceAdapter,
            signCallback = { _, _, _ -> KmmResult.success(byteArrayOf()) },
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