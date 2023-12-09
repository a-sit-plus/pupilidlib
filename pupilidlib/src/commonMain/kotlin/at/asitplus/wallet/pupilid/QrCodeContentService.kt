package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import at.asitplus.wallet.utils.Base45Encoder
import io.github.aakira.napier.Napier
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.cbor.ByteString
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.toByteString

/**
 * Encode and decode [CredentialContent] into QR Codes, used by Wallet and Verifier Apps.
 * Clients may use [PupilIdQrCodeService] to get cryptographic validation.
 */
object QrCodeContentService {

    fun interface Callback {

        /**
         * Implementers: Create a signed JWS with [input] as the payload.
         */
        suspend fun sign(input: String): KmmResult<String>
    }

    /**
     * 1. Encodes the [content] as JSON
     * 2. Creates a signed JWS with [callback]
     * 3. Encodes in CBOR (a simple data structure with `jws` and `picture`)
     * 4. Encode in Base45
     * 5. Split every [splitLength] characters
     * 6. Prepend with [prefix] and counter, e.g. `PI1:1-3:` and `PI1:2-3:` and so on
     * 7. Return list of these strings
     *
     * Clients shall encode each String as a QR code and display them.
     *
     * [prefix] may include a custom random string to distinguish QR codes, e.g. `PI1:AG4x:`, but no `-`
     */
    suspend fun encodeInQrCode(
        content: CredentialContent,
        picture: ByteArray,
        splitLength: Int = 1701,
        prefix: String = "PI1:",
        callback: Callback,
    ): KmmResult<List<String>> {
        if (prefix.contains('-')) return KmmResult.failure(IllegalArgumentException("prefix contains -"))
        val serializedContent = Json.encodeToString(content)
        val jws = callback.sign(serializedContent)
            .getOrElse { return KmmResult.failure(it) }
        val extendedContent = ExtendedQrCodeContent(jws, picture)
        val cbor = Cbor.encodeToByteArray(extendedContent)
        return encodeInQrCode(
            payload = cbor,
            prefix = prefix,
            splitLength = splitLength
        )
    }

    /**
     * 1. Encode in Base45
     * 2. Split every [splitLength] characters
     * 3. Prepend with [prefix] and the sequence number `1-3:`, `2-3:` and so on
     * 4. Return list of these strings
     *
     * Clients shall encode each String as a QR code
     */
    fun encodeInQrCode(
        payload: String,
        prefix: String = "PI2:",
        splitLength: Int = 1701,
    ): KmmResult<List<String>> {
        if (prefix.contains('-')) return KmmResult.failure(IllegalArgumentException("prefix contains -"))
        return encodeInQrCode(
            payload = payload.encodeToByteArray(),
            prefix = prefix,
            splitLength = splitLength
        )
    }

    /**
     * 1. Encode in Base45
     * 2. Split every [splitLength] characters
     * 3. Prepend with [prefix] and the sequence number `1-3:`, `2-3:` and so on
     * 4. Return list of these strings
     *
     * Clients shall encode each String as a QR code
     */
    fun encodeInQrCode(
        payload: ByteArray,
        prefix: String = "PI2:",
        splitLength: Int = 1701,
    ): KmmResult<List<String>> {
        if (prefix.contains('-')) return KmmResult.failure(IllegalArgumentException("prefix contains -"))
        val base45 = Base45Encoder.encode(payload)
        val result = splitEveryNChars(base45, splitLength)
        val prefixWithoutSemicolon = prefix.removeSuffix(":")
        val list = result.mapIndexed { index, s ->
            "$prefixWithoutSemicolon:${index + 1}-${result.size}:$s"
        }
        return KmmResult.success(list)
    }

    private fun splitEveryNChars(input: String, n: Int): MutableList<String> {
        val result = mutableListOf<String>()
        var splitIndex = 0
        while (splitIndex < input.length) {
            if (splitIndex + n > input.length)
                result += input.substring(splitIndex)
            else
                result += input.substring(splitIndex, splitIndex + n)
            splitIndex += n
        }
        return result
    }

