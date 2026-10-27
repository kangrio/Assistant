package com.kangrio.byd.assistant.ota

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.kangrio.byd.assistant.Constant
import com.kangrio.byd.assistant.R
import com.kangrio.byd.assistant.activity.SettingsActivity
import com.kangrio.byd.assistant.data.ReleaseInfo
import com.kangrio.byd.assistant.util.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object OtaUpdater {
    private const val TAG = "OtaUpdater"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Assistant-OTA-Updater")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val gitHubApi: GitHubApi = retrofit.create(GitHubApi::class.java)

    /**
     * Checks whether an update check should be performed based on the 3-hour frequency interval.
     */
    fun shouldCheck(force: Boolean = false): Boolean {
        if (force) return true
        val lastCheck = Preferences.lastOtaCheckTime
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastCheck) >= Constant.OTA_CHECK_INTERVAL_MS
    }

    /**
     * Triggers a check in the background without blocking the caller.
     */
    fun checkUpdateInBackground(context: Context, force: Boolean = false) {
        if (!shouldCheck(force)) {
            Log.d(TAG, "Skipping OTA check, throttled by 3-hour frequency.")
            return
        }

        scope.launch {
            try {
                val releaseInfo = checkForUpdate(context, force = force, preRelease = Preferences.useBetaUpdate)
                if (releaseInfo != null) {
                    Log.i(TAG, "New OTA version available: ${releaseInfo.tagName}")
                    Preferences.latestOtaVersion = releaseInfo.tagName
                    showUpdateNotification(context, releaseInfo)
                } else {
                    Log.d(TAG, "Assistant is up-to-date.")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error checking OTA update in background", e)
            }
        }
    }

    /**
     * Performs a network check against the GitHub latest release API using Retrofit.
     * Returns [com.kangrio.byd.assistant.data.ReleaseInfo] if a newer version is available, or null otherwise.
     */
    suspend fun checkForUpdate(context: Context, force: Boolean = false, preRelease: Boolean = false): ReleaseInfo? = withContext(Dispatchers.IO) {
        if (!shouldCheck(force)) {
            return@withContext null
        }

        try {
            val allReleases = gitHubApi.getAllReleases(owner = "kangrio", repo = "Assistant")
            val release = allReleases.first {
                if (preRelease) {
                    it.prerelease
                } else {
                    !it.prerelease
                }
            }

            Preferences.lastOtaCheckTime = System.currentTimeMillis()

            val apkAsset = release.assets.firstOrNull { asset ->
                asset.name.endsWith(".apk", ignoreCase = true)
            } ?: run {
                Log.w(TAG, "No APK asset found in release ${release.tag_name}")
                return@withContext null
            }

            val currentVersionName = getCurrentVersionName(context)
            val cleanRemoteVersion = release.tag_name.removePrefix("v").removePrefix("V")

            if (isNewerVersion(cleanRemoteVersion, currentVersionName)) {
                return@withContext ReleaseInfo(
                    tagName = release.tag_name,
                    versionName = cleanRemoteVersion,
                    title = release.name ?: release.tag_name,
                    body = release.body ?: "",
                    htmlUrl = release.html_url,
                    downloadUrl = apkAsset.browser_download_url,
                    apkName = apkAsset.name,
                    size = apkAsset.size
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to check for OTA update via Retrofit", e)
        }

        return@withContext null
    }

    /**
     * Gets the current installed version name of the app.
     */
    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Compares semantic versions (e.g. "1.0.3" > "1.0.2").
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        val cleanRemote = remote.trim().removePrefix("v").removePrefix("V").replace("-beta", ".")
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V").replace("-beta", ".")

        val remoteBase = cleanRemote.split("-")[0]
        val currentBase = cleanCurrent.split("-")[0]

        val remoteParts = remoteBase.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentBase.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        // If base versions are equal, check pre-release suffix (release > beta)
        if (cleanRemote.contains("-") && !cleanCurrent.contains("-")) {
            return false
        }
        if (!cleanRemote.contains("-") && cleanCurrent.contains("-")) {
            return true
        }

        return false
    }

    /**
     * Creates notification channel for OTA update notifications.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constant.OTA_NOTIFICATION_CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Assistant app updates"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Displays a system notification prompting the user about the new version.
     */
    fun showUpdateNotification(context: Context, releaseInfo: ReleaseInfo) {
        createNotificationChannel(context)

        val intent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_OTA_VERSION", releaseInfo.tagName)
            putExtra("EXTRA_OTA_URL", releaseInfo.downloadUrl)
            putExtra("EXTRA_OTA_NAME", releaseInfo.apkName)
            putExtra("EXTRA_OTA_BODY", releaseInfo.body)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            Constant.OTA_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constant.OTA_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Update Available: ${releaseInfo.tagName}")
            .setContentText("New version ${releaseInfo.tagName} is available. Tap to update.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Version ${releaseInfo.tagName} is ready to install.\n\n${releaseInfo.body.take(200)}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(Constant.OTA_NOTIFICATION_ID, notification)
    }

    /**
     * Downloads an APK file to the app's cache directory with progress reporting.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (progress: Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        val downloadDir = File(context.cacheDir, "ota_updates").apply { mkdirs() }
        val targetFile = File(downloadDir, fileName)

        if (targetFile.exists()) {
            targetFile.delete()
        }

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Assistant-OTA-Updater")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Download failed with HTTP ${response.code}")
                    return@withContext null
                }

                val body = response.body ?: return@withContext null
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                onProgress(progress)
                            }
                        }
                        output.flush()
                    }
                }
                onProgress(1.0f)
                return@withContext targetFile
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error downloading APK", e)
            targetFile.delete()
            return@withContext null
        }
    }

    /**
     * Prompts system package installer to install the downloaded APK file.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start APK installation", e)
        }
    }
}
