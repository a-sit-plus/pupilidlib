package at.asitplus.wallet

import at.asitplus.KmmResult
import at.asitplus.wallet.pupilid.CredentialContent
import at.asitplus.wallet.pupilid.InputIncorrectError
import at.asitplus.wallet.pupilid.InputInsufficientError
import at.asitplus.wallet.pupilid.PictureNotMatchingError
import at.asitplus.wallet.pupilid.QrCodeContentService
import at.asitplus.wallet.pupilid.QrCodeDecodingError
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.toByteString
import kotlin.random.Random
import kotlin.test.Test

class QrCodeContentServiceTest {

    private val validInput = CredentialContent(
        randomString(),
        randomString(),
        1,
        2,
        randomString(),
        randomString(),
        randomString()
    )

    @Test
    fun testEncoding() = runBlocking {
        val picture = Random.Default.nextBytes(Random.nextInt(900, 1800))
        val pictureHash = picture.toByteString().sha256().toByteArray()
        val input = validInput.copy(scaledPictureHash = pictureHash.encodeBase64())

        val encoded = QrCodeContentService.encodeInQrCode(content = input, picture = picture) { KmmResult.success(it) }
            .getOrThrow()
        encoded.shouldNotBeEmpty()

        val decoded = QrCodeContentService.decodeQrCode(input = encoded) { KmmResult.success(it) }.getOrThrow()

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input)
        decoded.picture.shouldBe(picture)
    }

    @Test
    fun testEmptyPicture() = runBlocking {
        val input = validInput.copy()

        val encoded =
            QrCodeContentService.encodeInQrCode(content = input, picture = byteArrayOf()) { KmmResult.success(it) }
                .getOrThrow()
        encoded.shouldNotBeEmpty()

        val decoded = QrCodeContentService.decodeQrCode(input = encoded) { KmmResult.success(it) }.getOrThrow()

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input)
    }

    @Test
    fun testEmptyPictureWithAdditionalScans() = runBlocking {
        val input = validInput.copy()

        val encoded =
            QrCodeContentService.encodeInQrCode(content = input, picture = byteArrayOf()) { KmmResult.success(it) }
                .getOrThrow()
        encoded.shouldNotBeEmpty()

        val decoded = QrCodeContentService.decodeQrCode(input = encoded + "foo") { KmmResult.success(it) }

        decoded.shouldNotBeNull()
        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<InputIncorrectError>()
    }

    @Test
    fun testPictureHashNotMatching() = runBlocking {
        val picture = Random.Default.nextBytes(Random.nextInt(900, 1800))
        val pictureHash = picture.toByteString().sha256().toByteArray()
        val input = validInput.copy(scaledPictureHash = pictureHash.encodeBase64().reversed())

        val encoded = QrCodeContentService.encodeInQrCode(content = input, picture = picture) { KmmResult.success(it) }
            .getOrThrow()

        encoded.shouldNotBeEmpty()

        val decoded = QrCodeContentService.decodeQrCode(input = encoded) { KmmResult.success(it) }

        decoded.shouldNotBeNull()
        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<PictureNotMatchingError>()
    }

    @Test
    fun testNoPictureHash() = runBlocking {
        val picture = Random.Default.nextBytes(Random.nextInt(900, 1800))
        val input = validInput.copy()

        val encoded = QrCodeContentService.encodeInQrCode(content = input, picture = picture) { KmmResult.success(it) }
            .getOrThrow()

        encoded.shouldNotBeEmpty()

        val decoded = QrCodeContentService.decodeQrCode(input = encoded) { KmmResult.success(it) }

        decoded.shouldNotBeNull()
        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<PictureNotMatchingError>()
    }

    @Test
    fun testWrongPrefix() {
        val decoded = QrCodeContentService.decodeQrCode(listOf("foo")) { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<InputIncorrectError>()
    }

    @Test
    fun testEmptyInput() {
        val decoded = QrCodeContentService.decodeQrCode(listOf()) { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<InputInsufficientError>()
    }

    @Test
    fun testNotEnoughInput() {
        val decoded = QrCodeContentService.decodeQrCode(listOf("PI1:1-3:foo"), "PI1:") { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<InputInsufficientError>()
    }

    @Test
    fun testQrCodeError() {
        val decoded = QrCodeContentService.decodeQrCode(listOf("PI1:1-1:foo"), "PI1:") { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<QrCodeDecodingError>()
    }

    private fun randomString(): String {
        return Random.nextBytes(32).encodeBase64()
    }

}