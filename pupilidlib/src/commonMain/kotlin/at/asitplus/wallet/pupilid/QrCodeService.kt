package at.asitplus.wallet.pupilid

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object QrCodeContentService {
    fun serializedContent(content: CredentialContent) : String {
        return Json.encodeToString(content)
    }
}