    data class DecodeResult(
        val content: CredentialContent,
        val picture: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as DecodeResult

            if (content != other.content) return false
            if (!picture.contentEquals(other.picture)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = content.hashCode()
            result = 31 * result + picture.contentHashCode()
            return result
        }
    }

    fun interface DecodeCallback {
        /**
         * Implementers: Verify JWS from Wallet App and return payload
         */
        fun verifyJwsExtractPayload(input: String): KmmResult<String>
    }

    /**
     * Decodes a collection of scanned QR codes in [input] with a custom [prefix] (e.g. `PI1:`).
     * If any input does not start with [prefix], an error is returned.
     *
     * If [stripSecondPrefix] is true, the input strings are expected to contain a random string between
     * [prefix] and the counter, e.g. `PI1:AG4x:1-3:...`, which will be stripped by this method.
     *
     * If multiple "second prefixes" are contained in the input, the first (by random) complete input will be decoded.
     *
     * Custom exceptions returned in `KmmResult` include:
     * - [InputInsufficientError]: The list of inputs is not complete
     * - [QrCodeDecodingError]: The QR Code can't be decoded
     * - [PictureNotMatchingError]: The picture and credential content are not linked correctly
     * - [InputIncorrectError]: The list of inputs contains strings not starting with [prefix]
     */
    fun decodeQrCode(
        input: Collection<String>,
        prefix: String = "PI1:",
        stripSecondPrefix: Boolean = false,
        callback: DecodeCallback,
    ): KmmResult<DecodeResult> {
        Napier.d("decodeQrCode: Got ${input.size} input strings")
        val prefixWithoutSemicolon = prefix.removeSuffix(":")
        if (input.isEmpty())
            return KmmResult.failure(InputInsufficientError(0, 0))
        if (input.any { !it.startsWith("$prefixWithoutSemicolon:") })
            return KmmResult.failure(InputIncorrectError())

        return runCatching {
            val base45 = input.extractPayload(prefixWithoutSemicolon, stripSecondPrefix)
            val cbor = Base45Encoder.decode(base45)
            val extendedContent = Cbor.decodeFromByteArray<ExtendedQrCodeContent>(cbor)
            val jwsPayload = callback.verifyJwsExtractPayload(extendedContent.jws)
                .getOrElse { return KmmResult.failure(it) }
            val content = Json.decodeFromString<CredentialContent>(jwsPayload)
            if (extendedContent.picture.isNotEmpty() && content.scaledPictureHash == null)
                throw PictureNotMatchingError()
            content.scaledPictureHash?.let { includedHash ->
                val computedHash = extendedContent.picture.toByteString().sha256().toByteArray()
                if (!computedHash.contentEquals(includedHash.decodeBase64Bytes()))
                    throw PictureNotMatchingError()
            }
            val result = DecodeResult(content, extendedContent.picture)
            KmmResult.success(result)
        }.getOrElse {
            Napier.w("decodeQrCode: Error", it)
            if (it is PictureNotMatchingError)
                return KmmResult.failure(it)
            if (it is InputInsufficientError)
                return KmmResult.failure(it)
            KmmResult.failure(QrCodeDecodingError(it))
        }
    }

    data class MapResult(
        val secondPrefix: String? = null,
        val actualCount: Int,
        val expectedCount: Int,
    )

    /**
     * Analyzes the given [input], i.e. expecting all strings to start with [prefix],
     * and extracting and mapping potential second prefixes, see also [MapResult].
     */
    fun analyzeQrCode(
        input: Collection<String>,
        prefix: String = "PI1:"
    ): KmmResult<List<MapResult>> {
        Napier.d("analyzeQrCode: Got ${input.size} strings")
        val prefixWithoutSemicolon = prefix.removeSuffix(":")
        if (input.isEmpty())
            return KmmResult.failure(InputInsufficientError(0, 0))
        if (input.any { !it.startsWith("$prefixWithoutSemicolon:") })
            return KmmResult.failure(InputIncorrectError())

        return kotlin.runCatching {
            val result = mutableListOf<MapResult>()
            val map = input.mapBySecondPrefix(prefixWithoutSemicolon)
            map.forEach {
                result.add(MapResult(it.key, it.value.size, it.value.expectedSize(prefixWithoutSemicolon)))
            }
            KmmResult.success<List<MapResult>>(result)
        }.getOrElse {
            KmmResult.success(listOf(MapResult(null, input.size, input.expectedSize(prefixWithoutSemicolon))))
        }
    }

