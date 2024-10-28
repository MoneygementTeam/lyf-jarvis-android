package com.example.myapplication

import OpenAiService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class ConversationState {
    IDLE,
    GREETING,
    LISTENING,
    RESPONDING
}

class ConversationHandler(
    private val speakText: (String) -> Unit,
    private val serviceScope: CoroutineScope
) {
    private var currentState = ConversationState.IDLE
    private var lastUserQuery: String = ""
    private var pendingResponse: String = ""
    private val specificWord = "Jarvis"
    private val openAiService: OpenAiService = OpenAiService()

    fun handleConversation(recognizedText: String) {
        when (currentState) {
            ConversationState.IDLE -> {
                if (recognizedText.lowercase().contains(specificWord.lowercase())) {
                    currentState = ConversationState.GREETING
                    speakText("Hello! How is it going?")
                }
            }
            ConversationState.GREETING -> {
                lastUserQuery = recognizedText
                currentState = ConversationState.LISTENING
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

        serviceScope.launch {
            try {
                val result = openAiService.call(query)
                pendingResponse = result
                speakText(pendingResponse)
            } catch (e: Exception) {
                Log.e("ConversationHandler", "에러: ${e.message}")
                pendingResponse = "Sorry, I couldn't process your request"
                speakText(pendingResponse)
            }
        }
        currentState = ConversationState.LISTENING
    }
}