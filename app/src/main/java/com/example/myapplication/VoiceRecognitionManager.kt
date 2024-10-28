package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class VoiceRecognitionManager(
    private val context: Context,
    private val onRecognitionResult: (String) -> Unit,
    private val onStateChanged: (Boolean) -> Unit
) {
    private var mRecognizer: SpeechRecognizer? = null
    private var isListening = AtomicBoolean(false)
    private var retryCount = 0
    private val maxRetries = 3
    private val handler = Handler(Looper.getMainLooper())
    private var isInitialized = AtomicBoolean(false)

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
    }

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        handler.post {
            try {
                if (mRecognizer == null) {
                    mRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                        setRecognitionListener(createListener())
                    }
                    isInitialized.set(true)
                    Log.d(TAG, "음성 인식 초기화 성공")
                }
            } catch (e: Exception) {
                Log.e(TAG, "음성 인식 초기화 실패: ${e.message}")
                isInitialized.set(false)
                reinitializeRecognizer()
            }
        }
    }

    private fun reinitializeRecognizer() {
        handler.postDelayed({
            destroy()
            initializeRecognizer()
        }, 1000)
    }

    fun startListening() {
        if (!isInitialized.get()) {
            Log.w(TAG, "음성 인식이 초기화되지 않음, 재초기화 시도")
            reinitializeRecognizer()
            return
        }

        if (!isListening.get() && mRecognizer != null) {
            try {
                mRecognizer?.startListening(intent)
                isListening.set(true)
                onStateChanged(true)
                Log.d(TAG, "음성 인식 시작")
            } catch (e: Exception) {
                Log.e(TAG, "음성 인식 시작 실패: ${e.message}")
                isListening.set(false)
                onStateChanged(false)
                retryRecognition()
            }
        }
    }

    fun stopListening() {
        if (isListening.get()) {
            try {
                mRecognizer?.stopListening()
                isListening.set(false)
                onStateChanged(false)
                Log.d(TAG, "음성 인식 중지")
            } catch (e: Exception) {
                Log.e(TAG, "음성 인식 중지 실패: ${e.message}")
            }
        }
    }

    private fun retryRecognition() {
        if (retryCount < maxRetries) {
            retryCount++
            Log.d(TAG, "음성 인식 재시도 $retryCount/$maxRetries")

            handler.postDelayed({
                try {
                    mRecognizer?.cancel()
                    startListening()
                } catch (e: Exception) {
                    Log.e(TAG, "재시도 실패: ${e.message}")
                    isListening.set(false)
                    onStateChanged(false)
                    if (retryCount < maxRetries) {
                        retryRecognition()
                    }
                }
            }, 1000)
        } else {
            Log.d(TAG, "최대 재시도 횟수 도달, 재초기화 시도")
            retryCount = 0
            isListening.set(false)
            onStateChanged(false)
            reinitializeRecognizer()
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(bundle: Bundle) {
            retryCount = 0
            Log.d(TAG, "음성 인식 준비 완료")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "음성 인식 시작됨")
        }

        override fun onRmsChanged(v: Float) {
            // Log.v(TAG, "음성 레벨: $v")
        }

        override fun onBufferReceived(bytes: ByteArray) {
            Log.d(TAG, "음성 버퍼 수신됨")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "음성 인식 종료")
            isListening.set(false)
            onStateChanged(false)
        }

        override fun onError(i: Int) {
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

            isListening.set(false)
            onStateChanged(false)

            when (i) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // 일반적인 타임아웃은 바로 다시 시작
                    startListening()
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    // busy 에러는 cancel 후 재시작
                    handler.postDelayed({
                        mRecognizer?.cancel()
                        startListening()
                    }, 100)
                }
                else -> retryRecognition()
            }
        }

        override fun onResults(results: Bundle) {
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            isListening.set(false)
            onStateChanged(false)

            matches?.forEachIndexed { index, text ->
                Log.d(TAG, "인식된 텍스트 $index: $text")
            }

            if (!matches.isNullOrEmpty()) {
                onRecognitionResult(matches[0])
            }

            // 결과 처리 후 자동으로 다시 시작
            startListening()
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

    fun destroy() {
        try {
            stopListening()
            mRecognizer?.destroy()
            mRecognizer = null
            isInitialized.set(false)
            isListening.set(false)
            Log.d(TAG, "음성 인식 매니저 정리 완료")
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 매니저 정리 중 오류: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "VoiceRecognitionManager"
    }
}