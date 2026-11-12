package com.kangrio.byd.assistant.service


import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.kangrio.byd.assistant.util.Preferences
import com.kangrio.byd.assistant.util.Utils

@SuppressLint("AccessibilityPolicy")
class ButtonTriggerService : AccessibilityService() {



    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("ButtonTriggerService", "onServiceConnected")
        if (!Preferences.isInitialized) Preferences.init(applicationContext)
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        isServiceRunning = true
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Capture mode: save whatever is pressed and block it
        if (Preferences.captureMode) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val list = Preferences.triggerKeyCodes
                if (event.keyCode !in list) Preferences.triggerKeyCodes = list + event.keyCode
            }
            return true
        }

        val codes = Preferences.triggerKeyCodes
        if (event.keyCode !in codes) return false

        // Only the first keycode triggers; the others are just blocked
        if (event.keyCode == codes.first() &&
            event.action == Preferences.keyEventAction &&
            event.repeatCount == 0
        ) {
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