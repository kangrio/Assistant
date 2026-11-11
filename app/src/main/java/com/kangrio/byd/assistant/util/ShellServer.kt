package com.kangrio.byd.assistant.util

import android.os.IBinder
import android.util.Log
import com.kangrio.byd.assistant.App
import com.kangrio.byd.assistant.runnable.RestartAppRunnable
import com.kangrio.shellserver.client.ShellServerHelper
import com.kangrio.shellserver.client.WakeUpActivity
import com.kangrio.shellserver.shared.ShellServerRunnable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object ShellServer {
    @Volatile
    private var binder: IBinder? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isConnected(): Boolean {
        val connected = binder != null && binder?.isBinderAlive == true
        Log.d("ShellServer", "isConnected: $connected")
        return connected
    }

    fun init(onConnected: (IBinder) -> Unit) {
        if (isConnected()) {
            onConnected(binder!!)
            return
        }
        scope.launch {
            ShellServerHelper.init(App.get()) {
                binder = it
                try {
                    onConnected(binder!!)
                } catch (e: Throwable) {
                }
            }
        }
    }

    fun exec(cmd: String) {
        Log.d("ShellServer", "exec: $cmd")
        if (isConnected()) {
            ShellServerHelper.exec(cmd)
            return
        }

        init {
            ShellServerHelper.exec(cmd)
        }
    }

    fun runOnce(runnable: ShellServerRunnable, delay: Long) {
        if (isConnected()) {
            ShellServerHelper.runOnce(runnable, delay)
            return
        }

        init {
            ShellServerHelper.runOnce(runnable, delay)
        }
    }

    fun reStartApp() {
        val packageName = App.get().packageName
        val activityName = WakeUpActivity::class.java.name
        runOnce(RestartAppRunnable(packageName, activityName), 100)
    }

    fun schedule(runnable: ShellServerRunnable, delay: Long, period: Long) {
        if (isConnected()) {
            ShellServerHelper.schedule(runnable, delay, period)
            return
        }

        init {
            ShellServerHelper.schedule(runnable, delay, period)
        }
    }
}