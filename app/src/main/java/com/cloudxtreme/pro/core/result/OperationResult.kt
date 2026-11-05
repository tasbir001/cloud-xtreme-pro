package com.cloudxtreme.pro.core.result

/**
 * Standard Result wrapper for asynchronous and virtualization operations.
 */
sealed class OperationResult<out T> {
    data class Success<out T>(val data: T) : OperationResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : OperationResult<Nothing>()
    data class Progress(val percent: Int, val message: String) : OperationResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue
    }
}
