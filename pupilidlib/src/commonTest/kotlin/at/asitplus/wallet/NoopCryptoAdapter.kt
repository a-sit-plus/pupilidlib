package at.asitplus.wallet

import at.asitplus.wallet.lib.KmmResult

object NoopCryptoAdapter : Asn1Service.CryptoAdapter {
    override suspend fun sign(input: ByteArray, key: KeyAlgorithm, hash: HashAlgorithm) =
        KmmResult.success(byteArrayOf())
}