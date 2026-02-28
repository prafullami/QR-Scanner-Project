package com.example.sync.network

import com.example.sync.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val factoryApiService: FactoryApiService by lazy {
        retrofit.create(FactoryApiService::class.java)
    }
}
