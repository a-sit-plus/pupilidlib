package at.asitplus.wallet.pupilid

sealed class ServiceResult {
    data class Success(val message: String, val xAuthToken: String?) : ServiceResult()
    data class ErrorFromNetwork(val error: String) : ServiceResult()
    data class ErrorFromNativeCode(val throwable: Any?) : ServiceResult()
}