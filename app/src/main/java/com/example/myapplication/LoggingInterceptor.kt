package com.example.myapplication

import android.util.Log
import okhttp3.Interceptor
import okio.Buffer

class ApiLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()

        val requestLog = StringBuilder().apply {
            appendLine("API 요청 ----->")
            appendLine("URL: ${request.url}")
            appendLine("Method: ${request.method}")
            appendLine("Headers: ${request.headers}")
            val requestBody = request.body
            if (requestBody != null) {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                appendLine("Body: ${buffer.readUtf8()}")
            }
        }
        Log.d("API_LOG", requestLog.toString())

        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val endTime = System.currentTimeMillis()

        val responseLog = StringBuilder().apply {
            appendLine("API 응답 <-----")
            appendLine("URL: ${response.request.url}")
            appendLine("Time taken: ${endTime - startTime}ms")
            appendLine("Code: ${response.code}")
            appendLine("Headers: ${response.headers}")
            val responseBody = response.body
            if (responseBody != null) {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer
                appendLine("Body: ${buffer.clone().readUtf8()}")
            }
        }
        Log.d("API_LOG", responseLog.toString())

        return response
    }
}