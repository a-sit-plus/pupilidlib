package at.asitplus.wallet

import at.asitplus.wallet.pupilid.CredentialContent
import at.asitplus.wallet.pupilid.QrCodeContentService
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.security.Key
import java.security.KeyPairGenerator
import java.security.cert.CertificateFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import kotlin.random.Random
import kotlin.test.Test

class QrCodeContentServiceTestJvm {

    @Test
    fun testWalletAppBasicVerification() {
        val input = CredentialContent("PupilId", "id", 1, 2, "2022-12-01", "2022-12-31", "key")
        val picture = Random.Default.nextBytes(Random.nextInt(900, 1800))
        val keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()

        val encoded = QrCodeContentService.encodeInQrCode(
            content = input,
            createSignedJwsWithPayload = { s ->
                val jwk = ECKey.Builder(Curve.P_256, keyPair.public as ECPublicKey).build()
                JWSObject(
                    JWSHeader.Builder(JWSAlgorithm.ES256).jwk(jwk).build(),
                    Payload(s)
                ).apply {
                    sign(ECDSASigner(keyPair.private as ECPrivateKey))
                }.serialize()
                    .also { println(it) }
            },
            picture = picture
        )

        encoded.shouldNotBeNull()
        encoded.shouldNotBeEmpty()

        println(encoded)

        val decoded = QrCodeContentService.decodeQrCode(
            input = encoded,
            verifyJwsExtractPayload = { s ->
                JWSObject.parse(s).run {
                    if (!verify(DefaultJWSVerifierFactory().createJWSVerifier(header, loadKey(header))))
                        return@run null
                    payload.toString()
                }
            }
        )

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input)
        decoded.picture.shouldBe(picture)
    }

    private fun loadKey(header: JWSHeader): Key? {
        return header.jwk?.let {
            it.toECKey()?.toPublicKey()
        } ?: header.x509CertChain?.let {
            it.firstOrNull()?.let {
                CertificateFactory.getInstance("X.509").generateCertificate(it.decode().inputStream()).publicKey
            }
        }
    }

}