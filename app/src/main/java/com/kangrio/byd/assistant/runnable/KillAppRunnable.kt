package com.kangrio.byd.assistant.runnable

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.kangrio.shellserver.shared.BaseShellServerRunnable

class KillAppRunnable(private val packageName: String) : BaseShellServerRunnable() {
    override fun run() {
        val context = serverContext ?: return
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        try {
            val method = am.javaClass.getMethod("forceStopPackage", String::class.java)
            method.invoke(am, packageName)
        } catch (e: Exception) {
            Log.e("KillAppRunnable", "Failed to kill package: $packageName", e)
        }
    }
}