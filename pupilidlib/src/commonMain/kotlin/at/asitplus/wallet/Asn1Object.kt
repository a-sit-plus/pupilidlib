package at.asitplus.wallet


sealed class Asn1Object(
    private val tag: Byte,
    private val value: ByteArray
) {
    open fun encode() = byteArrayOf(tag) + length(value.size) + value

    private fun length(size: Int) = when {
        size < 128 -> byteArrayOf(size.toByte())
        size < 0x100 -> byteArrayOfInts(0x81, (size and 0xff))
        size < 0x8000 -> byteArrayOfInts(0x82, (size and 0xff00) shr 8, (size and 0xff))
        else -> byteArrayOf() // TODO Error?
    }

    class Sequence(value: ByteArray) : Asn1Object(0x30, value) {
        constructor(value: List<Asn1Object>) : this(value.map { it.encode() }.concat())
        constructor(vararg value: Asn1Object) : this(value.map { it.encode() }.concat())
    }

    class Utf8String(value: String) : Asn1Object(0x0c, value.encodeToByteArray())
    class Integer(value: Int) : Asn1Object(0x02, value.encodeToAsn1())
    class BitString(value: ByteArray) : Asn1Object(0x03, value)
    class Raw(val value: ByteArray) : Asn1Object(0x00, value) {
        constructor(vararg ints: Int) : this(ints.map { it.toByte() }.toByteArray())

        override fun encode(): ByteArray {
            return value
        }
    }

    class Set(value: Asn1Object) : Asn1Object(0x31, value.encode())
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
    val remainingBytes = numbers.drop(2).map { it.encodeToAsn1() }.concat()
    return firstByte + remainingBytes
}

private fun Int.encodeToAsn1() = when {
    this <= 0x7f -> byteArrayOf(this.toByte())
    this <= 0xff -> byteArrayOf(0x00, this.toByte())
    this <= 0xffff -> byteArrayOfInts((((this and 0xff00) shr 8) shl 1) or 0x80, (this and 0x00ff) and (0x7f))
    else -> byteArrayOf() // todo throw error
}