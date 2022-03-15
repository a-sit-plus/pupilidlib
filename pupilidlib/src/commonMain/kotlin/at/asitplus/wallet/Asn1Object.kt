package at.asitplus.wallet


sealed class Asn1Object(
    private val tag: Byte,
    private val value: ByteArray
) {
    open val encoded get() = byteArrayOf(tag) + length(value.size) + value

    private fun length(size: Int) = when {
        size < 128 -> byteArrayOf(size.toByte())
        size < 0x100 -> byteArrayOfInts(0x81, (size and 0xff))
        size < 0x8000 -> byteArrayOfInts(0x82, (size and 0xff00) shr 8, (size and 0xff))
        else -> byteArrayOf() // TODO Error?
    }

    class Sequence(value: ByteArray) : Asn1Object(0x30, value) {
        constructor(value: List<Asn1Object>) : this(value.map { it.encoded }.concat())
        constructor(vararg value: Asn1Object) : this(value.map { it.encoded }.concat())
    }

    class Null : Asn1Object(0x05, byteArrayOf())
    class Utf8String(value: String) : Asn1Object(0x0c, value.encodeToByteArray())
    class Integer(value: Long) : Asn1Object(0x02, value.encodeToByteArray())
    class BitString(value: ByteArray) : Asn1Object(0x03, value)
    class Raw(val value: ByteArray) : Asn1Object(0x00, value) {
        constructor(vararg ints: Int) : this(ints.map { it.toByte() }.toByteArray())

        override val encoded: ByteArray
            get() {
                return value
            }
    }

    class Set(value: Asn1Object) : Asn1Object(0x31, value.encoded)
    class Oid(value: String) : Asn1Object(0x06, value.encodeToOid())
}

fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
fun Sequence<ByteArray>.concat(): ByteArray = reduce { acc, bytes -> acc + bytes }
fun List<ByteArray>.concat(): ByteArray = reduce { acc, bytes -> acc + bytes }
fun Array<out ByteArray>.concat() = this.reduce { acc, bytes -> acc + bytes }

private fun String.encodeToOid(): ByteArray {
    val numbers = this.split(".").mapNotNull { it.toIntOrNull() }
    if (numbers.isEmpty()) return byteArrayOf()
    if (numbers.size <= 1) return byteArrayOfInts(numbers[0])
    val firstByte = byteArrayOf(((numbers[0] * 40) + (numbers[1])).toByte())
    val remainingBytes = numbers.drop(2).map { it.encodeAsOidPart() }.concat()
    return firstByte + remainingBytes
}

private fun Int.encodeAsOidPart(): ByteArray {
    // Inspired by Bouncycastle's implementation (under MIT license)
    val bytes = ByteArray(9)
    var position = 8
    var value = this
    bytes[position] = (value and 0x7F).toByte()
    while (value > 0x7F) {
        value = value shr 7
        bytes[--position] = (value or 0x80).toByte()
    }
    return bytes.drop(position).toByteArray()
}

private fun Long.encodeToByteArray(): ByteArray {
    // Inspired by Bouncycastle's implementation (under MIT license)
    val bytes = ByteArray(9)
    var position = 8
    var value = this
    while (value != 0L) {
        bytes[position--] = (value and 0xFF).toByte()
        value = value shr 8
        if (this < 0 && value == -1L)
            break
    }
    val result = bytes.drop(position + 1).toByteArray()
    if (result.isEmpty())
        return byteArrayOf(0x00)
    if (result.first() < 0 && this >= 0L)
        return byteArrayOf(0x00) + result
    return result
}