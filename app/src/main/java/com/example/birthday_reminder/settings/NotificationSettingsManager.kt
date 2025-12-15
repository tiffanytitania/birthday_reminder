package com.example.birthday_reminder.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.birthday_reminder.data.model.NotificationSettings

/**
 * Manager untuk handle Notification Settings
 * Sekarang support pengaturan menit (misal: 7:19, 14:30, dll)
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
    fun setNotificationTime(totalMinutes: Int) {
        // Validasi range (0-1439 menit = 00:00 - 23:59)
        val validMinutes = totalMinutes.coerceIn(0, 1439)
        prefs.edit().putInt(KEY_NOTIFICATION_TIME, validMinutes).apply()

        android.util.Log.d("NotificationSettings", "⏰ Notification time set to: ${validMinutes / 60}:${String.format("%02d", validMinutes % 60)}")
    }

    /**
     * Set notification time dari hour dan minute terpisah
     */
    fun setNotificationTime(hour: Int, minute: Int) {
        val totalMinutes = (hour * 60) + minute
        setNotificationTime(totalMinutes)
    }

    /**
     * Get notification hour
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

    /**
     * Get formatted time string (HH:mm)
     */
    fun getFormattedTime(): String {
        val hour = getNotificationHour()
        val minute = getNotificationMinute()
        return String.format("%02d:%02d", hour, minute)
    }
}