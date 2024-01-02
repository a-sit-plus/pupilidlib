package at.asitplus.wallet.pupilid.testmocks

import at.asitplus.KmmResult
import at.asitplus.wallet.pupilid.HashAlgorithm
import at.asitplus.wallet.pupilid.KeyAlgorithm
import at.asitplus.wallet.utils.Asn1Service

object NoopCryptoAdapter : Asn1Service.CryptoAdapter {

    override suspend fun sign(input: ByteArray, key: KeyAlgorithm, hash: HashAlgorithm) =
        KmmResult.success(byteArrayOf())

}