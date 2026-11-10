package com.kangrio.byd.assistant.runnable

import android.util.Log
import com.kangrio.shellserver.shared.BaseShellServerRunnable

class RestartAppRunnable(val packageName: String, val activityName: String) : BaseShellServerRunnable() {
    override fun run() {
        try {
            Runtime.getRuntime().exec("am force-stop $packageName")
            Thread.sleep(1000)
            Runtime.getRuntime().exec("am start -n $packageName/$activityName")
        } catch (e: Exception) {
            Log.e("KillAppRunnable", "Failed to kill package: $packageName", e)
        }
    }
}