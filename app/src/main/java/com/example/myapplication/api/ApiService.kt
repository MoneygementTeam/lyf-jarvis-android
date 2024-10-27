package com.example.myapplication.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("your/endpoint/path")  // 실제 엔드포인트 경로로 변경하세요
    suspend fun sendActivityRequest(@Body request: ActivityRequest): Response<Unit>
}