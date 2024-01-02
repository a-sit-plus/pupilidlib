package at.asitplus.wallet.pupilid.testmocks

import at.asitplus.KmmResult
import at.asitplus.wallet.pupilid.AttestedPublicKey
import at.asitplus.wallet.pupilid.DeviceAdapter
import at.asitplus.wallet.pupilid.KeyAlgorithm
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class AssertKidDeviceAdapter(private val attestedPublicKeyKid: String) : DeviceAdapter {
    override suspend fun loadAttestationCerts(challenge: ByteArray, clientData: ByteArray): KmmResult<List<ByteArray>> =
        KmmResult.success(listOf())

    override fun storeCertificate(certificate: ByteArray, attestedPublicKey: String?): KmmResult<Boolean> {
        attestedPublicKey.shouldNotBeNull()
        val apk = AttestedPublicKey.deserialize(attestedPublicKey)
        apk.shouldNotBeNull()
        apk.kid shouldBe attestedPublicKeyKid
        return KmmResult.success(true)
    }

    override suspend fun createKey(key: KeyAlgorithm, challenge: ByteArray): KmmResult<Boolean> =
        KmmResult.success(true)

    override fun getPublicKeyEncoded(): KmmResult<ByteArray> = KmmResult.success(
        byteArrayOf(
            0x04.toUByte().toByte(),
            0x97.toUByte().toByte(),
            0x47.toUByte().toByte(),
            0xe9.toUByte().toByte(),
            0x81.toUByte().toByte(),
            0xaa.toUByte().toByte(),
            0xb8.toUByte().toByte(),
            0xea.toUByte().toByte(),
            0x71.toUByte().toByte(),
            0xa2.toUByte().toByte(),
            0x55.toUByte().toByte(),
            0xcd.toUByte().toByte(),
            0x9a.toUByte().toByte(),
            0x15.toUByte().toByte(),
            0x62.toUByte().toByte(),
            0x85.toUByte().toByte(),
            0x8c.toUByte().toByte(),
            0x40.toUByte().toByte(),
            0x60.toUByte().toByte(),
            0x06.toUByte().toByte(),
            0xf1.toUByte().toByte(),
            0xe4.toUByte().toByte(),
            0x18.toUByte().toByte(),
            0x26.toUByte().toByte(),
            0x0d.toUByte().toByte(),
            0x31.toUByte().toByte(),
            0xe1.toUByte().toByte(),
            0xa7.toUByte().toByte(),
            0x7f.toUByte().toByte(),
            0x6c.toUByte().toByte(),
            0x02.toUByte().toByte(),
            0xb3.toUByte().toByte(),
            0x5a.toUByte().toByte(),
            0x9a.toUByte().toByte(),
            0x31.toUByte().toByte(),
            0x32.toUByte().toByte(),
            0xf2.toUByte().toByte(),
            0x32.toUByte().toByte(),
            0xdb.toUByte().toByte(),
            0x00.toUByte().toByte(),
            0x35.toUByte().toByte(),
            0x1d.toUByte().toByte(),
            0x9d.toUByte().toByte(),
            0x80.toUByte().toByte(),
            0x03.toUByte().toByte(),
            0x48.toUByte().toByte(),
            0x7d.toUByte().toByte(),
            0x4e.toUByte().toByte(),
            0xe5.toUByte().toByte(),
            0x28.toUByte().toByte(),
            0x47.toUByte().toByte(),
            0x99.toUByte().toByte(),
            0x03.toUByte().toByte(),
            0x13.toUByte().toByte(),
            0xda.toUByte().toByte(),
            0xa7.toUByte().toByte(),
            0xc7.toUByte().toByte(),
            0x21.toUByte().toByte(),
            0xf8.toUByte().toByte(),
            0x8d.toUByte().toByte(),
            0x04.toUByte().toByte(),
            0xbb.toUByte().toByte(),
            0x56.toUByte().toByte(),
            0xda.toUByte().toByte(),
            0x91.toUByte().toByte()
        )
    )

    override val deviceName = "Unit Test"
}
