package com.example.myapplication.openai.infra.dto


data class OpenAiRequest(
    val model: String,
    val messages: List<Any>,
    val temperature: Double
)
