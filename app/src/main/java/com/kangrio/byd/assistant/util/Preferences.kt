package com.kangrio.byd.assistant.util

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaRecorder
import com.kangrio.byd.assistant.Constant
import androidx.core.content.edit

object Preferences {
    lateinit var prefs: SharedPreferences
    fun init(context: Context) {
        prefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    }

    var assistantPackageComponent: String
        get() = prefs.getString(Constant.PREFS_ASSISTANT_PACKAGE_COMPONENT, "") ?: ""
        set(value) = prefs.edit { putString(Constant.PREFS_ASSISTANT_PACKAGE_COMPONENT, value) }

    var startHotword: Boolean
        get() = prefs.getBoolean(Constant.PREFS_START_HOTWORD, false)
        set(value) = prefs.edit { putBoolean(Constant.PREFS_START_HOTWORD, value) }

    var hotwordModelName: String
        get() = prefs.getString(Constant.PREFS_HOTWORD_MODEL_NAME, "hey_billy") ?: "hey_billy"
        set(value) = prefs.edit { putString(Constant.PREFS_HOTWORD_MODEL_NAME, value) }

    var hotwordSensitivity: Float
        get() = prefs.getFloat(Constant.PREFS_HOTWORD_SENSITIVITY, 0.5f)
        set(value) = prefs.edit { putFloat(Constant.PREFS_HOTWORD_SENSITIVITY, value) }

    var micAudioSource: Int
        get() = prefs.getInt(Constant.PREFS_MIC_AUDIO_SOURCE, MediaRecorder.AudioSource.DEFAULT)
        set(value) = prefs.edit { putInt(Constant.PREFS_MIC_AUDIO_SOURCE, value) }

    var playDingOnStart: Boolean
        get() = prefs.getBoolean(Constant.PREFS_PLAY_DING_ON_START, true)
        set(value) = prefs.edit { putBoolean(Constant.PREFS_PLAY_DING_ON_START, value) }

    var lastOtaCheckTime: Long
        get() = prefs.getLong(Constant.PREFS_LAST_OTA_CHECK_TIME, 0L)
        set(value) = prefs.edit { putLong(Constant.PREFS_LAST_OTA_CHECK_TIME, value) }

    var latestOtaVersion: String
        get() = prefs.getString(Constant.PREFS_LATEST_OTA_VERSION, "") ?: ""
        set(value) = prefs.edit { putString(Constant.PREFS_LATEST_OTA_VERSION, value) }

    var useBetaUpdate: Boolean
        get() = prefs.getBoolean(Constant.PREFS_USE_BETA_UPDATE, false)
        set(value) = prefs.edit { putBoolean(Constant.PREFS_USE_BETA_UPDATE, value) }
}