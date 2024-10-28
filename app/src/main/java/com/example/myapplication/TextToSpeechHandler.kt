package com.example.myapplication

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TextToSpeechHandler(
    context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "This language is not supported")
            }

            configureTTS()

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
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
            })
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    private fun configureTTS() {
        tts.apply {
            setSpeechRate(1.1f)
            setPitch(1.1f)

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageID")
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.8f)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                voices?.let { voices ->
                    val desiredVoice = voices.find {
                        it.name.contains("en-us", ignoreCase = true) &&
                                it.name.contains("female", ignoreCase = true)
                    }
                    desiredVoice?.let { setVoice(it) }
                }
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return

        handler.post {
            onSpeakingStateChanged(true)
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageID")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "messageID")
        }
    }

    fun shutdown() {
        tts.shutdown()
    }

    companion object {
        private const val TAG = "TextToSpeechHandler"
    }
}