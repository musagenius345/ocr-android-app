package com.musagenius.ocrapp.domain.model

/**
 * Sealed class for handling operation results
 * Provides type-safe error handling
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    /**
     * Check if result is successful
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Check if result is error
     */
    fun isError(): Boolean = this is Error

    /**
     * Check if result is loading
     */
    fun isLoading(): Boolean = this is Loading

    /**
     * Get data if success, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Get data or throw exception
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Cannot get data while loading")
    }

    /**
     * Execute block if success
     */
    inline fun onSuccess(block: (T) -> Unit): Result<T> {
        if (this is Success) {
            block(data)
        }
        return this
    }

    /**
     * Execute block if error
     */
    inline fun onError(block: (Exception) -> Unit): Result<T> {
        if (this is Error) {
            block(exception)
        }
        return this
    }

    /**
     * Execute block if loading
     */
    inline fun onLoading(block: () -> Unit): Result<T> {
        if (this is Loading) {
            block()
        }
        return this
    }

    companion object {
        /**
         * Create success result
         */
        fun <T> success(data: T): Result<T> = Success(data)

        /**
         * Create error result
         */
        fun error(exception: Exception, message: String? = null): Result<Nothing> =
            Error(exception, message)

        /**
         * Create loading result
         */
        fun loading(): Result<Nothing> = Loading
    }
}
