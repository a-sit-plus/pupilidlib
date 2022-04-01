package at.asitplus.wallet

import at.asitplus.wallet.lib.KmmResult

object ErrorThrowingDeviceAdapter : DeviceAdapter {
    override suspend fun loadAttestationCerts(): KmmResult<List<ByteArray>> =
        KmmResult.failure(IllegalArgumentException())

    override fun storeCertificate(certificate: ByteArray): KmmResult<Boolean> =
        KmmResult.failure(IllegalArgumentException())

    override suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean> =
        KmmResult.failure(IllegalArgumentException())

    override fun getPublicKeyEncoded(): KmmResult<ByteArray> = KmmResult.failure(IllegalArgumentException())
    override val deviceName = "Unit Test"
}