package at.asitplus.wallet

import kotlinx.coroutines.runBlocking
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.bouncycastle.util.encoders.Hex
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue


class Asn1ServiceTest {

    @Test
    fun oidEncoding() {
        assertContentEquals(
            ASN1ObjectIdentifier("1.2.840.10045.4.3.2").encoded,
            Asn1Object.Oid("1.2.840.10045.4.3.2").encoded
        )
        assertContentEquals(
            ASN1ObjectIdentifier("1.2.840.113549.1.1.11").encoded,
            Asn1Object.Oid("1.2.840.113549.1.1.11").encoded
        )
    }

    @Test
    fun integerEncoding() {
        assertContentEquals(
            ASN1Integer(-1).encoded,
            Asn1Object.Integer(-1).encoded
        )
        assertContentEquals(
            ASN1Integer(0).encoded,
            Asn1Object.Integer(0).encoded
        )
        assertContentEquals(
            ASN1Integer(1).encoded,
            Asn1Object.Integer(1).encoded
        )
        val negativeRand = Random.nextLong(Long.MIN_VALUE, -1L)
        assertContentEquals(
            ASN1Integer(negativeRand).encoded,
            Asn1Object.Integer(negativeRand).encoded
        )
        val positiveRand = Random.nextLong(1L, Long.MAX_VALUE)
        assertContentEquals(
            ASN1Integer(positiveRand).encoded,
            Asn1Object.Integer(positiveRand).encoded
        )
    }

    @Test
    fun testCsrEncodingEcSha256() = runBlocking {
        val subject = "CN=randomFoo,O=Stage Q"
        val keyAlg = KeyAlgorithm.EC
        val ecKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()
        val deviceAdapter = InMemoryDeviceAdapter(ecKeyPair)

        compareBouncycastleWithService(ecKeyPair, deviceAdapter, subject, keyAlg)
    }

    @Test
    fun testCsrEncodingRsaSha256() = runBlocking {
        val subject = "CN=fooRandom,O=Stage T"
        val keyAlg = KeyAlgorithm.RSA
        val rsaKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair()
        val deviceAdapter = InMemoryDeviceAdapter(rsaKeyPair)

        compareBouncycastleWithService(rsaKeyPair, deviceAdapter, subject, keyAlg)
    }

    private fun compareBouncycastleWithService(
        ecKeyPair: KeyPair,
        deviceAdapter: InMemoryDeviceAdapter,
        subject: String,
        keyAlg: KeyAlgorithm
    ) = runBlocking {
        val service = Asn1Service(deviceAdapter)
        val contentSigner =
            JcaContentSignerBuilder(getJcaSignatureName(keyAlg, HashAlgorithm.SHA256)).build(ecKeyPair.private)
        val contentVerifierProvider = JcaContentVerifierProviderBuilder().build(ecKeyPair.public)
        val bcEncoded =
            JcaPKCS10CertificationRequestBuilder(X500Name(subject), ecKeyPair.public).build(contentSigner).encoded
        val libEncoded = service.signAndCreateCsr(deviceAdapter.getPublicKeyEncoded(), subject, keyAlg)

        println(Hex.encode(bcEncoded).decodeToString())
        println(Hex.encode(libEncoded).decodeToString())

        // Strip signatures before comparing, as ECDSA signatures contain random values, i.e. are never the same
        // also remove first 3 bytes, because they encode the length of the whole structure,
        // and those lengths may also not be the same since the ECDSA signature may be 251 bit or 256 bit
        if (keyAlg == KeyAlgorithm.EC)
            assertContentEquals(bcEncoded.drop(3).take(133), libEncoded.drop(3).take(133))
        else
            assertContentEquals(bcEncoded, libEncoded)
        assertTrue(PKCS10CertificationRequest(libEncoded).isSignatureValid(contentVerifierProvider))
    }

}

class InMemoryDeviceAdapter(private val keyPair: KeyPair) : DeviceAdapter {
    override fun loadAttestationCerts(): List<ByteArray> {
        return listOf()
    }

    override fun storeCertificate(certificate: ByteArray) {
        //
    }

    override fun createKey(key: KeyAlgorithm, challenge: ByteArray) {
        // key has been created
    }

    override fun getPublicKeyEncoded(): ByteArray = keyPair.public.encoded

    override suspend fun sign(input: ByteArray, key: KeyAlgorithm, hash: HashAlgorithm): ByteArray =
        createSignature(keyPair.private, getJcaSignatureName(key, hash), input)

    override val deviceName = "Unit Test"

    private fun createSignature(privateKey: PrivateKey, signatureAlg: String, input: ByteArray) =
        Signature.getInstance(signatureAlg).also {
            it.initSign(privateKey)
            it.update(input)
        }.sign()
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
