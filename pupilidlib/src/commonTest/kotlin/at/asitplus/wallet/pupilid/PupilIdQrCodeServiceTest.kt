package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.DefaultVerifierCryptoService
import at.asitplus.wallet.lib.jws.*
import at.asitplus.testballoon.matrix.matrixSuite
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEachTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.util.sha256
import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlin.random.Random

val PupilIdQrCodeServiceTest by matrixSuite {

    lateinit var walletCryptoService: DefaultCryptoService
    lateinit var wallet: PupilIdQrCodeService
    lateinit var issuerCryptoService: DefaultCryptoService
    lateinit var verifier: PupilIdQrCodeService
    lateinit var pinnedAttestedKeyIssuers: List<JsonWebKey>


    testSuite("tests", testConfig = TestConfig.aroundEachTest { action ->
        walletCryptoService = DefaultCryptoService()
        wallet = PupilIdQrCodeService.newInstance(walletCryptoService)
        issuerCryptoService = DefaultCryptoService()
        verifier = PupilIdQrCodeService.newInstance(DefaultVerifierCryptoService())
        pinnedAttestedKeyIssuers = listOf(issuerCryptoService.toJsonWebKey())
        action()
    }) {
        "walkthrough should be successful" {
            val attestedKeyJws = buildAttestedKey(walletCryptoService, issuerCryptoService)
            val picture = Random.nextBytes(64)
            val content = randomCredentialContent(attestedKeyJws, sha256(picture))

            val listOfQrCodes = wallet.encodeInQrCode(content, picture).getOrThrow()

            val result = verifier.decodeQrCode(listOfQrCodes, "PI1:", true, pinnedAttestedKeyIssuers).getOrThrow()
            result.content shouldBe content
            result.picture shouldBe picture
        }

        "no pinned keys should throw" {
            val attestedKeyJws = buildAttestedKey(walletCryptoService, issuerCryptoService)
            val picture = Random.nextBytes(64)
            val content = randomCredentialContent(attestedKeyJws, sha256(picture))

            val listOfQrCodes = wallet.encodeInQrCode(content, picture).getOrThrow()

            val result = verifier.decodeQrCode(listOfQrCodes, "PI1:", true, listOf())
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<AttestedKeyNotVerifiedError>()
        }

        "wrong pinned key should throw" {
            val attestedKeyJws = buildAttestedKey(walletCryptoService, issuerCryptoService)
            val picture = Random.nextBytes(64)
            val content = randomCredentialContent(attestedKeyJws, sha256(picture))

            val listOfQrCodes = wallet.encodeInQrCode(content, picture).getOrThrow()

            val result = verifier.decodeQrCode(listOfQrCodes, "PI1:", true, listOf(walletCryptoService.toJsonWebKey()))
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<AttestedKeyNotVerifiedError>()
        }

        "no attested key should throw" {
            val picture = Random.nextBytes(64)
            val content = randomCredentialContent("", sha256(picture))

            val listOfQrCodes = wallet.encodeInQrCode(content, picture).getOrThrow()

            val result = verifier.decodeQrCode(listOfQrCodes, "PI1:", true, pinnedAttestedKeyIssuers)
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<QrCodeDecodeError>()
        }

        "signed with wrong key should throw" {
            val attestedKeyJws = buildAttestedKey(walletCryptoService, issuerCryptoService)
            val picture = Random.nextBytes(64)
            val content = randomCredentialContent(attestedKeyJws, sha256(picture))

            val listOfQrCodes = PupilIdQrCodeService.newInstance(DefaultCryptoService())
                .encodeInQrCode(content, picture).getOrThrow()

            val result = verifier.decodeQrCode(listOfQrCodes, "PI1:", true, pinnedAttestedKeyIssuers)
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<JwsNotVerifiedError>()
        }
    }
}

private suspend fun buildAttestedKey(
    walletCryptoService: DefaultCryptoService,
    issuerCryptoService: DefaultCryptoService
): String {
    val attestedKeyPayload =
        AttestedPublicKey(walletCryptoService.toJsonWebKey().keyId!!).serialize().encodeToByteArray()
    return DefaultJwsService(issuerCryptoService).createSignedJws(JwsHeader(JwsAlgorithm.ES256), attestedKeyPayload)!!
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

fun randomString() = Random.nextBytes(32).encodeToString(Base16)
