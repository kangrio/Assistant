package com.kangrio.byd.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kangrio.byd.assistant.ota.OtaUpdater
import com.kangrio.byd.assistant.service.VoiceWakeService
import com.kangrio.byd.assistant.util.AccessibilityUtil
import com.kangrio.byd.assistant.util.Preferences
import com.kangrio.byd.assistant.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext

        runCatching { OtaUpdater.checkUpdateInBackground(app, force = false) }
        runCatching { Utils.enableVoiceAssistant(app, Preferences.assistantPackageComponent) }
        runCatching { VoiceWakeService.startService(app) }

        if (!Preferences.enabledButtonTrigger) return
        Preferences.captureMode = false
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AccessibilityUtil.reEnable(app, delayMs = 1000)
            } finally {
                pending.finish()
            }
        }
    }
}