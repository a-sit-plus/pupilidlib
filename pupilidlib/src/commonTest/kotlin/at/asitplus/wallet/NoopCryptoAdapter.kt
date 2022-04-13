package at.asitplus.wallet

import at.asitplus.wallet.pupilid.Asn1Service
import at.asitplus.wallet.pupilid.HashAlgorithm
import at.asitplus.wallet.pupilid.KeyAlgorithm
import at.asitplus.wallet.pupilid.KmmResult

object NoopCryptoAdapter : Asn1Service.CryptoAdapter {

    override suspend fun sign(input: ByteArray, key: KeyAlgorithm, hash: HashAlgorithm) =
        KmmResult.success(byteArrayOf())

}