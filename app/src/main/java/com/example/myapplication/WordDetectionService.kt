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
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class WordDetectionService : Service() {
    private var intent: Intent? = null
    private var mRecognizer: SpeechRecognizer? = null
    private var audioManager: AudioManager? = null
    private var originalNotificationVolume: Int = 0
    private var retryCount: Int = 0
    private val maxRetries: Int = 3

    private var isSpeaking = false
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var conversationHandler: ConversationHandler
    private lateinit var ttsHandler: TextToSpeechHandler

    private var isServiceActive = false  // 서비스 활성화 상태 추적
    private var currentRobotState = RobotState.IDLE

    override fun onCreate() {
        super.onCreate()
        setupAudio()
        setupSpeechRecognition()
        createNotification()
        isServiceActive = true

        ttsHandler = TextToSpeechHandler(
            context = this,
            onSpeakingStateChanged = { speaking ->
                isSpeaking = speaking
                if (speaking) {
                    stopListening()
                    updateRobotState(RobotState.GO)
                } else {
                    startListening()
                    if (isServiceActive) {
                        updateRobotState(RobotState.LISTEN)
                    }
                }
            }
        )

        conversationHandler = ConversationHandler(
            speakText = { text -> ttsHandler.speak(text) },
            serviceScope = serviceScope,
            onStateChange = { newState ->
                updateRobotState(newState)
            }
        )

        // 서비스 시작 시 LISTEN 상태로 설정
        updateRobotState(RobotState.LISTEN)
    }

    private fun updateRobotState(newState: RobotState) {
        currentRobotState = newState
        SharedState.updateRobotState(newState)
        Log.d("RobotState", "State changed to: $newState")
    }

    private fun setupAudio() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalNotificationVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        muteSystemSounds()
    }

    private fun setupSpeechRecognition() {
        intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        handler.post {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(listener)
            }
            startListening()
        }
    }

    private fun muteSystemSounds() {
        audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        audioManager?.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
    }

    private fun restoreSystemSounds() {
        audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
        audioManager?.setStreamVolume(AudioManager.STREAM_SYSTEM, originalNotificationVolume, 0)
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
                conversationHandler.handleConversation(matches[0])
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceActive = true
        if (!isListening && !isSpeaking) {
            updateRobotState(RobotState.LISTEN)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceActive = false
        isListening = false
        mRecognizer?.destroy()
        mRecognizer = null
        ttsHandler.shutdown()
        restoreSystemSounds()
        serviceScope.cancel()

        // 서비스 종료 시 IDLE 상태로만 변경
        updateRobotState(RobotState.IDLE)
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    companion object {
        private const val TAG = "WordDetectionService"
        private const val NOTI_ID = 1
    }
}