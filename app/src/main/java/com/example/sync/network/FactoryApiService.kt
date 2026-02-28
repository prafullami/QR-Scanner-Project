package com.example.sync.network

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface FactoryApiService {

    @POST
    suspend fun postAction(
        @Url url: String,
        @Body request: ApiActionRequest<JsonElement>
    ): Response<ApiEnvelope<JsonElement>>
}
