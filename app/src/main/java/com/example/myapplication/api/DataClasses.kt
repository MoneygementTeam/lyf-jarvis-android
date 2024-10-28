package com.example.myapplication.api

data class HistoryRequest(
    val userId: String,
    val groupId: String?,
    val request: String,
    val response: String
)

data class RecommendRequest(
    val userId: String,
    val request: String,
    val latitude: Double,
    val longitude: Double
)