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

    private val validInput = createValidInput()
    private val picture = Random.Default.nextBytes(Random.nextInt(900, 1800))
    private val pictureHash = picture.toByteString().sha256().toByteArray()

    private fun createValidInput() = CredentialContent(
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
    fun testEncodingWithSecondPrefix() = runBlocking {
        val input = validInput.copy(scaledPictureHash = pictureHash.encodeBase64())
        val secondPrefix = Random.Default.nextBytes(4).encodeBase64()

        val encoded = QrCodeContentService.encodeInQrCode(
            content = input,
            picture = picture,
            prefix = "PI1:$secondPrefix:"
        ) { KmmResult.success(it) }
            .getOrThrow()
        encoded.shouldNotBeEmpty()

        val decoded =
            QrCodeContentService.decodeQrCode(input = encoded, prefix = "PI1:$secondPrefix:") { KmmResult.success(it) }
                .getOrThrow()

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input)
        decoded.picture.shouldBe(picture)
    }

    @Test
    fun testEncodingWithSecondPrefixNotGiven() = runBlocking {
        val input = validInput.copy(scaledPictureHash = pictureHash.encodeBase64())
        val secondPrefix = Random.Default.nextBytes(4).encodeBase64()

        val encoded = QrCodeContentService.encodeInQrCode(
            content = input,
            picture = picture,
            prefix = "PI1:$secondPrefix:"
        ) { KmmResult.success(it) }
            .getOrThrow()
        encoded.shouldNotBeEmpty()

        val decoded = QrCodeContentService.decodeQrCode(
            input = encoded,
            prefix = "PI1",
            stripSecondPrefix = true
        ) { KmmResult.success(it) }.getOrThrow()

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input)
        decoded.picture.shouldBe(picture)
    }

    @Test
    fun testEncodingWithSecondPrefixFromDifferentSources() = runBlocking {
        val (input1, encoded1) = encodeWithSecondPrefix()
        val (input2, encoded2) = encodeWithSecondPrefix()

        val decoded = QrCodeContentService.decodeQrCode(
            input = (encoded1 + encoded2.drop(1)).shuffled(),
            prefix = "PI1",
            stripSecondPrefix = true
        ) { KmmResult.success(it) }.getOrThrow()

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input1)
        decoded.picture.shouldBe(picture)
    }

    private suspend fun encodeWithSecondPrefix(): Pair<CredentialContent, List<String>> {
        val input1 = createValidInput().copy(scaledPictureHash = pictureHash.encodeBase64())
        val secondPrefix1 = Random.nextBytes(4).encodeBase64()
        val encoded1 = QrCodeContentService.encodeInQrCode(
            content = input1,
            picture = picture,
            prefix = "PI1:$secondPrefix1:"
        ) { KmmResult.success(it) }.getOrThrow()
        encoded1.shouldNotBeEmpty()
        return Pair(input1, encoded1)
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
        Unit
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
        Unit
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
        Unit
    }

    @Test
    fun testWrongPrefix() {
        val decoded = QrCodeContentService.decodeQrCode(listOf("foo")) { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<InputIncorrectError>()
        Unit
    }

    @Test
    fun testEmptyInput() {
        val decoded = QrCodeContentService.decodeQrCode(listOf()) { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<InputInsufficientError>()
        Unit
    }

    @Test
    fun testNotEnoughInput() {
        val decoded = QrCodeContentService.decodeQrCode(listOf("PI1:1-3:foo"), "PI1:") { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<InputInsufficientError>()
        Unit
    }

    @Test
    fun testQrCodeError() {
        val decoded = QrCodeContentService.decodeQrCode(listOf("PI1:1-1:foo"), "PI1:") { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<QrCodeDecodingError>()
        Unit
    }

    private fun randomString(): String {
        return Random.nextBytes(32).encodeBase64()
    }

}