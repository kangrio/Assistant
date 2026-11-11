package com.kangrio.byd.assistant.util

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaRecorder
import android.view.KeyEvent
import com.kangrio.byd.assistant.Constant
import androidx.core.content.edit

object Preferences {
    lateinit var prefs: SharedPreferences
    const val KEYCODE_NONE = -1

    fun init(context: Context) {
        prefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    }

    val isInitialized get() = ::prefs.isInitialized

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(l)

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(l)

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

    var enabledButtonTrigger: Boolean
        get() = prefs.getBoolean(Constant.PREFS_ENABLED_BUTTON_TRIGGER, false)
        set(value) = prefs.edit { putBoolean(Constant.PREFS_ENABLED_BUTTON_TRIGGER, value) }

    var keyEventAction: Int
        get() = prefs.getInt(Constant.PREFS_BUTTON_TRIGGER_ACTION, KeyEvent.ACTION_UP)
        set(value) = prefs.edit { putInt(Constant.PREFS_BUTTON_TRIGGER_ACTION, value) }

    var triggerKeyCode: Int
        get() = prefs.getInt(Constant.PREFS_TRIGGER_KEYCODE, KEYCODE_NONE)
        set(value) = prefs.edit { putInt(Constant.PREFS_TRIGGER_KEYCODE, value) }

    var captureMode: Boolean
        get() = prefs.getBoolean(Constant.PREFS_CAPTURE_MODE, false)
        set(value) = prefs.edit { putBoolean(Constant.PREFS_CAPTURE_MODE, value) }

    var blockOriginalAction: Boolean
        get() = prefs.getBoolean(Constant.PREFS_BLOCK_ORIGINAL, true)
        set(value) = prefs.edit { putBoolean(Constant.PREFS_BLOCK_ORIGINAL, value) }
}