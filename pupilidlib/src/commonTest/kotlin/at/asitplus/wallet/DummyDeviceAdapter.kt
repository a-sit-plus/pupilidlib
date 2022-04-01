package at.asitplus.wallet

import at.asitplus.wallet.lib.KmmResult

object DummyDeviceAdapter : DeviceAdapter {
    override suspend fun loadAttestationCerts(): KmmResult<List<ByteArray>> = KmmResult.success(listOf())
    override fun storeCertificate(certificate: ByteArray): KmmResult<Boolean> = KmmResult.success(true)
    override suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean> =
        KmmResult.success(true)

    override fun getPublicKeyEncoded(): KmmResult<ByteArray> = KmmResult.success(byteArrayOf())
    override val deviceName = "Unit Test"
}