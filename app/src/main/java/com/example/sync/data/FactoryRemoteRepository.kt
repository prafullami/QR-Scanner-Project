package com.example.sync.data

import com.example.sync.BuildConfig
import com.example.sync.network.ApiActionRequest
import com.example.sync.network.CreateFactoryPayload
import com.example.sync.network.FactoryDto
import com.example.sync.network.ApiEnvelope
import com.example.sync.network.NetworkModule
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.time.LocalDate
import retrofit2.Response

class FactoryRemoteRepository {

    private val gson = Gson()

    data class CreateFactoryResult(
        val message: String?,
        val record: FactoryDto?
    )

    suspend fun createFactory(name: String, location: String, createdBy: String): Result<CreateFactoryResult> {
        return runCatching {
            val request = ApiActionRequest(
                action = "factory.create",
                date = LocalDate.now().toString(),
                payload = CreateFactoryPayload(
                    name = name,
                    location = location,
                    createdBy = createdBy
                )
            )

            val response = postAction(request.toJsonActionRequest())
            if (!response.isSuccessful) {
                throw IllegalStateException("Create factory HTTP ${response.code()}")
            }

            val body = response.body() ?: throw IllegalStateException("Empty server response")
            if (!body.ok) {
                throw IllegalStateException(body.error?.message ?: "Create factory failed")
            }

            parseCreateFactoryResult(body.data)
        }
    }

    suspend fun listFactories(): Result<List<FactoryDto>> {
        return runCatching {
            val request = ApiActionRequest<JsonElement>(
                action = "factory.list",
                date = LocalDate.now().toString(),
                payload = null
            )

            val response = postAction(request)
            if (!response.isSuccessful) {
                throw IllegalStateException("Factory sync HTTP ${response.code()}")
            }

            val body = response.body() ?: throw IllegalStateException("Empty server response")
            if (!body.ok) {
                throw IllegalStateException(body.error?.message ?: "Factory sync failed")
            }

            parseFactories(body.data)
        }
    }

    private fun ApiActionRequest<CreateFactoryPayload>.toJsonActionRequest(): ApiActionRequest<JsonElement> {
        return ApiActionRequest(
            action = action,
            date = date,
            payload = gson.toJsonTree(payload)
        )
    }

    private suspend fun postAction(
        request: ApiActionRequest<JsonElement>
    ): Response<ApiEnvelope<JsonElement>> {
        return NetworkModule.factoryApiService.postAction(
            url = BuildConfig.API_ACTION_URL,
            request = request
        )
    }

    private fun parseFactories(data: JsonElement?): List<FactoryDto> {
        if (data == null || data.isJsonNull) return emptyList()

        if (data.isJsonArray) {
            return parseFactoryArray(data.asJsonArray)
        }

        if (data.isJsonObject) {
            val obj = data.asJsonObject
            val keyedArray = extractFactoryArray(obj)
            if (keyedArray != null) {
                return parseFactoryArray(keyedArray)
            }

            // Fallback: server may return a single factory object in data.
            val single = gson.fromJson(obj, FactoryDto::class.java)
            return listOf(single).filter { !it.name.isNullOrBlank() }
        }

        return emptyList()
    }

    private fun extractFactoryArray(obj: JsonObject): JsonArray? {
        val candidateKeys = listOf("factories", "records", "items", "rows", "list", "data")
        for (key in candidateKeys) {
            val value = obj.get(key)
            if (value != null && value.isJsonArray) {
                return value.asJsonArray
            }
        }
        return null
    }

    private fun parseFactoryArray(array: JsonArray): List<FactoryDto> {
        return array.mapNotNull { element ->
            runCatching { gson.fromJson(element, FactoryDto::class.java) }
                .getOrNull()
                ?.takeIf { !it.name.isNullOrBlank() }
        }
    }

    private fun parseCreateFactoryResult(data: JsonElement?): CreateFactoryResult {
        if (data == null || !data.isJsonObject) {
            return CreateFactoryResult(message = null, record = null)
        }

        val dataObject = data.asJsonObject
        val message = dataObject.get("message")?.takeIf { !it.isJsonNull }?.asString
        val record = dataObject.get("record")
            ?.takeIf { it.isJsonObject }
            ?.let { gson.fromJson(it, FactoryDto::class.java) }

        return CreateFactoryResult(message = message, record = record)
    }
}