    @Throws(InputInsufficientError::class)
    private fun Collection<String>.extractPayload(prefixWithoutSemicolon: String, stripSecondPrefix: Boolean) =
        if (stripSecondPrefix)
            this.extractPayloadStrippingSecondPrefix(prefixWithoutSemicolon)
        else
            this.extractPayload(prefixWithoutSemicolon)

    @Throws(InputInsufficientError::class)
    private fun Collection<String>.extractPayloadStrippingSecondPrefix(prefixWithoutSemicolon: String): String {
        val mapSecondPrefixToPayload = mapBySecondPrefix(prefixWithoutSemicolon)
        mapSecondPrefixToPayload.forEach {
            val base45 = runCatching { it.value.extractPayload("$prefixWithoutSemicolon:${it.key}") }.getOrNull()
            if (base45 != null)
                return base45
        }
        throw InputInsufficientError(
            mapSecondPrefixToPayload.maxOf { it.value.size },
            mapSecondPrefixToPayload.maxOf { it.value.expectedSize(prefixWithoutSemicolon) })
    }

    @Throws(IllegalArgumentException::class)
    private fun Collection<String>.mapBySecondPrefix(prefixWithoutSemicolon: String): MutableMap<String, MutableList<String>> {
        val mapSecondPrefixToPayload = mutableMapOf<String, MutableList<String>>()
        distinct().forEach {
            val secondPrefix = it.removePrefix("$prefixWithoutSemicolon:").takeWhile { it != ':' }
            if (secondPrefix.contains("-")) throw IllegalArgumentException()
            mapSecondPrefixToPayload.getOrPut(secondPrefix) { mutableListOf() }.add(it)
        }
        return mapSecondPrefixToPayload
    }

    @Throws(InputInsufficientError::class)
    private fun Collection<String>.extractPayload(prefixWithoutSemicolon: String): String {
        val expectedCount = expectedSize(prefixWithoutSemicolon)
        val actualCount = distinct().count()
        Napier.d("decodeQrCode: Expecting $expectedCount inputs, got $actualCount distinct input strings")
        if (actualCount != expectedCount)
            throw InputInsufficientError(actualCount, expectedCount)
        return distinct().sorted().mapIndexed { index, s ->
            s.removePrefix("$prefixWithoutSemicolon:${index + 1}-$size:")
        }.joinToString(separator = "")
    }

    private fun Collection<String>.expectedSize(prefixWithoutSemicolon: String) = minOf { it }
        .removePrefix("$prefixWithoutSemicolon:")
        .dropWhile { it != '-' }.drop(1)
        .takeWhile { it != ':' }.toIntOrNull() ?: 1
}

@OptIn(ExperimentalSerializationApi::class)
@kotlinx.serialization.Serializable
data class ExtendedQrCodeContent(
    @SerialName("jws")
    val jws: String,
    @SerialName("picture")
    @ByteString
    val picture: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExtendedQrCodeContent

        if (jws != other.jws) return false
        if (!picture.contentEquals(other.picture)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jws.hashCode()
        result = 31 * result + picture.contentHashCode()
        return result
    }
}

class InputInsufficientError(val count: Int, val expectedCount: Int) : Throwable("Need more input")
class QrCodeDecodingError(it: Throwable) : Throwable("QR Code Decoding Error", it)
class PictureNotMatchingError : Throwable("Picture hash does not match")
class InputIncorrectError : Throwable("Input does not start with prefix")