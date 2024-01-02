@file:UseSerializers(ByteArrayBase64Serializer::class)

package at.asitplus.wallet.pupilid

import at.asitplus.wallet.utils.ByteArrayBase64Serializer
import io.github.aakira.napier.Napier
import io.matthewnelson.component.base64.encodeBase64
import kotlinx.serialization.*
import kotlinx.serialization.json.Json


@Serializable
open class BindingParamsRequest(
    val deviceName: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BindingParamsRequest

        if (deviceName != other.deviceName) return false

        return true
    }

    override fun hashCode(): Int {
        return deviceName.hashCode()
    }

    override fun toString(): String {
        return "BindingParamsRequest(deviceName='$deviceName')"
    }

}

@Serializable
open class BindingParamsResponse(
    val challenge: ByteArray,
    val subject: String,
    val keyType: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BindingParamsResponse

        if (!challenge.contentEquals(other.challenge)) return false
        if (subject != other.subject) return false
        if (keyType != other.keyType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = challenge.contentHashCode()
        result = 31 * result + subject.hashCode()
        result = 31 * result + keyType.hashCode()
        return result
    }

    override fun toString(): String {
        return "BindingParamsResponse(challenge=${challenge.encodeBase64()}, subject='$subject', keyType='$keyType')"
    }

}

@Serializable
open class BindingCsrRequest(
    val challenge: ByteArray,
    val csr: ByteArray,
    val deviceName: String,
    val attestationCerts: List<ByteArray>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BindingCsrRequest

        if (!challenge.contentEquals(other.challenge)) return false
        if (!csr.contentEquals(other.csr)) return false
        if (deviceName != other.deviceName) return false
        if (attestationCerts != other.attestationCerts) return false

        return true
    }

    override fun hashCode(): Int {
        var result = challenge.contentHashCode()
        result = 31 * result + csr.contentHashCode()
        result = 31 * result + deviceName.hashCode()
        result = 31 * result + attestationCerts.hashCode()
        return result
    }

    override fun toString(): String {
        val attestationCertsString =
            attestationCerts.joinToString(separator = ", ") { it.encodeBase64() }
        return "BindingCsrRequest(challenge=${challenge.encodeBase64()}, csr=${csr.encodeBase64()}, deviceName='$deviceName', attestationCerts=[$attestationCertsString])"
    }

}

@Serializable
open class BindingCsrResponse(
    val certificate: ByteArray,
    /**
     * JWS with payload a serialized [AttestedPublicKey], signed by the backend key.
     */
    val attestedPublicKey: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BindingCsrResponse

        if (!certificate.contentEquals(other.certificate)) return false
        if (attestedPublicKey != other.attestedPublicKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = certificate.contentHashCode()
        result = 31 * result + (attestedPublicKey?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "BindingCsrResponse(certificate=${certificate.encodeBase64()}, attestedPublicKey=$attestedPublicKey)"
    }

}

@Serializable
open class BindingConfirmRequest(
    val success: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BindingConfirmRequest

        if (success != other.success) return false

        return true
    }

    override fun hashCode(): Int {
        return success.hashCode()
    }

    override fun toString(): String {
        return "BindingConfirmRequest(success=$success)"
    }

}

@Serializable
open class BindingConfirmResponse(
    val success: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BindingConfirmResponse

        if (success != other.success) return false

        return true
    }

    override fun hashCode(): Int {
        return success.hashCode()
    }

    override fun toString(): String {
        return "BindingConfirmResponse(success=$success)"
    }

}

@Serializable
data class CredentialContent(
    @SerialName("typ")
    val type: String,
    @SerialName("id")
    val id: String,
    @SerialName("exp")
    val qrCodeExpiration: Int,
    @SerialName("nbf")
    val qrCodeNotBefore: Int,
    @SerialName("vu")
    val validUntil: String,
    @SerialName("ex")
    val expiration: String,
    @SerialName("key")
    val key: String,
    @SerialName("pic")
    val scaledPictureHash: String? = null,
) {
    fun serialize() = jsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(it: String) =
            kotlin.runCatching { jsonSerializer.decodeFromString<CredentialContent>(it) }
                .onFailure { e -> Napier.e("Failure while deserializing", e) }
                .getOrNull()
    }
}


/**
 * Structure for the "attestedPublicKey" format for apps,
 * as returned in [BindingCsrResponse.attestedPublicKey].
 */
@Serializable
data class AttestedPublicKey(
    @SerialName("kid")
    val kid: String
) {
    fun serialize() = jsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(it: String) =
            kotlin.runCatching { jsonSerializer.decodeFromString<AttestedPublicKey>(it) }
                .onFailure { e -> Napier.e("Failure while deserializing", e) }
                .getOrNull()
    }
}

private val jsonSerializer = Json { ignoreUnknownKeys = true }