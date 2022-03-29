package at.asitplus.wallet

sealed class ServiceResult {
    data class Success(val message: String) : ServiceResult()
    data class ErrorFromNetwork(val error: String) : ServiceResult()
    data class ErrorFromNativeCode(val throwable: Any?) : ServiceResult()
}