package at.asitplus.wallet.utils

import at.asitplus.KmmResult
import at.asitplus.wallet.pupilid.DeviceAdapter
import at.asitplus.wallet.pupilid.HashAlgorithm
import at.asitplus.wallet.pupilid.KeyAlgorithm
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import kotlin.random.Random


class Asn1ServiceTestJvm : StringSpec({


    fun compareBouncycastleWithService(
        ecKeyPair: KeyPair,
        deviceAdapter: InMemoryDeviceAdapter,
        keyPair: KeyPair,
        subject: String,
        keyAlg: KeyAlgorithm
    ) = runBlocking {
        val service = Asn1Service(object : Asn1Service.CryptoAdapter {
            override suspend fun sign(input: ByteArray, key: KeyAlgorithm, hash: HashAlgorithm): KmmResult<ByteArray> {
                return KmmResult.success(Signature.getInstance(getJcaSignatureName(key, hash)).also {
                    it.initSign(keyPair.private)
                    it.update(input)
                }.sign())
            }
        })
        val contentSigner =
            JcaContentSignerBuilder(getJcaSignatureName(keyAlg, HashAlgorithm.SHA256)).build(ecKeyPair.private)
        val contentVerifierProvider = JcaContentVerifierProviderBuilder().build(ecKeyPair.public)
        val bcEncoded =
            JcaPKCS10CertificationRequestBuilder(X500Name(subject), ecKeyPair.public).build(contentSigner).encoded
        val publicKeyEncoded = deviceAdapter.getPublicKeyEncoded().getOrThrow()
        val libEncoded = service.signAndCreateCsr(publicKeyEncoded, subject, keyAlg).getOrThrow()

        // Strip signatures before comparing, as ECDSA signatures contain random values, i.e. are never the same
        // also remove first 3 bytes, because they encode the length of the whole structure,
        // and those lengths may also not be the same since the ECDSA signature may be 251 bit or 256 bit
        if (keyAlg == KeyAlgorithm.EC)
            libEncoded.drop(3).take(133) shouldBe bcEncoded.drop(3).take(133)
        else
            libEncoded shouldBe bcEncoded

        PKCS10CertificationRequest(libEncoded).isSignatureValid(contentVerifierProvider) shouldBe true
    }

    "testCsrEncodingEcSha256" {
        val subject = "CN=randomFoo,O=Stage Q"
        val keyAlg = KeyAlgorithm.EC
        val ecKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()
        val deviceAdapter = InMemoryDeviceAdapter(ecKeyPair)

        compareBouncycastleWithService(ecKeyPair, deviceAdapter, ecKeyPair, subject, keyAlg)
    }

    "testCsrEncodingRsaSha256" {
        val subject = "CN=fooRandom,O=Stage T"
        val keyAlg = KeyAlgorithm.RSA
        val rsaKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair()
        val deviceAdapter = InMemoryDeviceAdapter(rsaKeyPair)

        compareBouncycastleWithService(rsaKeyPair, deviceAdapter, rsaKeyPair, subject, keyAlg)
    }

})

class InMemoryDeviceAdapter(private val keyPair: KeyPair) : DeviceAdapter {
    override suspend fun loadAttestationCerts(challenge: ByteArray, clientData: ByteArray): KmmResult<List<ByteArray>> =
        KmmResult.success(listOf())

    override fun storeCertificate(certificate: ByteArray, attestedPublicKey: String?): KmmResult<Boolean> =
        KmmResult.success(true)

    override suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean> =
        KmmResult.success(true)

    override fun getPublicKeyEncoded(): KmmResult<ByteArray> = KmmResult.success(keyPair.public.encoded)
    override val deviceName = "Unit Test"

}


fun getJcaSignatureName(key: KeyAlgorithm, hash: HashAlgorithm) = when (key) {
    KeyAlgorithm.EC -> when (hash) {
        HashAlgorithm.SHA1 -> "SHA1withECDSA"
        HashAlgorithm.SHA256 -> "SHA256withECDSA"
        HashAlgorithm.SHA512 -> "SHA512withECDSA"
    }

    KeyAlgorithm.RSA -> when (hash) {
        HashAlgorithm.SHA1 -> "SHA1withRSA"
        HashAlgorithm.SHA256 -> "SHA256withRSA"
        HashAlgorithm.SHA512 -> "SHA512withRSA"
    }
}