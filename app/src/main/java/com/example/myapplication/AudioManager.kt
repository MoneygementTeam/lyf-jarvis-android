package com.example.myapplication

import android.content.Context
import android.media.AudioManager

class SystemAudioManager(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val originalNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)

    fun muteSystemSounds() {
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
    }

    fun restoreSystemSounds() {
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, originalNotificationVolume, 0)
    }
}