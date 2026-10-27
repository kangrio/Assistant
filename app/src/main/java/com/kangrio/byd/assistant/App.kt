package com.kangrio.byd.assistant

import android.app.Application
import com.kangrio.byd.assistant.ota.OtaUpdater
import com.kangrio.byd.assistant.service.VoiceWakeService
import com.kangrio.byd.assistant.util.Preferences

class App : Application() {
    companion object {
        @Volatile
        private var appContext: App? = null
        fun get(): App {
            return appContext!!
        }
    }
    override fun onCreate() {
        super.onCreate()
        appContext = this
        Preferences.init(this)
        OtaUpdater.createNotificationChannel(this)
        OtaUpdater.checkUpdateInBackground(this, force = false)
        VoiceWakeService.startService(this)
    }
}