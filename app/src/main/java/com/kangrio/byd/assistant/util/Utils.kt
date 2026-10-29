package com.kangrio.byd.assistant.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.SoundPool
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.util.Log
import android.widget.Toast
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.content.edit
import androidx.core.net.toUri
import com.kangrio.byd.assistant.Constant
import com.kangrio.byd.assistant.R
import com.kangrio.byd.assistant.StartActivity
import com.kangrio.byd.assistant.data.AssistantApp

object Utils {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .build()

    private var dingSound: Int = -1

    /**
     * Check if a permission is granted.
     */
    fun isGranted(context: Context, permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Set default voice assistant app to be used.
     */
    fun enableVoiceAssistant(context: Context, componentName: String? = null) {
        val componentName = componentName ?: return

        putSecureSetting(context, "assistant", null)
        putSecureSetting(context, "voice_interaction_service", null)
        Thread.sleep(100)
        putSecureSetting(context, "assistant", componentName)
        putSecureSetting(context, "voice_interaction_service", componentName)
    }

    /**
     * List all installed voice assistant apps with support for VoiceInteractionService.
     */
    fun listAssistantPackages(context: Context): List<AssistantApp> {
        val pm = context.packageManager

        return pm.queryIntentServices(
            Intent(VoiceInteractionService.SERVICE_INTERFACE),
            0
        ).mapNotNull { resolveInfo ->
            val serviceInfo = resolveInfo.serviceInfo ?: return@mapNotNull null

            AssistantApp(
                name = serviceInfo.loadLabel(pm).toString(),
                packageName = serviceInfo.packageName,
                className = serviceInfo.name,
            )
        }
    }

    /**
     * Check if a package has support for VoiceInteractionService.
     */
    fun isSupportAssistService(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val intent = Intent(VoiceInteractionService.SERVICE_INTERFACE)
        intent.setPackage(packageName)
        return pm.queryIntentServices(intent, 0).isNotEmpty()
    }

    /**
     * Get the currently selected voice assistant app.
     */
    fun getCurrentAssistantApp(context: Context): AssistantApp {
        val assistant = Settings.Secure.getString(context.contentResolver, "assistant") ?: return AssistantApp()

        val component = ComponentName.unflattenFromString(assistant) ?: return AssistantApp()
        val appInfo = context.packageManager.getPackageInfo(component.packageName, 0)
        val appLabel = appInfo.applicationInfo?.loadLabel(context.packageManager)?.toString()

        val assistantApp = AssistantApp(
            name = appLabel ?: "",
            packageName = component.packageName,
            className = component.className,
        )

        return assistantApp
    }

    /**
     * Check if voice assistant is enabled.
     */
    fun isEnabledVoiceAssistant(context: Context): Boolean {
        val assistant = Settings.Secure.getString(context.contentResolver, "assistant")
        val voiceInteractionService =
            Settings.Secure.getString(context.contentResolver, "voice_interaction_service")

        return !assistant.isNullOrEmpty() && !voiceInteractionService.isNullOrEmpty()
    }

    /**
     * Check if all required permissions are granted.
     */
    fun setupCompleted(context: Context): Boolean {
        val isGrantedWriteSecureSettings = !isDilink() || isGranted(context, Manifest.permission.WRITE_SECURE_SETTINGS)
        val isEnableVoiceAssistant = isEnabledVoiceAssistant(context)
        val isAssistantAppsInstalled = listAssistantPackages(context).isNotEmpty()
        val isAutoStart = isGrantedAutoStart(context)
        return isGrantedWriteSecureSettings && isEnableVoiceAssistant && isAssistantAppsInstalled && isAutoStart
    }

    /**
     * Play a ding sound when Voice Assistant app is launched.
     */
    private fun playDing(context: Context) {
        if (!Preferences.playDingOnStart) return

        if (dingSound == -1) {
            dingSound = soundPool.load(context, R.raw.ding, 1)
            soundPool.setOnLoadCompleteListener { pool, i, i1 ->
                soundPool.play(dingSound, 1f, 1f, 0, 0, 1f)
            }
        } else {
            soundPool.play(dingSound, 1f, 1f, 0, 0, 1f)
        }
    }

    /**
     * Launch the currently selected voice assistant app.
     */
    fun startVoiceAssistant(context: Context) {
        try {
            startVoiceAssistantViaAdb()
            return
        } catch (e: Throwable) {}

        try {
            // Redirect through StartActivity when called from a non-Activity context
            if (context !is Activity) {
                context.startActivity(
                    Intent(context, StartActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
                return
            }

            val assistantApp = getCurrentAssistantApp(context)
            val packageName = assistantApp.packageName
            val intent = Intent(Intent.ACTION_ASSIST).apply {
                if (packageName.isNotEmpty()) {
                    setPackage(packageName)
                }
            }

            when (packageName) {
                Constant.GOOGLE_APP_PACKAGE -> {
                    intent.action = Intent.ACTION_VOICE_COMMAND
                }

                Constant.CHATGPT_APP_PACKAGE -> {
                    intent.component = ComponentName(packageName, Constant.CHATGPT_APP_ASSISTANT_CLASS_NAME)
                    intent.putExtra("isAssistant", true)
                }
            }

            context.startActivity(intent)
            playDing(context)
        } catch (e: Throwable) {
            Log.e("Utils", "startVoiceAssistant", e)
        }
    }

    fun startVoiceAssistantViaAdb() {
        ShellServer.exec("input keyevent 219")
    }

    /**
     * Get the Notification Listener component name for a given package name.
     */
    fun getNotificationListenerComponentName(context: Context, packageName: String): ComponentName? {
        val intent = Intent("android.service.notification.NotificationListenerService").apply {
            setPackage(packageName)
        }
        val resolveInfos = context.packageManager.queryIntentServices(intent, 0)
        if (resolveInfos.isEmpty()) return null
        val serviceInfo = resolveInfos[0].serviceInfo
        return ComponentName(serviceInfo.packageName, serviceInfo.name)
    }

    /**
     * Check if the Notification Listener service is enabled for a given package name.
     */
    fun isNotificationListenerEnabled(context: Context, packageName: String): Boolean {
        val notificationListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val components = notificationListeners?.split(":") ?: emptyList()
        return components.any { ComponentName.unflattenFromString(it)?.packageName == packageName }
    }

    /**
     * Grant the Notification Listener service permission for a given package name via ADB.
     */
    suspend fun grantNotificationListener(context: Context, packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!isGranted(context, Manifest.permission.WRITE_SECURE_SETTINGS)) return@withContext false

            val componentName =
                getNotificationListenerComponentName(context, packageName)?.flattenToString()
                    ?: return@withContext false

            adbShell(context, "cmd notification allow_listener $componentName")
            return@withContext isNotificationListenerEnabled(context, packageName)
        }

    /**
     * Put a secure setting.
     */
    private fun putSecureSetting(context: Context, key: String, value: String?) {
        if (!isGranted(context, Manifest.permission.WRITE_SECURE_SETTINGS)) return

        Settings.Secure.putString(context.contentResolver, key, value)
    }

    /**
     * Run a shell command via ADB.
     */
    suspend fun adbShell(context: Context, cmd: String) =
        withContext(Dispatchers.IO) {
            setupUserHome(context)
            val result = withTimeoutOrNull(20_000L.milliseconds) {
                while (isActive) {
                    try {
                        var dAdb = Dadb.discover(connectTimeout = 2000, socketTimeout = 5000)
                        dAdb?.shell("echo 'init'") ?: throw Throwable("fail to connect adb")
                        dAdb.close()

                        dAdb = Dadb.discover(connectTimeout = 2000, socketTimeout = 5000)
                        dAdb?.shell(cmd)
                        dAdb?.close()
                        return@withTimeoutOrNull true
                    } catch (e: Throwable) {
                        delay(100L.milliseconds)
                    }
                }
                false
            }

            if (result != true) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "ADB connection failed. Please check ADB and try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    /**
     * Request a permission via ADB.
     */
    suspend fun adbRequestPermission(context: Context, permission: String) =
        withContext(Dispatchers.IO) {
            setupUserHome(context)
            adbShell(context, "pm grant ${context.packageName} $permission")
        }

    /**
     * setup user home when it is empty required by DAdb library.
     */
    private fun setupUserHome(context: Context) {
        val userHome = System.getProperty("user.home") ?: ""
        if (userHome.isEmpty()) {
            val userDir = context.filesDir.absolutePath
            System.setProperty("user.home", userDir)
        }
    }

    /**
     * Open the Google Play Store to install the app.
     */
    fun openStore(context: Context) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "market://".toUri()
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Throwable) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com".toUri()
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * Check if the device is a Dilink device.
     */
    fun isDilink(): Boolean {
        return listOf(
            Build.BRAND,
            Build.FINGERPRINT,
            Build.MODEL,
            Build.PRODUCT
        ).any { it.contains("dilink", ignoreCase = true) }
    }

    /**
     * Check if the app is granted auto-start permission.
     * for dilink device, use last update time instead
     */
    fun isGrantedAutoStart(context: Context): Boolean {
        if (!isDilink()) {
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return power.isIgnoringBatteryOptimizations(context.packageName)
        }

        val prefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        val currentUpdateTime = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        val lastUpdateTime = prefs.getLong("last_update_time", 0)
        return lastUpdateTime > currentUpdateTime
    }

    /**
     * Open the auto-start settings for the device.
     * for none dilink device, use ignore battery optimization instead
     */
    @SuppressLint("BatteryLife")
    fun openAutoStartSettings(context: Context) = runCatching {
        if (!isDilink()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
            return@runCatching
        }

        val intent = Intent("android.intent.action.BYD_APPSTARTMANAGEMENT")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Mark the last auto-start update time.
     */
    fun markAutoStartTime(context: Context) {
        val prefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        prefs.edit {
            putLong(
                "last_update_time",
                System.currentTimeMillis()
            )
        }
    }

    /**
     * Format byte count into human-readable size string.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val formatted = String.format(java.util.Locale.US, "%.1f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups]}"
    }
}