package at.asitplus.wallet.utils

import at.asitplus.KmmResult
import at.asitplus.crypto.datatypes.CryptoAlgorithm
import at.asitplus.crypto.datatypes.CryptoPublicKey
import at.asitplus.crypto.datatypes.asn1.Asn1String
import at.asitplus.crypto.datatypes.asn1.ObjectIdentifier
import at.asitplus.crypto.datatypes.pki.DistinguishedName
import at.asitplus.crypto.datatypes.pki.Pkcs10CertificationRequest
import at.asitplus.crypto.datatypes.pki.TbsCertificationRequest
import at.asitplus.wallet.pupilid.HashAlgorithm
import at.asitplus.wallet.pupilid.KeyAlgorithm

//TODO this is a remnant from the olden days and should be properly refactored
class Asn1Service(
    private val cryptoAdapter: CryptoAdapter,
) {

    suspend fun signAndCreateCsr(
        publicKeyEncoded: ByteArray,
        subject: String,
        key: KeyAlgorithm
    ): KmmResult<ByteArray> {
        val subjectName = mapSubjectPart.mapNotNull { extractSubjectPart(subject, it.key, it.value) }
        val publicKey = CryptoPublicKey.decodeFromDerSafe(publicKeyEncoded).getOrElse {
            kotlin.runCatching { CryptoPublicKey.fromIosEncoded(publicKeyEncoded) }.getOrElse {
                return KmmResult.failure(IllegalArgumentException("unsupported key encoding"))
            }
        }
        val tbsCsr = TbsCertificationRequest(
            subjectName = subjectName,
            publicKey = publicKey
        )
        val hashAlgorithm = HashAlgorithm.SHA256
        val signatureAlgorithm = if (key == KeyAlgorithm.EC) CryptoAlgorithm.ES256 else CryptoAlgorithm.RS256
        val tbsDerEncoded = tbsCsr.encodeToDerSafe()
            .getOrElse { return KmmResult.failure(it) }
        val signature = cryptoAdapter.sign(tbsDerEncoded, key, hashAlgorithm)
            .getOrElse { return KmmResult.failure(it) }

        return Pkcs10CertificationRequest(
            tbsCsr = tbsCsr,
            signatureAlgorithm = signatureAlgorithm,
            signature = signature
        ).encodeToDerSafe()
    }

    private val mapSubjectPart = mapOf(
        "E" to ObjectIdentifier("1.2.840.113549.1.9.1"),
        "CN" to ObjectIdentifier("2.5.4.3"),
        "C" to ObjectIdentifier("2.5.4.6"),
        "L" to ObjectIdentifier("2.5.4.7"),
        "O" to ObjectIdentifier("2.5.4.10"),
        "OU" to ObjectIdentifier("2.5.4.11"),
        "DN" to ObjectIdentifier("2.5.4.49"),
        "UID" to ObjectIdentifier("2.5.4.45"),
        "UniqueIdentifier" to ObjectIdentifier("2.5.4.45"),
    )

    private fun extractSubjectPart(subject: String, component: String, header: ObjectIdentifier): DistinguishedName? {
        val string = Regex("$component=([^,]*)")
            .findAll(subject)
            .map { it.groupValues }
            .filter { it.size > 1 }
            .map { it[1] }
            .map { Asn1String.UTF8(it) }
            .firstOrNull() ?: return null
        return DistinguishedName.Other(header, string)
    }


    interface CryptoAdapter {
        /**
         * Implementers: Sign [input] with the defined [key] and [hash], using the private key for the CSR
         */
        suspend fun sign(input: ByteArray, key: KeyAlgorithm, hash: HashAlgorithm): KmmResult<ByteArray>
    }

}
