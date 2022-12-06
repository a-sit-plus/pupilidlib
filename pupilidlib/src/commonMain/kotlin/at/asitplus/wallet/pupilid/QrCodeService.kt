package at.asitplus.wallet.pupilid

import kotlinx.serialization.ExperimentalSerializationApi
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
     * 4. Applies ZLIB compression
     * 5. Encode in Base45
     * 6. Split every [splitLength] characters
     * 7. Prepend with `PI1:1-3:` and `PI1:2-3:` and so on
     * 8. Return list of these strings
     *
     * Clients shall encode each String as a QR code
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun encodeInQrCode(
        content: CredentialContent,
        createSignedJwsWithPayload: (String) -> String,
        picture: ByteArray,
        splitLength: Int = 1701,
    ): List<String>? {
        val serializedContent = Json.encodeToString(content)
        val jws = createSignedJwsWithPayload(serializedContent)
        val extendedContent = ExtendedQrCodeContent(jws, picture)
        val cbor = Cbor.encodeToByteArray(extendedContent)
        val gzipped = DefaultZlibService().compress(cbor) ?: return null
        val base45 = Base45Encoder.encode(gzipped)
        val result = mutableListOf<String>()
        var splitIndex = 0
        while (splitIndex < base45.length) {
            if (splitIndex + splitLength > base45.length)
                result += base45.substring(splitIndex)
            else
                result += base45.substring(splitIndex, splitIndex + splitLength)
            splitIndex += splitLength
        }
        return result.mapIndexed { index, s ->
            "PI1:${index + 1}-${result.size}:$s"
        }
    }

    data class DecodeResult(
        val content: CredentialContent,
        val picture: ByteArray,
    )

    /**
     * Decodes a list of scanned QR codes with prefix `PI1`
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun decodeQrCode(
        input: List<String>,
        verifyJwsExtractPayload: (String) -> String?
    ): DecodeResult? {
        val base45 = input.mapIndexed { index, s ->
            s.removePrefix("PI1:${index + 1}-${input.size}:")
        }.joinToString(separator = "")
        val gzipped = Base45Encoder.decode(base45)
        val cbor = DefaultZlibService().decompress(gzipped) ?: return null
        val extendedContent = Cbor.decodeFromByteArray<ExtendedQrCodeContent>(cbor)
        val jwsPayload = verifyJwsExtractPayload(extendedContent.jws) ?: return null
        val content = Json.decodeFromString<CredentialContent>(jwsPayload)
        return DecodeResult(content, extendedContent.picture)
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


