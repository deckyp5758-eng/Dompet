package com.example.core.result

/**
 * Representasi hasil operasi yang aman (safe result)
 * Membantu mendeteksi kegagalan secara terstruktur tanpa menyebabkan crash aplikasi.
 */
sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Failure(
        val message: String,
        val throwable: Throwable? = null,
        val errorCode: String? = null
    ) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> defaultValue
    }

    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (Failure) -> Unit): AppResult<T> {
        if (this is Failure) action(this)
        return this
    }
}

/**
 * Helper untuk mengeksekusi blok kode secara aman dan membungkusnya dalam AppResult
 */
inline fun <T> runCatchingApp(tag: String = "AppOperation", block: () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (e: Throwable) {
        AppResult.Failure(
            message = e.localizedMessage ?: "Terjadi kesalahan yang tidak terduga pada $tag",
            throwable = e
        )
    }
}
