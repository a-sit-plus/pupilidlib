package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import kotlin.random.Random

class QrCodeContentServiceTestJvm : StringSpec({

    "testWalletAppBasicVerification" {
        val picture = Random.Default.nextBytes(Random.nextInt(900, 1800))
        val input = CredentialContent("PupilId", "id", 1, 2, "2022-12-01", "2022-12-31", "key", picture.sha256())
        val keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()

        val encoded = runBlocking {
            QrCodeContentService.encodeInQrCode(
                content = input,
                picture = picture
            ) {
                KmmResult.success(JWSObject(
                    JWSHeader.Builder(JWSAlgorithm.ES256).jwk(keyPair.toPublicJwk()).build(),
                    Payload(it)
                ).apply {
                    sign(keyPair.toEcdsaSigner())
                }.serialize()
                    .also { println(it) }
                )
            }.getOrThrow()
        }

        encoded.shouldNotBeEmpty()

        println(encoded)

        val decoded = QrCodeContentService.decodeQrCode(
            input = encoded
        ) {
            JWSObject.parse(it).run {
                if (!verify(this.header.createVerifier()))
                    return@run KmmResult.failure(NullPointerException())
                KmmResult.success(payload.toString())
            }
        }.getOrThrow()

        decoded.shouldNotBeNull()
        decoded.content.shouldBe(input)
        decoded.picture.shouldBe(picture)
    }

    "testValidationAppExtendedValidation" {
        Security.addProvider(BouncyCastleProvider())
        val jwsWallet = Wallet.step1()
        val jwsValidationApp = Validator.step1(jwsWallet)
        val jweWalletApp = Wallet.step2(jwsValidationApp)
        Validator.step2(jweWalletApp)
    }

})

object Wallet {
    val walletBindingKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()
    val walletEphemeralKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()

    fun step1(): String {
        val picture = Random.Default.nextBytes(32)
        val input = CredentialContent("PupilId", "id", 1, 2, "2022-12-01", "2022-12-31", "key", picture.sha256())
        val keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()

        val serializedContent = Json.encodeToString(input)
        return JWSObject(
            JWSHeader.Builder(JWSAlgorithm.ES256)
                .jwk(keyPair.toPublicJwk())
                .customParam("epk", walletEphemeralKeyPair.toPublicJwk().toJSONObject())
                .build(),
            Payload(serializedContent)
        ).apply {
            sign(keyPair.toEcdsaSigner())
        }.serialize()
            .also { println(it) }
    }

    fun step2(input: String): String {
        val jwsObject = JWSObject.parse(input)
        if (!jwsObject.verify(jwsObject.header.createVerifier())) throw IllegalArgumentException()

        return JWEObject(
            JWEHeader.Builder(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256GCM)
                .jwk(walletBindingKeyPair.toPublicJwk())
                .build(),
            Payload(
                mapOf(
                    "vp" to "verifiable-presentation-with-challenge-signed-by-wallet-binding-key-pair",
                )
            )
        ).apply {
            // TODO Can we use the EPK from above?
            encrypt(ECDHEncrypter(jwsObject.header.epk.toECKey())) // adds "epk" to header
        }.serialize()
            .also { println(it) }
    }

}

object Validator {
    val validationBindingKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()
    val validationEphemeralKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()

    fun step1(input: String): String {
        val jwsObject = JWSObject.parse(input)
        if (!jwsObject.verify(jwsObject.header.createVerifier())) throw IllegalArgumentException()

        return JWSObject(
            JWSHeader.Builder(JWSAlgorithm.ES256)
                .jwk(validationBindingKeyPair.toPublicJwk())
                .customParam("epk", validationEphemeralKeyPair.toPublicJwk().toJSONObject())
                .build(),
            Payload(
                mapOf(
                    "key" to "attestation-structure-for-jwk-from-header-signed-by-server",
                    "challenge" to Random.nextBytes(32).encodeBase64(),
                    "response" to jwsObject.header.epk.toJSONString().encodeBase64()
                )
            )
        ).apply {
            sign(validationBindingKeyPair.toEcdsaSigner())
        }.serialize()
            .also { println(it) }
    }

    fun step2(input: String) {
        val parsedInValidation = JWEObject.parse(input)
        parsedInValidation.decrypt(validationEphemeralKeyPair.toEcdhDecrypter())
        println(parsedInValidation.payload.toJSONObject()["vp"])
    }

}

private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).encodeBase64()

private fun JWSHeader.loadKey(): Key? {
    return jwk?.let {
        it.toECKey()?.toPublicKey()
    } ?: x509CertChain?.let {
        it.firstOrNull()?.let {
            CertificateFactory.getInstance("X.509").generateCertificate(it.decode().inputStream()).publicKey
        }
    }
}

private fun JWSHeader.createVerifier() = DefaultJWSVerifierFactory().createJWSVerifier(this, this.loadKey())

private val JWSHeader.epk get() = JWK.parse(this.getCustomParam("epk").toString())

private fun KeyPair.toEcdsaSigner() = ECDSASigner(private as ECPrivateKey)

private fun KeyPair.toEcdhEncrypter() = ECDHEncrypter(this.toPublicJwk())

private fun KeyPair.toEcdhDecrypter() = ECDHDecrypter(this.private as ECPrivateKey)

private fun KeyPair.toPublicJwk() = ECKey.Builder(Curve.P_256, public as ECPublicKey).build()
