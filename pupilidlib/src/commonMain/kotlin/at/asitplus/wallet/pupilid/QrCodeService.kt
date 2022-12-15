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

    /**
     * 1. Encodes the [content] as JSON
     * 2. Creates a signed JWS with callback [createSignedJwsWithPayload]
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
        createSignedJwsWithPayload: suspend (String) -> KmmResult<String>,
        picture: ByteArray,
        splitLength: Int = 1701,
    ): KmmResult<List<String>> {
        val serializedContent = Json.encodeToString(content)
        val jws = createSignedJwsWithPayload(serializedContent)
            .getOrElse { return KmmResult.failure(it) }
        val extendedContent = ExtendedQrCodeContent(jws, picture)
        val cbor = Cbor.encodeToByteArray(extendedContent)
        val base45 = Base45Encoder.encode(cbor)
        val result = splitEveryNChars(base45, splitLength)
        val list = result.mapIndexed { index, s ->
            "PI1:${index + 1}-${result.size}:$s"
        }
        return KmmResult.success(list)
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
        val base45 = Base45Encoder.encode(payload.encodeToByteArray())
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

    /**
     * Decodes a list of scanned QR codes with prefix `PI1`
     */
    fun decodeQrCode(
        input: List<String>,
        verifyJwsExtractPayload: (String) -> KmmResult<String>
    ): KmmResult<DecodeResult> {
        val base45 = input.mapIndexed { index, s ->
            s.removePrefix("PI1:${index + 1}-${input.size}:")
        }.joinToString(separator = "")
        val cbor = Base45Encoder.decode(base45)
        val extendedContent = Cbor.decodeFromByteArray<ExtendedQrCodeContent>(cbor)
        val jwsPayload = verifyJwsExtractPayload(extendedContent.jws)
            .getOrElse { return KmmResult.failure(it) }
        val content = Json.decodeFromString<CredentialContent>(jwsPayload)
        val result = DecodeResult(content, extendedContent.picture)
        return KmmResult.success(result)
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


