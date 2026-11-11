package com.kangrio.byd.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kangrio.byd.assistant.ota.OtaUpdater
import com.kangrio.byd.assistant.service.VoiceWakeService
import com.kangrio.byd.assistant.util.Preferences
import com.kangrio.byd.assistant.util.Utils

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext

        runCatching { OtaUpdater.checkUpdateInBackground(app, force = false) }
        runCatching { Utils.enableVoiceAssistant(app, Preferences.assistantPackageComponent) }
        runCatching { VoiceWakeService.startService(app) }
    }
}