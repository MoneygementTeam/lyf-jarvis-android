package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.openai.application.OpenAiService
import java.util.Locale

enum class ConversationState {
    IDLE,
    GREETING,
    LISTENING,
    RESPONDING
}

class WordDetectionService: Service(), TextToSpeech.OnInitListener {
    private var intent: Intent? = null
    private var mRecognizer: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech
    private val specificWord = "life Jarvis"
    private var audioManager: AudioManager? = null
    private var originalNotificationVolume: Int = 0
    private var isAnswer: Boolean = false
    private val openAiService: OpenAiService = OpenAiService()
    private var retryCount: Int = 0
    private val maxRetries: Int = 3

    private var currentState = ConversationState.IDLE
    private var lastUserQuery: String = ""
    private var isSpeaking = false
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    private fun muteSystemSounds() {
        audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        audioManager?.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
    }

    private fun restoreSystemSounds() {
        audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
        audioManager?.setStreamVolume(AudioManager.STREAM_SYSTEM, originalNotificationVolume, 0)
    }

    private fun handleConversation(recognizedText: String) {
        when (currentState) {
            ConversationState.IDLE -> {
                if (recognizedText.lowercase().contains(specificWord.lowercase())) {
                    currentState = ConversationState.GREETING
                    speakText("Hello! How can I help you today?")
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

        when {
            query.lowercase().contains("anything fun") -> {
                speakText("Yes! Here are some fun activities I recommend: " +
                        "1. Visit the Busan Cinema Center for a movie " +
                        "2. Take a scenic cable car ride at Songdo Beach " +
                        "3. Explore the colorful Gamcheon Culture Village " +
                        "Would you like to know more about any of these?")
            }
            query.lowercase().contains("thank") -> {
                speakText("You're welcome! Let me know if you need anything else.")
                currentState = ConversationState.IDLE
            }
            else -> {
                speakText("I didn't quite catch that. Could you please repeat your question?")
            }
        }

        currentState = ConversationState.LISTENING
    }

    private fun configureTTS() {
        tts.apply {
            // 기본 설정
            setSpeechRate(1.1f)  // 20% 더 빠르게
            setPitch(1.1f)       // 기본 피치

            // 음성 품질 설정
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageID")
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.8f)
                }
            }

            // 음성 엔진 설정 (Android 5.0 이상)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                voices?.let { voices ->
                    // 원하는 음성 찾기 (예: 여성 음성)
                    val desiredVoice = voices.find {
                        it.name.contains("en-us", ignoreCase = true) &&
                                it.name.contains("female", ignoreCase = true)
                    }
                    desiredVoice?.let { setVoice(it) }
                }
            }
        }
    }

    private fun createNotification() {
        val builder = NotificationCompat.Builder(this, "default")
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle("STT Conversion")
        builder.setContentText("Voice Recognition in Progress..")
        builder.color = Color.RED
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)

        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    "default",
                    "기본 채널",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        notificationManager.notify(NOTI_ID, builder.build())
        val notification = builder.build()
        startForeground(NOTI_ID, notification)
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalNotificationVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        muteSystemSounds()

        intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        createNotification()

        handler.post {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(listener)
            }
            startListening()
        }

        tts = TextToSpeech(this, this)
    }

    private fun startListening() {
        if (!isListening && !isSpeaking && mRecognizer != null) {
            try {
                isListening = true
                mRecognizer?.startListening(intent)
                Log.d(TAG, "음성 인식 시작")
            } catch (e: Exception) {
                Log.e(TAG, "음성 인식 시작 실패: ${e.message}")
                isListening = false
                retryRecognition()
            }
        }
    }

    private fun stopListening() {
        try {
            isListening = false
            mRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 중지 실패: ${e.message}")
        }
    }

    private fun retryRecognition() {
        if (retryCount < maxRetries && !isSpeaking) {
            retryCount++
            Log.d(TAG, "음성 인식 재시도 $retryCount/$maxRetries")

            handler.post {
                try {
                    mRecognizer?.cancel()
                    startListening()
                } catch (e: Exception) {
                    Log.e(TAG, "재시도 실패: ${e.message}")
                    isListening = false
                    startListening()
                }
            }
        } else {
            retryCount = 0
            isListening = false
            startListening()
        }
    }

    private fun speakText(text: String) {
        handler.post {
            isSpeaking = true
            stopListening()

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageID")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "messageID")
        }
    }

    private val listener: RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(bundle: Bundle) {
            retryCount = 0
            Log.d(TAG, "음성 인식 준비 완료")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "음성 인식 시작됨")
        }

        override fun onRmsChanged(v: Float) {}

        override fun onBufferReceived(bytes: ByteArray) {
            Log.d(TAG, "음성 버퍼 수신됨")
        }

        override fun onEndOfSpeech() {
            isListening = false
            Log.d(TAG, "음성 인식 종료")
        }

        override fun onError(i: Int) {
            isListening = false
            val message: String = when (i) {
                SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "퍼미션 없음"
                SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트웍 타임아웃"
                SpeechRecognizer.ERROR_NO_MATCH -> "찾을 수 없음"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RECOGNIZER 가 바쁨"
                SpeechRecognizer.ERROR_SERVER -> "서버 에러"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "시간초과"
                else -> "알 수 없는 오류"
            }
            Log.d(TAG, "[$message] 에러 발생")
            if (!isSpeaking) {
                retryRecognition()
            }
        }

        override fun onResults(results: Bundle) {
            isListening = false
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

            matches?.forEachIndexed { index, text ->
                Log.d(TAG, "인식된 텍스트 $index: $text")
            }

            if (matches != null && matches.isNotEmpty()) {
                handleConversation(matches[0])
            }

            if (!isSpeaking) {
                startListening()
            }
        }

        override fun onPartialResults(bundle: Bundle) {
            val partialMatches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            partialMatches?.forEach { text ->
                Log.d(TAG, "부분 인식 결과: $text")
            }
        }

        override fun onEvent(i: Int, bundle: Bundle) {
            Log.d(TAG, "이벤트 발생: $i")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This language is not supported")
            }

            // TTS 설정 적용
            configureTTS()

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    handler.post {
                        isSpeaking = true
                        stopListening()
                    }
                }

                override fun onDone(utteranceId: String) {
                    handler.post {
                        isSpeaking = false
                        startListening()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String) {
                    handler.post {
                        isSpeaking = false
                        startListening()
                    }
                }
            })
        } else {
            Log.e("TTS", "TTS initialization failed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        if (mRecognizer != null) {
            mRecognizer!!.destroy()
            mRecognizer = null
        }
        restoreSystemSounds()
    }

    companion object {
        private const val TAG = "VoiceRecognition"
        private const val NOTI_ID = 1
    }
}