package com.kangrio.byd.assistant.service


import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.kangrio.byd.assistant.Constant
import com.kangrio.byd.assistant.util.Preferences
import com.kangrio.byd.assistant.util.Utils

@SuppressLint("AccessibilityPolicy")
class ButtonTriggerService : AccessibilityService() {

    @Volatile
    private var triggerKeyCode = Preferences.KEYCODE_NONE

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Constant.PREFS_TRIGGER_KEYCODE) {
                triggerKeyCode = Preferences.triggerKeyCode
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("ButtonTriggerService", "onServiceConnected")
        if (!Preferences.isInitialized) Preferences.init(applicationContext)
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        isServiceRunning = true

        triggerKeyCode = Preferences.triggerKeyCode
        Preferences.registerListener(prefsListener)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Capture mode: save whatever is pressed and block it
        if (Preferences.captureMode) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                Preferences.triggerKeyCode = event.keyCode
                Preferences.captureMode = false
            }
            return true
        }

        val target = triggerKeyCode
        if (target == Preferences.KEYCODE_NONE || event.keyCode != target) return false

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            onTriggered()
        }

        // true = block the original action (DOWN, repeats and UP); false = let it through too
        return Preferences.blockOriginalAction
    }

    private fun onTriggered() {
        Utils.startVoiceAssistant(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d("ButtonTriggerService", "onAccessibilityEvent")
    }
    override fun onInterrupt() {
        Log.d("ButtonTriggerService", "onInterrupt")
    }

    override fun onDestroy() {
        Log.d("ButtonTriggerService", "onDestroy")
        if (Preferences.isInitialized) Preferences.unregisterListener(prefsListener)
        isServiceRunning = false
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("ButtonTriggerService", "onUnbind")
        isServiceRunning = false
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile
        var isServiceRunning = false
    }
}