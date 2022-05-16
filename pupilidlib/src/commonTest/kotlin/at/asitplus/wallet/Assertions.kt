package at.asitplus.wallet

import at.asitplus.wallet.pupilid.ServiceResult
import kotlin.test.assertTrue
import kotlin.test.fail

object Assertions {

    fun assertErrorFromNativeCode(result: ServiceResult) {
        when (result) {
            is ServiceResult.ErrorFromNativeCode -> assertTrue(true)
            is ServiceResult.ErrorFromNetwork -> fail(result.error)
            is ServiceResult.Success -> fail("Unexpected success")
        }
    }

    fun assertErrorFromNetwork(result: ServiceResult) {
        when (result) {
            is ServiceResult.ErrorFromNativeCode -> fail(result.throwable?.toString() ?: "NULL ERROR")
            is ServiceResult.ErrorFromNetwork -> assertTrue(true)
            is ServiceResult.Success -> fail("Unexpected success")
        }
    }

    fun assertSuccess(result: ServiceResult) {
        when (result) {
            is ServiceResult.ErrorFromNativeCode -> fail(result.throwable?.toString() ?: "NULL ERROR")
            is ServiceResult.ErrorFromNetwork -> fail(result.error)
            is ServiceResult.Success -> assertTrue(true)
        }
    }
}