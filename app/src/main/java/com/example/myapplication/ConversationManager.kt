package com.example.myapplication

import OpenAiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConversationManager(
    private val openAiService: OpenAiService,
    private val onResponseReady: (String) -> Unit
) {
    private var currentState = ConversationState.IDLE
    private val specificWord = "Jarvis"
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    fun handleInput(recognizedText: String) {
        when (currentState) {
            ConversationState.IDLE -> {
                if (recognizedText.lowercase().contains(specificWord.lowercase())) {
                    currentState = ConversationState.GREETING
                    onResponseReady("Hello! How is it going?")
                }
            }
            ConversationState.GREETING -> {
                currentState = ConversationState.LISTENING
                handleUserQuery(recognizedText)
            }
            ConversationState.LISTENING -> {
                handleUserQuery(recognizedText)
            }
            ConversationState.RESPONDING -> {
                // Ignore input while responding
            }
        }
    }

    private fun handleUserQuery(query: String) {
        currentState = ConversationState.RESPONDING
        serviceScope.launch {
            try {
                val result = openAiService.call(query)
                onResponseReady(result)
            } catch (e: Exception) {
                onResponseReady("Sorry, I couldn't process your request")
            }
            currentState = ConversationState.LISTENING
        }
    }

    fun destroy() {
        serviceScope.cancel()
    }
}