package at.asitplus.wallet

import at.asitplus.wallet.pupilid.CredentialContent
import at.asitplus.wallet.pupilid.QrCodeContentService
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.test.Test

class QrCodeContentServiceTest {

    @Test
    fun testEncoding() {
        val input = CredentialContent("PupilId", "id", 1, 2, "2022-12-01", "2022-12-31", "key")
        val picture = Random.Default.nextBytes(Random.nextInt(900, 1800))

        val encoded = QrCodeContentService.encodeInQrCode(
            content = input,
            createSignedJwsWithPayload = { s -> s },
            picture = picture
        )

        encoded.shouldNotBeNull()
        encoded.shouldNotBeEmpty()

        println(encoded)
        //println(encoded.size)

        val decoded = QrCodeContentService.decodeQrCode(
            input = encoded,
            verifyJwsExtractPayload = { s -> s }
        )

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input)
        decoded.picture.shouldBe(picture)
    }

}