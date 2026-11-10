package com.kangrio.byd.assistant.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.kangrio.byd.assistant.service.ButtonTriggerService
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

object AccessibilityUtil {

    fun hasSecurePermission(context: Context) =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED

    fun isEnabled(context: Context): Boolean {
        val me = ComponentName(context, ButtonTriggerService::class.java)
        val list = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return list.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    /** Writes the service into/out of the enabled list. No UI fallback. */
    private fun writeEnabled(context: Context, enable: Boolean) {
        val me = ComponentName(context, ButtonTriggerService::class.java).flattenToString()
        val cr = context.contentResolver
        val current = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').filter { it.isNotBlank() }.toMutableList()

        if (enable) {
            if (current.none { it.equals(me, true) }) current.add(me)
        } else {
            current.removeAll { it.equals(me, true) }
        }

        Settings.Secure.putString(
            cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, current.joinToString(":")
        )
        Settings.Secure.putInt(
            cr, Settings.Secure.ACCESSIBILITY_ENABLED, if (current.isEmpty()) 0 else 1
        )
    }

    fun setEnabled(context: Context, enable: Boolean): Boolean {
        if (!hasSecurePermission(context)) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        writeEnabled(context, enable)
        return true
    }

    /**
     * Disable, wait, enable again so the system rebinds the service.
     * Returns false if WRITE_SECURE_SETTINGS isn't granted (no settings screen is opened,
     * since opening it makes no sense for a restart).
     */
    suspend fun reEnable(context: Context, delayMs: Long = 500): Boolean {
        if (!hasSecurePermission(context)) return false
        val app = context.applicationContext
        writeEnabled(app, false)
        delay(delayMs.milliseconds)
        writeEnabled(app, true)
        return true
    }
}