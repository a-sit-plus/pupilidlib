@file:UseSerializers(ByteArrayBase64Serializer::class)

package at.asitplus.wallet

import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers


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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BindingParamsResponse

        if (!challenge.contentEquals(other.challenge)) return false

        return true
    }

    override fun hashCode(): Int {
        return challenge.contentHashCode()
    }

    override fun toString(): String {
        return "BindingParamsResponse(challenge=${challenge.encodeBase64()})"
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
        val attestationCertsString = attestationCerts.joinToString(separator = ", ") { it.encodeBase64() }
        return "BindingCsrRequest(challenge=${challenge.encodeBase64()}, csr=${csr.encodeBase64()}, deviceName='$deviceName', attestationCerts=[$attestationCertsString])"
    }

}

@Serializable
open class BindingCsrResponse(
    val certificate: ByteArray,
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
