package com.kangrio.byd.assistant

object Constant {
    const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
    const val CHATGPT_APP_PACKAGE = "com.openai.chatgpt"
    const val CHATGPT_APP_ASSISTANT_CLASS_NAME = "com.openai.voice.assistant.AssistantActivity"
    const val PREFS_START_HOTWORD = "PREFS_START_HOTWORD"
    const val PREFS_HOTWORD_MODEL_NAME = "PREFS_HOTWORD_MODEL_NAME"
    const val PREFS_HOTWORD_SENSITIVITY = "PREFS_HOTWORD_SENSITIVITY"
    const val PREFS_MIC_AUDIO_SOURCE = "PREFS_MIC_AUDIO_SOURCE"
    const val PREFS_PLAY_DING_ON_START = "PREFS_PLAY_DING_ON_START"
    const val PREFS_ASSISTANT_PACKAGE_COMPONENT = "PREFS_ASSISTANT_PACKAGE_COMPONENT"
    const val PREFS_LAST_OTA_CHECK_TIME = "PREFS_LAST_OTA_CHECK_TIME"
    const val PREFS_LATEST_OTA_VERSION = "PREFS_LATEST_OTA_VERSION"
    const val PREFS_USE_BETA_UPDATE = "PREFS_USE_BETA_UPDATE"

    const val GITHUB_OTA_URL = "https://api.github.com/repos/kangrio/Assistant/releases/latest"
    const val OTA_CHECK_INTERVAL_MS = 3 * 60 * 60 * 1000L // 3 hours
    const val OTA_NOTIFICATION_CHANNEL_ID = "ota_updates"
    const val OTA_NOTIFICATION_ID = 2001
}