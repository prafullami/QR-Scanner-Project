package com.example.sync.network

data class ApiActionRequest<T>(
    val action: String,
    val date: String,
    val payload: T? = null
)

data class CreateFactoryPayload(
    val name: String,
    val location: String,
    val createdBy: String
)

data class ApiEnvelope<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: ApiMeta? = null
)

data class ApiError(
    val code: String? = null,
    val message: String? = null,
    val details: Any? = null
)

data class ApiMeta(
    val requestId: String? = null,
    val method: String? = null,
    val path: String? = null,
    val timestamp: String? = null
)

data class FactoryDto(
    val id: String? = null,
    val name: String? = null,
    val location: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null
)
