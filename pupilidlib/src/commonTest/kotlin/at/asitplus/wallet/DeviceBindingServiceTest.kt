package at.asitplus.wallet

import at.asitplus.wallet.lib.KmmResult
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

    object NoopCryptoAdapter : Asn1Service.CryptoAdapter {
        override suspend fun sign(input: ByteArray, key: KeyAlgorithm, hash: HashAlgorithm) =
            KmmResult.success(byteArrayOf())
    }

    object DummyDeviceAdapter : DeviceAdapter {
        override suspend fun loadAttestationCerts(): KmmResult<List<ByteArray>> = KmmResult.success(listOf())
        override fun storeCertificate(certificate: ByteArray): KmmResult<Boolean> = KmmResult.success(true)
        override suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean> =
            KmmResult.success(true)

        override fun getPublicKeyEncoded(): KmmResult<ByteArray> = KmmResult.success(byteArrayOf())
        override fun getIssuingMessage(): String? = null
        override val deviceName = "Unit Test"
    }

    object ErrorThrowingDeviceAdapter : DeviceAdapter {
        override suspend fun loadAttestationCerts(): KmmResult<List<ByteArray>> =
            KmmResult.failure(IllegalArgumentException())

        override fun storeCertificate(certificate: ByteArray): KmmResult<Boolean> =
            KmmResult.failure(IllegalArgumentException())

        override suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean> =
            KmmResult.failure(IllegalArgumentException())

        override fun getPublicKeyEncoded(): KmmResult<ByteArray> = KmmResult.failure(IllegalArgumentException())
        override fun getIssuingMessage(): String? = null
        override val deviceName = "Unit Test"
    }

    private fun assertErrorFromNativeCode(result: DeviceBindingService.Result) {
        when (result) {
            is DeviceBindingService.Result.ErrorFromNativeCode -> assertTrue(true)
            is DeviceBindingService.Result.ErrorFromNetwork -> fail(result.error)
            is DeviceBindingService.Result.Success -> fail("Unexpected success")
        }
    }

    private fun assertErrorFromNetwork(result: DeviceBindingService.Result) {
        when (result) {
            is DeviceBindingService.Result.ErrorFromNativeCode -> fail(result.throwable?.toString() ?: "NULL ERROR")
            is DeviceBindingService.Result.ErrorFromNetwork -> assertTrue(true)
            is DeviceBindingService.Result.Success -> fail("Unexpected success")
        }
    }

    private fun assertSuccess(result: DeviceBindingService.Result) {
        when (result) {
            is DeviceBindingService.Result.ErrorFromNativeCode -> fail(result.throwable?.toString() ?: "NULL ERROR")
            is DeviceBindingService.Result.ErrorFromNetwork -> fail(result.error)
            is DeviceBindingService.Result.Success -> assertTrue(true)
        }
    }
}