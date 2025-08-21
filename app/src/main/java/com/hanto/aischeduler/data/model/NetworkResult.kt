package com.hanto.aischeduler.data.model

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val exception: Exception) : NetworkResult<Nothing>()
    data class Loading(val message: String = "로딩 중...") : NetworkResult<Nothing>()
}

// 확장 함수들
inline fun <T> NetworkResult<T>.onSuccess(action: (value: T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) action(data)
    return this
}

inline fun <T> NetworkResult<T>.onError(action: (exception: Exception) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Error) action(exception)
    return this
}

inline fun <T> NetworkResult<T>.onLoading(action: (message: String) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Loading) action(message)
    return this
}