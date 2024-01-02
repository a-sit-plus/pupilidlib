package at.asitplus.wallet.pupilid.testmocks

import at.asitplus.KmmResult
import at.asitplus.wallet.pupilid.DeviceAdapter
import at.asitplus.wallet.pupilid.KeyAlgorithm

object ErrorThrowingDeviceAdapter : DeviceAdapter {
    override suspend fun loadAttestationCerts(challenge: ByteArray, clientData: ByteArray): KmmResult<List<ByteArray>> =
        KmmResult.failure(IllegalArgumentException())

    override fun storeCertificate(certificate: ByteArray, attestedPublicKey: String?): KmmResult<Boolean> =
        KmmResult.failure(IllegalArgumentException())

    override suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean> =
        KmmResult.failure(IllegalArgumentException())

    override fun getPublicKeyEncoded(): KmmResult<ByteArray> = KmmResult.failure(IllegalArgumentException())
    override val deviceName = "Unit Test"
}
