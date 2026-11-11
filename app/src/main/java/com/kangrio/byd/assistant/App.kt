package com.kangrio.byd.assistant

import android.app.Application
import android.content.Context
import com.kangrio.byd.assistant.ota.OtaUpdater
import com.kangrio.byd.assistant.service.VoiceWakeService
import com.kangrio.byd.assistant.util.AccessibilityUtil
import com.kangrio.byd.assistant.util.Preferences
import com.kangrio.byd.assistant.util.ShellServer
import com.kangrio.byd.assistant.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class App : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        @Volatile
        private var appContext: App? = null
        fun get(): App {
            return appContext!!
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        appContext = this
    }

    override fun onCreate() {
        super.onCreate()
        Preferences.init(this)
        OtaUpdater.createNotificationChannel(this)
        OtaUpdater.checkUpdateInBackground(this, force = false)
        VoiceWakeService.startService(this)
        scope.launch {
            delay(10000.milliseconds)
            if (Utils.setupCompleted(this@App)) {
                ShellServer.init {
                    println("ShellServer connected")
                }
            }
        }

        if (!Preferences.enabledButtonTrigger) return
        Preferences.captureMode = false
        scope.launch {
            AccessibilityUtil.reEnable(this@App, delayMs = 1000)
        }
    }
}