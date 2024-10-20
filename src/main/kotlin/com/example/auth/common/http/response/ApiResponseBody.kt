package com.example.auth.common.http.response

open class ApiResponseBody(
    val success: Boolean,
)

data class SucceededApiResponseBody<T>(
    val data: T?,
) : ApiResponseBody(true) {
    companion object {
        fun unit(): SucceededApiResponseBody<Unit> = SucceededApiResponseBody(null)
    }
}

data class FailedApiResponseBody(
    val code: String,
    val message: String,
) : ApiResponseBody(false)

data class PaginatedApiResponseDto<T>(
    val contents: List<T>,
    val hasNextPage: Boolean,
    val totalCount: Long,
)

data class PaginatedApiResponseBody<T>(
    val data: PaginatedApiResponseDto<T>,
) : ApiResponseBody(true)