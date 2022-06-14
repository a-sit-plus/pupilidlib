package at.asitplus.wallet

import at.asitplus.wallet.pupilid.Asn1Object
import at.asitplus.wallet.pupilid.byteArrayOfInts
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class Asn1ObjectTest {

    @Test
    fun testOidEncoding() {
        Asn1Object.Oid("1.3.6.1.4.1.21.20").encoded shouldBe
                byteArrayOfInts(0x06, 0x07, 0x2b, 0x06, 0x01, 0x04, 0x01, 0x15, 0x14)

        Asn1Object.Oid("1.2.840.10045.3.1.7").encoded shouldBe
                byteArrayOfInts(0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03, 0x01, 0x07)

        Asn1Object.Oid("1.2.840.10045.4.3.2").encoded shouldBe
                byteArrayOfInts(0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x04, 0x03, 0x02)

        Asn1Object.Oid("1.2.840.113549.1.1.11").encoded shouldBe
                byteArrayOfInts(0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xF7, 0x0D, 0x01, 0x01, 0x0B)
    }
}