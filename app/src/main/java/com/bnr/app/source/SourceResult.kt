package com.bnr.app.source

sealed class SourceResult<out T> {
    data class Success<T>(val data: T) : SourceResult<T>()
    data class Error(val exception: SourceException) : SourceResult<Nothing>()
}

sealed class SourceException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkException(message: String, cause: Throwable? = null) : SourceException(message, cause)
    class ParseException(message: String, cause: Throwable? = null) : SourceException(message, cause)
    class CloudflareException(message: String, cause: Throwable? = null) : SourceException(message, cause)
    class NotFoundException(message: String) : SourceException(message)
    class RateLimitException(message: String) : SourceException(message)
}

inline fun <T> SourceResult<T>.getOrThrow(): T = when (this) {
    is SourceResult.Success -> data
    is SourceResult.Error -> throw exception
}

inline fun <T, R> SourceResult<T>.map(transform: (T) -> R): SourceResult<R> = when (this) {
    is SourceResult.Success -> SourceResult.Success(transform(data))
    is SourceResult.Error -> this
}

inline fun <T> SourceResult<T>.onSuccess(action: (T) -> Unit): SourceResult<T> {
    if (this is SourceResult.Success) action(data)
    return this
}

inline fun <T> SourceResult<T>.onError(action: (SourceException) -> Unit): SourceResult<T> {
    if (this is SourceResult.Error) action(exception)
    return this
}

fun <T> SourceResult<T>.getOrNull(): T? = when (this) {
    is SourceResult.Success -> data
    is SourceResult.Error -> null
}
