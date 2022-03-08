@file:UseSerializers(ByteArrayBase64Serializer::class)

package at.asitplus.wallet

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers


@Serializable
data class BindingParamsRequest(
    val deviceName: String,
)

@Serializable
data class BindingParamsResponse(
    val challenge: ByteArray,
)

@Serializable
data class BindingCsrRequest(
    val challenge: ByteArray,
    val csr: ByteArray,
    val deviceName: String,
    val attestationCerts: List<ByteArray>,
)

@Serializable
data class BindingCsrResponse(
    val certificate: ByteArray,
    val attestedPublicKey: ByteArray,
)

@Serializable
data class BindingConfirmRequest(
    val success: Boolean
)

@Serializable
data class BindingConfirmResponse(
    val success: Boolean
)