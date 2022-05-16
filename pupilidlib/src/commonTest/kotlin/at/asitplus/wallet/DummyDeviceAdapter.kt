package at.asitplus.wallet

import at.asitplus.wallet.pupilid.DeviceAdapter
import at.asitplus.wallet.pupilid.KeyAlgorithm
import at.asitplus.wallet.pupilid.KmmResult

object DummyDeviceAdapter : DeviceAdapter {
    override suspend fun loadAttestationCerts(): KmmResult<List<ByteArray>> = KmmResult.success(listOf())

    override fun storeCertificate(certificate: ByteArray, attestedPublicKey: String?): KmmResult<Boolean> =
        KmmResult.success(true)

    override suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean> =
        KmmResult.success(true)

    override fun getPublicKeyEncoded(): KmmResult<ByteArray> = KmmResult.success(byteArrayOf())

    override val deviceName = "Unit Test"
}