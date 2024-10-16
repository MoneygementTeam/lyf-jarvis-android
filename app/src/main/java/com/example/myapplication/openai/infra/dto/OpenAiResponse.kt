package com.example.myapplication.openai.infra.dto

import com.example.myapplication.openai.domain.Choice

data class OpenAiResponse(
    val id: String,
    val choices: List<Choice>
)