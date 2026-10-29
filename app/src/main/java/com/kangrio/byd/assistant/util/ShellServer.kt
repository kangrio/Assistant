package com.kangrio.byd.assistant.util

import android.os.IBinder
import android.util.Log
import com.kangrio.byd.assistant.App
import com.kangrio.shellserver.client.ShellServerHelper

object ShellServer {
    @Volatile
    private var binder: IBinder? = null

    fun isConnected(): Boolean {
        val connected = binder != null
        Log.d("ShellServer", "isConnected: $connected")
        return connected
    }

    fun exec(cmd: String) {
        if (isConnected()) {
            ShellServerHelper.exec(cmd)
            return
        }

        ShellServerHelper.init(App.get()) {
            try {
                binder = it
                ShellServerHelper.exec(cmd)
            } catch (e: Throwable) {
            }
        }
    }
}