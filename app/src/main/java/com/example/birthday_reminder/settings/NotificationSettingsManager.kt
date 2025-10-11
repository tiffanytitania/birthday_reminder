package com.example.birthday_reminder.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.birthday_reminder.data.model.NotificationSettings

/**
 * Manager untuk handle Notification Settings
 */
object NotificationSettingsManager {
    private const val PREF_NAME = "notification_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_DAYS_AHEAD = "days_ahead"
    private const val KEY_NOTIFICATION_TIME = "notification_time"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Ambil settings saat ini
     */
    fun getSettings(): NotificationSettings {
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        val daysAheadString = prefs.getStringSet(KEY_DAYS_AHEAD, setOf("0", "1", "3")) ?: setOf("0", "1", "3")
        val daysAhead = daysAheadString.map { it.toInt() }.sorted()
        val notificationTime = prefs.getInt(KEY_NOTIFICATION_TIME, 8)

        return NotificationSettings(enabled, daysAhead, notificationTime)
    }

    /**
     * Simpan settings
     */
    fun saveSettings(settings: NotificationSettings) {
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, settings.enabled)
            putStringSet(KEY_DAYS_AHEAD, settings.daysAhead.map { it.toString() }.toSet())
            putInt(KEY_NOTIFICATION_TIME, settings.notificationTime)
            apply()
        }
    }

    /**
     * Update enabled/disabled
     */
    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * Cek apakah notifikasi enabled
     */
    fun isEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    /**
     * Update hari notifikasi (H-0, H-1, etc)
     */
    fun setDaysAhead(days: List<Int>) {
        prefs.edit().putStringSet(KEY_DAYS_AHEAD, days.map { it.toString() }.toSet()).apply()
    }

    /**
     * Update jam notifikasi
     */
    fun setNotificationTime(hour: Int) {
        prefs.edit().putInt(KEY_NOTIFICATION_TIME, hour).apply()
    }
}