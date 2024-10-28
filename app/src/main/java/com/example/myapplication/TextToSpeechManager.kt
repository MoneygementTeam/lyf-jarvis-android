package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class TextToSpeechManager(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        try {
            if (tts == null) {
                tts = TextToSpeech(context.applicationContext, this).apply {
                    setOnUtteranceProgressListener(createUtteranceProgressListener())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS 초기화 실패: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                tts?.let { tts ->
                    val result = tts.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "언어가 지원되지 않습니다")
                        return
                    }
                    setupTTSEngine(tts)
                    isInitialized.set(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS 설정 실패: ${e.message}")
            }
        } else {
            Log.e(TAG, "TTS 초기화 실패: $status")
        }
    }

    private fun setupTTSEngine(tts: TextToSpeech) {
        try {
            tts.apply {
                setSpeechRate(1.1f)
                setPitch(1.1f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS 엔진 설정 실패: ${e.message}")
        }
    }

    private fun createUtteranceProgressListener() = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            handler.post {
                onSpeakingStateChanged(true)
            }
        }

        override fun onDone(utteranceId: String) {
            handler.post {
                onSpeakingStateChanged(false)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) {
            handler.post {
                onSpeakingStateChanged(false)
            }
        }
    }

    fun speak(text: String) {
        if (!isInitialized.get()) {
            Log.w(TAG, "TTS가 초기화되지 않았습니다")
            reinitializeTTS()
            return
        }

        try {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageID")
            }

            tts?.let { tts ->
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "messageID")
                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "음성 출력 실패")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "음성 출력 중 오류 발생: ${e.message}")
            onSpeakingStateChanged(false)
        }
    }

    private fun reinitializeTTS() {
        destroy()
        initializeTTS()
    }

    fun destroy() {
        try {
            tts?.let { tts ->
                tts.stop()
                tts.shutdown()
            }
            tts = null
            isInitialized.set(false)
        } catch (e: Exception) {
            Log.e(TAG, "TTS 정리 중 오류 발생: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}