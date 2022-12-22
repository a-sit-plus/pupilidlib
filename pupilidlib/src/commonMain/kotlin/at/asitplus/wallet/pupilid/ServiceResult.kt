package at.asitplus.wallet.pupilid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class ServiceResult {
    data class Success(val message: String, val xAuthToken: String?) : ServiceResult()
    data class ErrorFromNetwork(val error: String, val details: SpringErrorDocument? = null) : ServiceResult()
    data class ErrorFromNativeCode(val throwable: Any?) : ServiceResult()
    object SignatureInvalid : ServiceResult()
}

@Serializable
data class SpringErrorDocument(
    @SerialName("timestamp")
    val timestamp: String? = null,
    @SerialName("status")
    val status: Int? = null,
    @SerialName("error")
    val error: String? = null,
    @SerialName("exception")
    val exception: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("path")
    val path: String? = null,
    @SerialName("trace")
    val trace: String? = null,
)