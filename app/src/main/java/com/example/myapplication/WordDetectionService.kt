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
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.openai.application.OpenAiService
import java.util.Locale

class WordDetectionService: Service(), TextToSpeech.OnInitListener {

    private var intent: Intent? = null
    private var mRecognizer: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech
    private val specificWord = "영실아"
    private var audioManager: AudioManager? = null
    private var originalNotificationVolume: Int = 0
    private var isAnswer: Boolean = false
    private val openAiService: OpenAiService = OpenAiService()


    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalNotificationVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_NOTIFICATION)

        muteSystemSounds()

        intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent!!.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        intent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")

        createNotification()
        startSTT()

        tts = TextToSpeech(this, this)
    }

    private fun startSTT() {
        stopSTT()
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        mRecognizer!!.setRecognitionListener(listener)
        mRecognizer!!.startListening(intent)
    }

    private fun stopSTT() {
        if (mRecognizer != null) {
            mRecognizer!!.destroy()
            mRecognizer = null
        }
    }

    // 시스템 소리 및 알림음 음소거
    private fun muteSystemSounds() {
        audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        audioManager?.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
    }

    // 시스템 소리 및 알림음 복구
    private fun restoreSystemSounds() {
        audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
        audioManager?.setStreamVolume(AudioManager.STREAM_SYSTEM, originalNotificationVolume, 0)
    }

    private fun speakText(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private val listener: RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(bundle: Bundle) {

        }

        override fun onBeginningOfSpeech() {

        }

        override fun onRmsChanged(v: Float) {

        }

        override fun onBufferReceived(bytes: ByteArray) {

        }

        override fun onEndOfSpeech() {

        }

        override fun onError(i: Int) {
            val message: String
            message = when (i) {
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
            startSTT()
        }

        override fun onResults(results: Bundle) {
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

            if (matches != null) {
                if(matches[0].contains(specificWord)) {
                    speakText("여행 일정: 자갈치 시장 방문 후 해운대 해변 산책\n" +
                            "오전: 자갈치 시장 탐방\n" +
                            "09:00 - 자갈치 시장 도착\n" +
                            "신선한 해산물을 구경하며 부산의 수산 시장 문화를 체험합니다.\n" +
                            "시장 안의 식당에서 해산물 아침 식사 (회, 해산물탕 등)를 즐겨보세요.\n" +
                            "오후: 해운대 해변 산책\n" +
                            "12:00 - 해운대로 이동\n" +
                            "\n" +
                            "자갈치 시장에서 해운대 해변까지는 지하철로 약 45분 소요됩니다.\n" +
                            "택시 이용 시 약 30분 걸립니다.\n" +
                            "13:00 - 해운대 해변 도착\n" +
                            "\n" +
                            "해운대 해변에서 여유롭게 바다를 감상하며 산책을 즐겨보세요.\n" +
                            "날씨가 좋다면 모래사장에서 휴식을 취하거나 해변가의 카페에서 커피를 마실 수 있습니다.\n" +
                            "일정의 포인트: 해운대 해변에서의 여유\n" +
                            "해운대 해변은 부산의 대표적인 명소로, 탁 트인 바다와 모래사장을 느낄 수 있는 완벽한 장소입니다. 바다를 바라보며 산책하는 것만으로도 힐링이 될 것입니다.\n" +
                            "이 일정은 오전에 자갈치 시장을 체험하고, 오후에는 해운대에서 편안하게 산책하는 여유로운 여행으로 구성됩니다.")
                }
            }

            startSTT()
        }

        override fun onPartialResults(bundle: Bundle) {

        }

        override fun onEvent(i: Int, bundle: Bundle) {

        }
    }

    private fun createNotification() {
        val builder = NotificationCompat.Builder(this, "default")
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle("STT 변환")
        builder.setContentText("음성인식 중..")
        builder.color = Color.RED
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)

        // 알림 표시
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

    override fun onDestroy() {
        super.onDestroy()
        if (mRecognizer != null) {
            mRecognizer!!.stopListening()
            mRecognizer!!.destroy()
            mRecognizer = null
        }
        restoreSystemSounds()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            val result = tts.setLanguage(Locale.KOREAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "해당 언어는 지원되지 않습니다.")
            }
        } else {
            Log.e("TTS", "TTS 초기화 실패")
        }
    }

    companion object {
        private const val TAG = "ForegroundTag"

        private const val NOTI_ID = 1
    }

}