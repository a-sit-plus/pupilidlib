package at.asitplus.wallet

class Asn1Service(
    private val deviceAdapter: DeviceAdapter,
) {

    suspend fun signAndCreateCsr(publicKeyEncoded: ByteArray): ByteArray {
        val csrTbs = buildCsrTbs("CN=foo", publicKeyEncoded)
        val csrTbsSigned = deviceAdapter.sign(csrTbs)
        return buildCsr(csrTbsSigned)
    }

    private fun buildCsr(csrTbsSigned: ByteArray): ByteArray {
        val sha256Ec = Asn1Object.Sequence(Asn1Object.Oid("1.2.840.10045.4.3.2"))
        val signatureBitString = Asn1Object.BitString(byteArrayOf(0x00) + csrTbsSigned)
        return Asn1Object.Sequence(Asn1Object.Raw(csrTbsSigned), sha256Ec, signatureBitString).encode()
    }

    private val mapSubjectPart = mapOf(
        "C" to Asn1Object.Oid("2.5.4.6"),
        "CN" to Asn1Object.Oid("2.5.4.3"),
        "DN" to Asn1Object.Oid("2.5.4.49"),
        "E" to Asn1Object.Oid("1.2.840.113549.1.9.1"),
        "L" to Asn1Object.Oid("2.5.4.7"),
        "UID" to Asn1Object.Oid("2.5.4.45"),
        "UniqueIdentifier" to Asn1Object.Oid("2.5.4.45"),
        "O" to Asn1Object.Oid("2.5.4.10"),
        "OU" to Asn1Object.Oid("2.5.4.11"),
    )

    private fun buildCsrTbs(subject: String, publicKeyEncoded: ByteArray): ByteArray {
        val version = Asn1Object.Integer(0)
        val subjectTag: List<Asn1Object> =
            mapSubjectPart.mapNotNull { extractSubjectPart(subject, it.key, it.value) }
        val publicKeyInfoType = Asn1Object.Sequence(
            Asn1Object.Oid("1.2.840.10045.2.1"),
            Asn1Object.Oid("1.2.840.10045.3.1.7"),
        )
        val publicKeyAsn1 = Asn1Object.BitString(byteArrayOf(0x00) + publicKeyEncoded)
        val publicKeyInfo = Asn1Object.Sequence(publicKeyInfoType, publicKeyAsn1)
        val attributes = Asn1Object.Raw(0xa0, 0x00)
        return Asn1Object.Sequence(
            listOf(version) + subjectTag + publicKeyInfo + attributes
        ).encode()
    }

    private fun extractSubjectPart(subject: String, component: String, header: Asn1Object): Asn1Object.Set? {
        val values = Regex.fromLiteral("$component=(.*)")
            .findAll(subject)
            .mapNotNull { Asn1Object.Utf8String(it.value) }
            .firstOrNull() ?: return null
        return Asn1Object.Set(Asn1Object.Sequence(header, values))
    }

}
