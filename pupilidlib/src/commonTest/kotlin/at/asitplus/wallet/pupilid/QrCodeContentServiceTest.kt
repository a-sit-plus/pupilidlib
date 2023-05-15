package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.util.encodeBase64
import okio.ByteString.Companion.toByteString
import kotlin.random.Random

class QrCodeContentServiceTest : StringSpec({

    fun randomString(): String {
        return Random.nextBytes(32).encodeBase64()
    }

    fun createValidInput() = CredentialContent(
        randomString(),
        randomString(),
        1,
        2,
        randomString(),
        randomString(),
        randomString()
    )

    val validInput = createValidInput()
    val picture = Random.Default.nextBytes(Random.nextInt(900, 1800))
    val pictureHash = picture.toByteString().sha256().toByteArray()

    "testEncoding" {
        val input = validInput.copy(scaledPictureHash = pictureHash.encodeBase64())

        val encoded = QrCodeContentService.encodeInQrCode(content = input, picture = picture) { KmmResult.success(it) }
            .getOrThrow()
        encoded.shouldNotBeEmpty()

        val decoded = QrCodeContentService.decodeQrCode(input = encoded) { KmmResult.success(it) }.getOrThrow()

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input)
        decoded.picture.shouldBe(picture)

        val mapped = QrCodeContentService.analyzeQrCode(encoded).getOrThrow().first()
        mapped.secondPrefix.shouldBeNull()
        mapped.actualCount.shouldBe(encoded.size)
        mapped.expectedCount.shouldBe(encoded.size)
    }

    "testEncodingWithSecondPrefix" {
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

        val mapped = QrCodeContentService.analyzeQrCode(encoded).getOrThrow().first()
        mapped.secondPrefix.shouldBe(secondPrefix)
        mapped.actualCount.shouldBe(encoded.size)
        mapped.expectedCount.shouldBe(encoded.size)
    }

    "testEncodingWithSecondPrefixNotGiven" {
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

        val mapped = QrCodeContentService.analyzeQrCode(encoded).getOrThrow().first()
        mapped.secondPrefix.shouldBe(secondPrefix)
        mapped.actualCount.shouldBe(encoded.size)
        mapped.expectedCount.shouldBe(encoded.size)
    }

    suspend fun encodeWithSecondPrefix(): Triple<CredentialContent, String, List<String>> {
        val input = createValidInput().copy(scaledPictureHash = pictureHash.encodeBase64())
        val secondPrefix = Random.nextBytes(4).encodeBase64()
        val encoded = QrCodeContentService.encodeInQrCode(
            content = input,
            picture = picture,
            prefix = "PI1:$secondPrefix:"
        ) { KmmResult.success(it) }.getOrThrow()
        encoded.shouldNotBeEmpty()
        return Triple(input, secondPrefix, encoded)
    }

    "testEncodingWithSecondPrefixFromDifferentSources" {
        val (input1, secondPrefix1, encoded1) = encodeWithSecondPrefix()
        val (_, secondPrefix2, encoded2) = encodeWithSecondPrefix()

        val newInput = (encoded1 + encoded2.drop(1)).shuffled()
        val decoded = QrCodeContentService.decodeQrCode(
            input = newInput,
            prefix = "PI1",
            stripSecondPrefix = true
        ) { KmmResult.success(it) }.getOrThrow()

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input1)
        decoded.picture.shouldBe(picture)

        val mapped = QrCodeContentService.analyzeQrCode(newInput).getOrThrow()
        mapped.first { it.secondPrefix == secondPrefix1 }.actualCount.shouldBe(encoded1.size)
        mapped.first { it.secondPrefix == secondPrefix1 }.expectedCount.shouldBe(encoded1.size)
        mapped.first { it.secondPrefix == secondPrefix2 }.actualCount.shouldBe(encoded2.size - 1)
        mapped.first { it.secondPrefix == secondPrefix2 }.expectedCount.shouldBe(encoded2.size)
    }

    "testEmptyPicture" {
        val input = validInput.copy()

        val encoded =
            QrCodeContentService.encodeInQrCode(content = input, picture = byteArrayOf()) { KmmResult.success(it) }
                .getOrThrow()
        encoded.shouldNotBeEmpty()

        val decoded = QrCodeContentService.decodeQrCode(input = encoded) { KmmResult.success(it) }.getOrThrow()

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input)
    }

    "testEmptyPictureWithAdditionalScans" {
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

    "testPictureHashNotMatching" {
        val newPicture = Random.Default.nextBytes(Random.nextInt(900, 1800))
        val newPictureHash = newPicture.toByteString().sha256().toByteArray()
        val input = validInput.copy(scaledPictureHash = newPictureHash.encodeBase64().reversed())

        val encoded = QrCodeContentService.encodeInQrCode(content = input, picture = newPicture) { KmmResult.success(it) }
            .getOrThrow()

        encoded.shouldNotBeEmpty()

        val decoded = QrCodeContentService.decodeQrCode(input = encoded) { KmmResult.success(it) }

        decoded.shouldNotBeNull()
        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<PictureNotMatchingError>()
        Unit
    }

    "testNoPictureHash" {
        val newPicture = Random.Default.nextBytes(Random.nextInt(900, 1800))
        val input = validInput.copy()

        val encoded = QrCodeContentService.encodeInQrCode(content = input, picture = newPicture) { KmmResult.success(it) }
            .getOrThrow()

        encoded.shouldNotBeEmpty()

        val decoded = QrCodeContentService.decodeQrCode(input = encoded) { KmmResult.success(it) }

        decoded.shouldNotBeNull()
        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<PictureNotMatchingError>()
        Unit
    }

    "testWrongPrefix" {
        val decoded = QrCodeContentService.decodeQrCode(listOf("foo")) { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<InputIncorrectError>()
        Unit
    }

    "testEmptyInput" {
        val decoded = QrCodeContentService.decodeQrCode(listOf()) { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<InputInsufficientError>()
        Unit
    }

    "testNotEnoughInput" {
        val decoded = QrCodeContentService.decodeQrCode(listOf("PI1:1-3:foo"), "PI1:") { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        val throwable = decoded.exceptionOrNull()
        throwable.shouldBeInstanceOf<InputInsufficientError>()
        throwable.count shouldBe 1
        throwable.expectedCount shouldBe 3
    }

    "testNotEnoughMultipleSecondPrefixes" {
        val secondPrefix1 = Random.Default.nextBytes(4).encodeBase64()
        val secondPrefix2 = Random.Default.nextBytes(4).encodeBase64()
        val decoded = QrCodeContentService.decodeQrCode(listOf("PI1:$secondPrefix1:2-4:foo", "PI1:$secondPrefix2:1-3:bar"), "PI1:", true) { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        val throwable = decoded.exceptionOrNull()
        throwable.shouldBeInstanceOf<InputInsufficientError>()
        throwable.count shouldBe 1
        throwable.expectedCount shouldBe 4
    }

    "testQrCodeError" {
        val decoded = QrCodeContentService.decodeQrCode(listOf("PI1:1-1:foo"), "PI1:") { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.exceptionOrNull().shouldBeInstanceOf<QrCodeDecodingError>()
        Unit
    }

})