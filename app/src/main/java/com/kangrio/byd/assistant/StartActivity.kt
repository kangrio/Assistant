package com.kangrio.byd.assistant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.kangrio.byd.assistant.util.Utils
import com.kangrio.byd.assistant.activity.PermissionOnboardingActivity
import com.kangrio.byd.assistant.service.VoiceWakeService

class StartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VoiceWakeService.startService(this)
    }

    override fun onResume() {
        super.onResume()
        if (Utils.setupCompleted(this)) {
            Utils.startVoiceAssistant(this)
        } else {
            startActivity(Intent(this, PermissionOnboardingActivity::class.java))
        }
        finish()
    }
}