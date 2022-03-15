package at.asitplus.wallet

import kotlinx.coroutines.runBlocking
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.bouncycastle.util.encoders.Hex
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue


class Asn1ServiceTest {

    @Test
    fun testCsrEncoding() = runBlocking {
        val subject = "CN=foo,O=Stage Q"
        val signatureAlg = "SHA256withECDSA"
        val keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()
        val deviceAdapter = object : DeviceAdapter {
            override fun loadAttestationCerts(): List<ByteArray> {
                return listOf()
            }

            override fun storeCertificate(certificate: ByteArray) {
                //
            }

            override fun createKeyGetPublicKeyEncoded() = keyPair.public.encoded

            override suspend fun sign(input: ByteArray) = createSignature(keyPair, "SHA256withECDSA", input)

            override val deviceName = "Unit Test"

        }
        val service = Asn1Service(deviceAdapter)

        val contentSigner = JcaContentSignerBuilder(signatureAlg).build(keyPair.private)
        val contentVerifierProvider = JcaContentVerifierProviderBuilder().build(keyPair.public)
        val bcEncoded =
            JcaPKCS10CertificationRequestBuilder(X500Name(subject), keyPair.public).build(contentSigner).encoded
        val libEncoded = service.signAndCreateCsr(deviceAdapter.createKeyGetPublicKeyEncoded(), subject)

        println(Hex.encode(bcEncoded).decodeToString())
        println(Hex.encode(libEncoded).decodeToString())
        // Strip signatures before comparing, as ECDSA signatures contain random values, i.e. are never the same
        assertContentEquals(bcEncoded.dropLast(74), libEncoded.dropLast(74))
        assertTrue(PKCS10CertificationRequest(libEncoded).isSignatureValid(contentVerifierProvider))
    }

    private fun createSignature(keyPair: KeyPair, signatureAlg: String, input: ByteArray) =
        Signature.getInstance(signatureAlg).also {
            it.initSign(keyPair.private)
            it.update(input)
        }.sign()
}
