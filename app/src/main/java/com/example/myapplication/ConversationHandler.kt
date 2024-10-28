package com.example.myapplication

import OpenAiService
import android.util.Log
import com.example.myapplication.api.HistoryRequest
import com.example.myapplication.api.RecommendRequest
import com.example.myapplication.api.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class ConversationState {
    IDLE,
    GREETING,
    LISTENING,
    RESPONDING
}

// API 응답을 위한 데이터 클래스
data class RecommendResponse(
    val message: String,
    // 다른 필요한 필드들...
)

class ConversationHandler(
    private val speakText: (String) -> Unit,
    private val serviceScope: CoroutineScope,
    private val onStateChange: (RobotState) -> Unit
) {
    private var currentState = ConversationState.IDLE
    private var lastUserQuery: String = ""
    private var pendingResponse: String = ""
    private val specificWord = "Jarvis"
    private val openAiService: OpenAiService = OpenAiService()
    private val apiService = RetrofitClient.apiService
    private val gson = Gson()

    fun handleConversation(recognizedText: String) {
        when (currentState) {
            ConversationState.IDLE -> {
                if (recognizedText.lowercase().contains(specificWord.lowercase())) {
                    currentState = ConversationState.GREETING
                    onStateChange(RobotState.GO)
                    speakText("Hello! How is it going?")
                }
            }
            ConversationState.GREETING -> {
                lastUserQuery = recognizedText
                currentState = ConversationState.LISTENING
                onStateChange(RobotState.LISTEN)
                handleUserQuery(recognizedText)
            }
            ConversationState.LISTENING -> {
                lastUserQuery = recognizedText
                handleUserQuery(recognizedText)
            }
            ConversationState.RESPONDING -> {
                // 응답 중에는 새로운 입력을 무시
            }
        }
    }

    private fun handleUserQuery(query: String) {
        currentState = ConversationState.RESPONDING
        onStateChange(RobotState.LISTEN)

        serviceScope.launch {
            try {
                val isRecommendQuery = query.lowercase().contains("recommend")

                if (isRecommendQuery) {
                    // recommend 쿼리 처리
                    onStateChange(RobotState.WAIT)
                    handleRecommendQuery(query)
                } else {
                    // 일반 쿼리 처리
                    handleNormalQuery(query)
                }

            } catch (e: Exception) {
                Log.e(TAG, "에러: ${e.message}")
                pendingResponse = "Sorry, I couldn't process your request"
                onStateChange(RobotState.LISTEN)
                speakText(pendingResponse)
            }
        }
        currentState = ConversationState.LISTENING
    }

    private suspend fun handleRecommendQuery(query: String) {
        val recommendRequest = RecommendRequest(
            userId = "bobsbeautifulife",
            request = query,
            latitude = 1.2903,
            longitude = 103.8515
        )

        try {
            val response = apiService.postRecommend(recommendRequest)
            Log.d(TAG, "Recommend API 호출 결과: ${response.code()}")

            if (response.isSuccessful) {
                // 응답 본문을 직접 String으로 받음
                pendingResponse = response.body() ?: "Sorry, I couldn't get the recommendations at this moment."
                Log.d(TAG, "Recommend API 응답: $pendingResponse")
            } else {
                // 에러 응답 처리
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Recommend API 실패 응답: $errorBody")
                pendingResponse = "Sorry, I couldn't get the recommendations at this moment."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recommend API 호출 실패", e)
            e.printStackTrace()
            pendingResponse = "Sorry, there was an error processing your recommendation request."
        }

        onStateChange(RobotState.LISTEN)
        speakText(pendingResponse)
    }

    private suspend fun handleNormalQuery(query: String) {
        try {
            // OpenAI 응답 받기
            val result = openAiService.call(query)
            pendingResponse = result

            // History API 호출
            val historyRequest = HistoryRequest(
                userId = "bobsbeautifulife",
                groupId = null,
                request = query,
                response = result
            )

            try {
                val response = apiService.postHistory(historyRequest)
                Log.d(TAG, """
                    History API 호출 결과:
                    Status Code: ${response.code()}
                    Is Successful: ${response.isSuccessful}
                    Error Body: ${response.errorBody()?.string()}
                """.trimIndent())
            } catch (e: Exception) {
                Log.e(TAG, "History API 호출 실패", e)
            }

            // OpenAI 응답 발화
            onStateChange(RobotState.LISTEN)
            speakText(pendingResponse)
        } catch (e: Exception) {
            Log.e(TAG, "일반 쿼리 처리 실패", e)
            pendingResponse = "Sorry, I couldn't process your request"
            onStateChange(RobotState.LISTEN)
            speakText(pendingResponse)
        }
    }

    companion object {
        private const val TAG = "ConversationHandler"
    }
}