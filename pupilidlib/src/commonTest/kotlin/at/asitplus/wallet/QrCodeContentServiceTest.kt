package at.asitplus.wallet

import at.asitplus.KmmResult
import at.asitplus.wallet.pupilid.CredentialContent
import at.asitplus.wallet.pupilid.InputInsufficientError
import at.asitplus.wallet.pupilid.QrCodeContentService
import at.asitplus.wallet.pupilid.QrCodeDecodingError
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test

class QrCodeContentServiceTest {

    @Test
    fun testEncoding() = runBlocking {
        val input = CredentialContent("PupilId", "id", 1, 2, "2022-12-01", "2022-12-31", "key")
        val picture = Random.Default.nextBytes(Random.nextInt(900, 1800))

        val encoded = QrCodeContentService.encodeInQrCode(
            content = input,
            picture = picture
        ) { KmmResult.success(it) }.getOrThrow()

        encoded.shouldNotBeEmpty()

        println(encoded)
        //println(encoded.size)

        val decoded = QrCodeContentService.decodeQrCode(
            input = encoded
        ) { KmmResult.success(it) }.getOrThrow()

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input)
        decoded.picture.shouldBe(picture)
    }

    @Test
    fun testWrongPrefix() {
        val decoded = QrCodeContentService.decodeQrCode(listOf("foo")) { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.error shouldBe InputInsufficientError()
    }

    @Test
    fun testEmptyInput() {
        val decoded = QrCodeContentService.decodeQrCode(listOf()) { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.error shouldBe InputInsufficientError()
    }

    @Test
    fun testNotEnoughInput() {
        val decoded = QrCodeContentService.decodeQrCode(listOf("PI1:1-3:foo"), "PI1:") { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.error shouldBe InputInsufficientError()
    }

    @Test
    fun testQrCodeError() {
        val decoded = QrCodeContentService.decodeQrCode(listOf("PI1:1-1:foo"), "PI1:") { KmmResult.success(it) }

        decoded.isFailure shouldBe true
        decoded.error shouldBe QrCodeDecodingError(Throwable())
    }

}