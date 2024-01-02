package at.asitplus.wallet.pupilid

import at.asitplus.crypto.datatypes.jws.JsonWebKey
import at.asitplus.crypto.datatypes.jws.JwsAlgorithm
import at.asitplus.crypto.datatypes.jws.JwsHeader
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.DefaultVerifierCryptoService
import at.asitplus.wallet.lib.jws.DefaultJwsService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.matthewnelson.component.base64.encodeBase64
import io.matthewnelson.component.encoding.base16.encodeBase16
import okio.ByteString.Companion.toByteString
import kotlin.random.Random

class PupilIdQrCodeServiceTest : StringSpec({

    lateinit var walletCryptoService: DefaultCryptoService
    lateinit var wallet: PupilIdQrCodeService
    lateinit var issuerCryptoService: DefaultCryptoService
    lateinit var verifier: PupilIdQrCodeService
    lateinit var pinnedAttestedKeyIssuers: List<JsonWebKey>

    beforeTest {
        walletCryptoService = DefaultCryptoService()
        wallet = PupilIdQrCodeService.newInstance(walletCryptoService)
        issuerCryptoService = DefaultCryptoService()
        verifier = PupilIdQrCodeService.newInstance(DefaultVerifierCryptoService())
        pinnedAttestedKeyIssuers = listOf(issuerCryptoService.jsonWebKey)
    }

    "walkthrough should be successful" {
        val attestedKeyJws = buildAttestedKey(walletCryptoService, issuerCryptoService)
        val picture = Random.nextBytes(64)
        val content = randomCredentialContent(attestedKeyJws, picture.sha256())

        val listOfQrCodes = wallet.encodeInQrCode(content, picture).getOrThrow()

        val result = verifier.decodeQrCode(listOfQrCodes, "PI1:", true, pinnedAttestedKeyIssuers).getOrThrow()
        result.content shouldBe content
        result.picture shouldBe picture
    }

    "no pinned keys should throw" {
        val attestedKeyJws = buildAttestedKey(walletCryptoService, issuerCryptoService)
        val picture = Random.nextBytes(64)
        val content = randomCredentialContent(attestedKeyJws, picture.sha256())

        val listOfQrCodes = wallet.encodeInQrCode(content, picture).getOrThrow()

        val result = verifier.decodeQrCode(listOfQrCodes, "PI1:", true, listOf())
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<AttestedKeyNotVerifiedError>()
    }

    "wrong pinned key should throw" {
        val attestedKeyJws = buildAttestedKey(walletCryptoService, issuerCryptoService)
        val picture = Random.nextBytes(64)
        val content = randomCredentialContent(attestedKeyJws, picture.sha256())

        val listOfQrCodes = wallet.encodeInQrCode(content, picture).getOrThrow()

        val result =
            verifier.decodeQrCode(listOfQrCodes, "PI1:", true, listOf(walletCryptoService.jsonWebKey))
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<AttestedKeyNotVerifiedError>()
    }

    "no attested key should throw" {
        val picture = Random.nextBytes(64)
        val content = randomCredentialContent("", picture.sha256())

        val listOfQrCodes = wallet.encodeInQrCode(content, picture).getOrThrow()

        val result = verifier.decodeQrCode(listOfQrCodes, "PI1:", true, pinnedAttestedKeyIssuers)
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<QrCodeDecodeError>()
    }

    "signed with wrong key should throw" {
        val attestedKeyJws = buildAttestedKey(walletCryptoService, issuerCryptoService)
        val picture = Random.nextBytes(64)
        val content = randomCredentialContent(attestedKeyJws, picture.sha256())

        val listOfQrCodes = PupilIdQrCodeService.newInstance(DefaultCryptoService())
            .encodeInQrCode(content, picture).getOrThrow()

        val result = verifier.decodeQrCode(listOfQrCodes, "PI1:", true, pinnedAttestedKeyIssuers)
        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<JwsNotVerifiedError>()
    }
})

private fun ByteArray.sha256(): ByteArray {
    return this.toByteString().sha256().toByteArray()
}

private suspend fun buildAttestedKey(
    walletCryptoService: DefaultCryptoService,
    issuerCryptoService: DefaultCryptoService
): String {
    val attestedKeyPayload =
        AttestedPublicKey(walletCryptoService.jsonWebKey.keyId!!).serialize().encodeToByteArray()
    return DefaultJwsService(issuerCryptoService).createSignedJws(JwsHeader(algorithm = JwsAlgorithm.ES256), attestedKeyPayload)!!.serialize()
}

private fun randomCredentialContent(attestedKeyJws: String, pictureHash: ByteArray) = CredentialContent(
    randomString(),
    randomString(),
    0,
    1,
    randomString(),
    randomString(),
    attestedKeyJws,
    pictureHash.encodeBase64()
)

fun randomString() = Random.nextBytes(32).encodeBase16()
