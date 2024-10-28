import com.example.myapplication.openai.infra.OpenAiApi
import com.example.myapplication.openai.infra.dto.OpenAiRequest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenAiService {
    suspend fun call(message: String): String {
        val request = OpenAiRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                Message(
                    role = "user",
                    content = "\"$message\" said by your friend. as a best friend, just shortly answer him/her. if he/she seems need some recommandation to eat or enjoy, try to do it according to his/her mood and character.",
                    max_tokens = 70
                )
            ),
            temperature = 0.5
        )

        val authInterceptor = Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer sk-Tm-W93r-CFu-r5WbcY4m8TYUysIUjF8ZMjfUrqfrHST3BlbkFJz3eYXZXcnXPeh1MAoFz6dnSmBw1EzNmmGVmfGGWK8A")
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

        return try {
            val response = openAiApi.getCompletion(request)
            if (response.isSuccessful) {
                response.body()?.getFirstContent() ?: ""
            } else {
                throw Exception("API 호출 실패: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("에러 발생: ${e.message}")
            ""
        }
    }
}