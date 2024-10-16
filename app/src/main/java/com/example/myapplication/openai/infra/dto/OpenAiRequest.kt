package com.example.myapplication.openai.infra.dto

data class OpenAiRequest(
    val model: String,
    val prompt: String,
    val max_tokens: Int
)
