package at.asitplus.wallet.pupilid

/**
 * For easy use of this KMM library under iOS, we need a class like `Result`
 * that is not a `value` class (which is unsupported in Kotlin/Native)
 */
class KmmResult<T> {

    val error: Throwable?
    val value: T?

    constructor(value: T) {
        this.value = value
        this.error = null
    }

    constructor(error: Throwable) {
        this.value = null
        this.error = error
    }

    fun getOrNull(): T? {
        return value
    }

    fun getOrThrow(): T {
        if (value != null)
            return value
        throw error!!
    }

    inline fun getOrElse(onFailure: (exception: Throwable) -> T): T {
        @Suppress("UNCHECKED_CAST")
        return when (error) {
            null -> value as T
            else -> onFailure(error)
        }
    }

    companion object {
        fun <T> success(value: T): KmmResult<T> {
            return KmmResult(value)
        }

        fun <T> failure(error: Throwable): KmmResult<T> {
            return KmmResult(error)
        }
    }

}
