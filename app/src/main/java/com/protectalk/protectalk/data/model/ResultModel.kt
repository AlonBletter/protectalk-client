package com.protectalk.protectalk.data.model

sealed class ResultModel<out T> {
    data class Ok<T>(val data: T) : ResultModel<T>()
    data class Err(val message: String, val cause: Throwable? = null) : ResultModel<Nothing>()
}
