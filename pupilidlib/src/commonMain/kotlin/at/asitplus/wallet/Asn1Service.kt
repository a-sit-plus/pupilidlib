package at.asitplus.wallet

class Asn1Service(
    private val cryptoAdapter: CryptoAdapter,
) {

    suspend fun signAndCreateCsr(publicKeyEncoded: ByteArray, subject: String, key: KeyAlgorithm): Result<ByteArray> {
        val csrTbs = buildCsrTbs(subject, publicKeyEncoded, key)
        val hash = HashAlgorithm.SHA256
        val csrTbsSigned = cryptoAdapter.sign(csrTbs, key, hash).getOrElse {
            return Result.failure(it)
        }
        return Result.success(buildCsr(csrTbs, csrTbsSigned, key, hash))
    }

    private fun buildCsr(
        csrTbs: ByteArray,
        csrTbsSigned: ByteArray,
        key: KeyAlgorithm,
        hash: HashAlgorithm
    ): ByteArray {
        val keyIdentifier = buildAlgIdentifier(key, hash)
        val signatureBitString = Asn1Object.BitString(byteArrayOf(0x00) + csrTbsSigned)
        return Asn1Object.Sequence(Asn1Object.Raw(csrTbs), keyIdentifier, signatureBitString).encoded
    }

    private fun buildAlgIdentifier(key: KeyAlgorithm, hash: HashAlgorithm) = when (key) {
        KeyAlgorithm.EC -> when (hash) {
            HashAlgorithm.SHA1 -> Asn1Object.Sequence(Asn1Object.Oid("1.2.840.10045.4.1"))
            HashAlgorithm.SHA256 -> Asn1Object.Sequence(Asn1Object.Oid("1.2.840.10045.4.3.2"))
            HashAlgorithm.SHA512 -> Asn1Object.Sequence(Asn1Object.Oid("1.2.840.10045.4.3.4"))
        }
        KeyAlgorithm.RSA -> when (hash) {
            HashAlgorithm.SHA1 -> Asn1Object.Sequence(Asn1Object.Oid("1.2.840.113549.1.1.5"), Asn1Object.Null())
            HashAlgorithm.SHA256 -> Asn1Object.Sequence(Asn1Object.Oid("1.2.840.113549.1.1.11"), Asn1Object.Null())
            HashAlgorithm.SHA512 -> Asn1Object.Sequence(Asn1Object.Oid("1.2.840.113549.1.1.13"), Asn1Object.Null())
        }
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

    private fun buildCsrTbs(subject: String, publicKeyEncoded: ByteArray, key: KeyAlgorithm): ByteArray {
        val version = Asn1Object.Integer(0)
        val subjectTag = Asn1Object.Sequence(
            mapSubjectPart.mapNotNull { extractSubjectPart(subject, it.key, it.value) }
        )
        val publicKeyInfo = wrapPublicKey(publicKeyEncoded, key)
        val attributes = Asn1Object.Raw(0xa0, 0x00)
        return Asn1Object.Sequence(
            listOf(version) + subjectTag + publicKeyInfo + attributes
        ).encoded
    }

    private fun extractSubjectPart(subject: String, component: String, header: Asn1Object): Asn1Object.Set? {
        val values = Regex("$component=([^,]*)")
            .findAll(subject)
            .map { it.groupValues }
            .filter { it.size > 1 }
            .map { it[1] }
            .map { Asn1Object.Utf8String(it) }
            .firstOrNull() ?: return null
        return Asn1Object.Set(Asn1Object.Sequence(header, values))
    }

    fun wrapPublicKey(publicKey: ByteArray, key: KeyAlgorithm) = when (key) {
        KeyAlgorithm.EC -> wrapEcPublicKey(publicKey)
        else -> wrapRsaPublicKey(publicKey)
    }

    private fun wrapEcPublicKey(publicKey: ByteArray): Asn1Object {
        val publicKeyInfoType = Asn1Object.Sequence(
            Asn1Object.Oid("1.2.840.10045.2.1"),
            Asn1Object.Oid("1.2.840.10045.3.1.7"),
        )
        return if (publicKey.size > 2 && publicKey[0] == 0x00.toByte() && publicKey[1] == 0x04.toByte()) {
            // Encoded Public Key is "raw", i.e. starts with 0x00_04 ... and needs to be wrapped with algorithms
            val publicKeyAsn1 = Asn1Object.BitString(publicKey)
            Asn1Object.Sequence(publicKeyInfoType, publicKeyAsn1)
        } else if (publicKey.size > 1 && publicKey[0] == 0x04.toByte()) {
            // Encoded Public Key is "raw", i.e. starts with 0x04 ... and needs to be wrapped with algorithms (e.g. from iOS)
            val publicKeyAsn1 = Asn1Object.BitString(byteArrayOf(0x00) + publicKey)
            Asn1Object.Sequence(publicKeyInfoType, publicKeyAsn1)
        } else {
            // Encoded Public Key already contains algorithm information (e.g. from Java)
            Asn1Object.Raw(publicKey)
        }
    }

    private fun wrapRsaPublicKey(publicKey: ByteArray): Asn1Object {
        val publicKeyInfoType = Asn1Object.Sequence(
            Asn1Object.Oid("1.2.840.113549.1.1.1"),
            Asn1Object.Null(),
        )
        return if (publicKey.size > 4 && publicKey[0] == 0x30.toByte() && publicKey[4] == 0x30.toByte()) {
            // Encoded Public Key already contains algorithm information (e.g. from Java)
            Asn1Object.Raw(publicKey)
        } else if (publicKey.size > 1 && publicKey[0] == 0x00.toByte()) {
            val publicKeyAsn1 = Asn1Object.BitString(publicKey)
            Asn1Object.Sequence(publicKeyInfoType, publicKeyAsn1)
        } else {
            val publicKeyAsn1 = Asn1Object.BitString(byteArrayOf(0x00) + publicKey)
            Asn1Object.Sequence(publicKeyInfoType, publicKeyAsn1)
        }
    }

    interface CryptoAdapter {
        suspend fun sign(input: ByteArray, key: KeyAlgorithm, hash: HashAlgorithm): Result<ByteArray>
    }

}
