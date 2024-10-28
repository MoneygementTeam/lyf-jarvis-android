package com.example.myapplication.api

import com.example.myapplication.ApiLoggingInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://moneygement-api.o-r.kr/"

    private val loggingInterceptor = ApiLoggingInterceptor()

    // Scalars Converter 추가
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())  // 일반 텍스트 처리를 위해 추가
        .addConverterFactory(GsonConverterFactory.create())     // JSON 처리용으로 유지
        .client(OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}