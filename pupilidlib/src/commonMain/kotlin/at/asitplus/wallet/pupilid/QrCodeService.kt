package at.asitplus.wallet.pupilid

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object QrCodeContentService {
    fun serializedContent(content: CredentialContent) = Json.encodeToString(content)

    fun deserializedContent(json: String) : CredentialContent? = try {
        Json.decodeFromString(json)
    } catch (e: Throwable) {
        null
    }
}