package com.example.myapplication.openai.infra

import ChatCompletionResponse
import com.example.myapplication.openai.infra.dto.OpenAiRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun getCompletion(@Body request: OpenAiRequest): Response<ChatCompletionResponse>
}