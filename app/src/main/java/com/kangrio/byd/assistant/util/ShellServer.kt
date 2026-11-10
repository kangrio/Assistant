package com.kangrio.byd.assistant.util

import android.os.IBinder
import android.util.Log
import com.kangrio.byd.assistant.App
import com.kangrio.shellserver.client.ShellServerHelper
import com.kangrio.shellserver.shared.ShellServerRunnable

object ShellServer {
    @Volatile
    private var binder: IBinder? = null

    fun isConnected(): Boolean {
        val connected = binder != null && binder?.isBinderAlive == true
        Log.d("ShellServer", "isConnected: $connected")
        return connected
    }

    fun exec(cmd: String) {
        Log.d("ShellServer", "exec: $cmd")
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

    fun runOnce(runnable: ShellServerRunnable, delay: Long) {
        if (isConnected()) {
            ShellServerHelper.runOnce(runnable, delay)
            return
        }

        ShellServerHelper.init(App.get()) {
            try {
                binder = it
                ShellServerHelper.runOnce(runnable, delay)
            } catch (e: Throwable) {
            }
        }
    }

    fun schedule(runnable: ShellServerRunnable, delay: Long, period: Long) {
        if (isConnected()) {
            ShellServerHelper.schedule(runnable, delay, period)
            return
        }

        ShellServerHelper.init(App.get()) {
            try {
                binder = it
                ShellServerHelper.schedule(runnable, delay, period)
            } catch (e: Throwable) {
            }
        }
    }
}