package at.asitplus.wallet

import kotlin.test.Test
import kotlin.test.assertContentEquals

class Asn1ObjectTest {

    @Test
    fun testOidEncoding() {
        assertContentEquals(
            byteArrayOfInts(0x06, 0x07, 0x2b, 0x06, 0x01, 0x04, 0x01, 0x15, 0x14),
            Asn1Object.Oid("1.3.6.1.4.1.21.20").encode()
        )
        assertContentEquals(
            byteArrayOfInts(0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03, 0x01, 0x07),
            Asn1Object.Oid("1.2.840.10045.3.1.7").encode()
        )
        assertContentEquals(
            byteArrayOfInts(0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x04, 0x03, 0x02),
            Asn1Object.Oid("1.2.840.10045.4.3.2").encode()
        )
    }
}