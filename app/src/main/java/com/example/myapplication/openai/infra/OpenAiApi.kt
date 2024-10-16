package com.example.myapplication.openai.infra

import com.example.myapplication.openai.infra.dto.OpenAiRequest
import com.example.myapplication.openai.infra.dto.OpenAiResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAiApi {

    @Headers("Content-Type: application/json")
    @POST("v1/completions")
    fun getCompletion(
        @Body request: OpenAiRequest
    ): Call<OpenAiResponse>

}
