package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object QrCodeContentService {
    /**
     * Serializes the [content] to JSON
     */
    fun serializedContent(content: CredentialContent) = Json.encodeToString(content)

    /**
     * Parses [json] (a JSON string) into a [CredentialContent]
     */
    fun deserializedContent(json: String): CredentialContent? = try {
        Json.decodeFromString(json)
    } catch (e: Throwable) {
        null
    }

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
     * 6. Prepend with `PI1:1-3:` and `PI1:2-3:` and so on
     * 7. Return list of these strings
     *
     * Clients shall encode each String as a QR code
     */
    suspend fun encodeInQrCode(
        content: CredentialContent,
        picture: ByteArray,
        splitLength: Int = 1701,
        callback: Callback,
    ): KmmResult<List<String>> {
        val serializedContent = Json.encodeToString(content)
        val jws = callback.sign(serializedContent)
            .getOrElse { return KmmResult.failure(it) }
        val extendedContent = ExtendedQrCodeContent(jws, picture)
        val cbor = Cbor.encodeToByteArray(extendedContent)
        return encodeInQrCode(
            payload = cbor,
            prefix = "PI1:",
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
     * Decodes a collection of scanned QR codes with prefix [prefix] (e.g. "PI1:")
     */
    fun decodeQrCode(
        input: Collection<String>,
        prefix: String = "PI1:",
        callback: DecodeCallback,
    ): KmmResult<DecodeResult> {
        val prefixWithoutSemicolon = prefix.removeSuffix(":")
        val base45 = input.sorted().mapIndexed { index, s ->
            s.removePrefix("$prefixWithoutSemicolon:${index + 1}-${input.size}:")
        }.joinToString(separator = "")
        return kotlin.runCatching {
            val cbor = Base45Encoder.decode(base45)
            val extendedContent = Cbor.decodeFromByteArray<ExtendedQrCodeContent>(cbor)
            val jwsPayload = callback.verifyJwsExtractPayload(extendedContent.jws)
                .getOrElse { return KmmResult.failure(it) }
            val content = Json.decodeFromString<CredentialContent>(jwsPayload)
            val result = DecodeResult(content, extendedContent.picture)
            KmmResult.success(result)
        }.getOrElse {
            KmmResult.failure(it)
        }
    }
}

@kotlinx.serialization.Serializable
data class ExtendedQrCodeContent(
    @SerialName("jws")
    val jws: String,
    @SerialName("picture")
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


