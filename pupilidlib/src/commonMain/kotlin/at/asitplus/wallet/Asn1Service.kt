package at.asitplus.wallet

class Asn1Service(
    private val deviceAdapter: DeviceAdapter,
) {

    suspend fun signAndCreateCsr(publicKeyEncoded: ByteArray, subject: String): ByteArray {
        val csrTbs = buildCsrTbs(subject, publicKeyEncoded)
        val csrTbsSigned = deviceAdapter.sign(csrTbs)
        return buildCsr(csrTbs, csrTbsSigned)
    }

    private fun buildCsr(csrTbs: ByteArray, csrTbsSigned: ByteArray): ByteArray {
        val sha256Ec = Asn1Object.Sequence(Asn1Object.Oid("1.2.840.10045.4.3.2"))
        val signatureBitString = Asn1Object.BitString(byteArrayOf(0x00) + csrTbsSigned)
        return Asn1Object.Sequence(Asn1Object.Raw(csrTbs), sha256Ec, signatureBitString).encode()
    }

    private val mapSubjectPart = mapOf(
        "E" to Asn1Object.Oid("1.2.840.113549.1.9.1"),
        "CN" to Asn1Object.Oid("2.5.4.3"),
        "C" to Asn1Object.Oid("2.5.4.6"),
        "L" to Asn1Object.Oid("2.5.4.7"),
        "O" to Asn1Object.Oid("2.5.4.10"),
        "OU" to Asn1Object.Oid("2.5.4.11"),
        "DN" to Asn1Object.Oid("2.5.4.49"),
        "UID" to Asn1Object.Oid("2.5.4.45"),
        "UniqueIdentifier" to Asn1Object.Oid("2.5.4.45"),
    )

    private fun buildCsrTbs(subject: String, publicKeyEncoded: ByteArray): ByteArray {
        val version = Asn1Object.Integer(0)
        val subjectTag = Asn1Object.Sequence(
            mapSubjectPart.mapNotNull { extractSubjectPart(subject, it.key, it.value) }
        )
        val publicKeyInfo = wrapPublicKeyIfNecessary(publicKeyEncoded)
        val attributes = Asn1Object.Raw(0xa0, 0x00)
        return Asn1Object.Sequence(
            listOf(version) + subjectTag + publicKeyInfo + attributes
        ).encode()
    }

    private fun wrapPublicKeyIfNecessary(publicKey: ByteArray): Asn1Object {
        val publicKeyInfoType = Asn1Object.Sequence(
            Asn1Object.Oid("1.2.840.10045.2.1"),
            Asn1Object.Oid("1.2.840.10045.3.1.7"),
        )
        return if (publicKey.size > 2 && publicKey[0] == 0x00.toByte() && publicKey[1] == 0x04.toByte()) {
            // Encoded Public Key is "raw", i.e. starts with 0x0004 ... and needs to be wrapped with algorithms
            val publicKeyAsn1 = Asn1Object.BitString(publicKey)
            Asn1Object.Sequence(publicKeyInfoType, publicKeyAsn1)
        } else if (publicKey.size > 1 && publicKey[0] == 0x04.toByte()) {
            // Encoded Public Key is "raw", i.e. starts with 0x04 ... and needs to be wrapped with algorithms (e.g. from iOS)
            val publicKeyAsn1 = Asn1Object.BitString(byteArrayOf(0x00) + publicKey)
            Asn1Object.Sequence(publicKeyInfoType, publicKeyAsn1)
        } else {
            // Encoded Public Key already contains algorithm (e.g. from Java)
            Asn1Object.Raw(publicKey)
        }
    }

    private fun extractSubjectPart(subject: String, component: String, header: Asn1Object): Asn1Object.Set? {
        val findAll = Regex("$component=([^,]*)")
            .findAll(subject).toList()
        val values = findAll
            .map { it.groupValues }
            .map { it[1] }
            .map { Asn1Object.Utf8String(it) }
            .firstOrNull() ?: return null
        return Asn1Object.Set(Asn1Object.Sequence(header, values))
    }

}
