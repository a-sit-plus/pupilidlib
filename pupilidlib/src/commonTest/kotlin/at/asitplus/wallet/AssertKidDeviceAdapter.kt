package at.asitplus.wallet

import at.asitplus.wallet.pupilid.AttestedPublicKey
import at.asitplus.wallet.pupilid.DeviceAdapter
import at.asitplus.wallet.pupilid.KeyAlgorithm
import at.asitplus.wallet.pupilid.KmmResult
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class AssertKidDeviceAdapter(private val attestedPublicKeyKid: String) : DeviceAdapter {
    override suspend fun loadAttestationCerts(): KmmResult<List<ByteArray>> = KmmResult.success(listOf())

    override fun storeCertificate(certificate: ByteArray, attestedPublicKey: String?): KmmResult<Boolean> {
        attestedPublicKey.shouldNotBeNull()
        val apk = AttestedPublicKey.deserialize(attestedPublicKey)
        apk.kid shouldBe attestedPublicKeyKid
        return KmmResult.success(true)
    }

    override suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean> =
        KmmResult.success(true)

    override fun getPublicKeyEncoded(): KmmResult<ByteArray> = KmmResult.success(byteArrayOf())

    override val deviceName = "Unit Test"
}