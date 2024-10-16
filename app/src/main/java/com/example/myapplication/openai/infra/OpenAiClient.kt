package com.example.myapplication.openai.infra

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenAiClient {

    private const val BASE_URL = "https://api.openai.com/"

    fun create(): OpenAiApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(OpenAiApi::class.java)
    }

}