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
    private const val KEY_NOTIFICATION_TIME = "notification_time" // dalam menit total (0-1439)

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

        // Default: 8:00 AM = 8 * 60 = 480 menit
        val notificationTime = prefs.getInt(KEY_NOTIFICATION_TIME, 480)

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
     * Update jam notifikasi (dalam menit total, 0-1439)
     * Contoh: 08:00 = 480 menit, 14:30 = 870 menit
     */
    fun setNotificationTime(totalMinutes: Int) {
        prefs.edit().putInt(KEY_NOTIFICATION_TIME, totalMinutes).apply()
    }

    /**
     * Get notification hour (untuk backward compatibility)
     */
    fun getNotificationHour(): Int {
        val totalMinutes = prefs.getInt(KEY_NOTIFICATION_TIME, 480)
        return totalMinutes / 60
    }

    /**
     * Get notification minute
     */
    fun getNotificationMinute(): Int {
        val totalMinutes = prefs.getInt(KEY_NOTIFICATION_TIME, 480)
        return totalMinutes % 60
    }
}