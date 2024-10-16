package com.example.myapplication.openai.application

import com.example.myapplication.openai.infra.OpenAiApi
import com.example.myapplication.openai.infra.dto.OpenAiRequest
import com.example.myapplication.openai.infra.dto.OpenAiResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenAiService {

    fun call(message: String): String {
        var result: String = ""

        val request = OpenAiRequest(
            model = "text-davinci-003",
            prompt = message,
            max_tokens = 100
        )

        val authInterceptor = Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer sk-Tm-W93r-CFu-r5WbcY4m8TYUysIUjF8ZMjfUrqfrHST3BlbkFJz3eYXZXcnXPeh1MAoFz6dnSmBw1EzNmmGVmfGGWK8A") // 여기에 OpenAI API 키를 추가
                .build()
            chain.proceed(newRequest)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val openAiApi = retrofit.create(OpenAiApi::class.java)

        openAiApi.getCompletion(request).enqueue(object : retrofit2.Callback<OpenAiResponse> {
            override fun onResponse(call: Call<OpenAiResponse>, response: retrofit2.Response<OpenAiResponse>) {
                if (response.isSuccessful) {
                    val completionResponse = response.body()
                    completionResponse?.let {
                        val generatedText = it.choices[0].text
                        // 결과 처리
                        println("AI 응답: $generatedText")

                        result = generatedText
                    }
                } else {
                    // 에러 처리
                    println("API 호출 실패: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<OpenAiResponse>, t: Throwable) {
                // 네트워크 에러 처리
                println("네트워크 에러: ${t.message}")
            }
        })

        return result
    }
}