package com.example.birthday_reminder.data.model

/**
 * Data class untuk Notification Settings
 */
data class NotificationSettings(
    val enabled: Boolean = true,
    val daysAhead: List<Int> = listOf(0, 1, 3), // H-0, H-1, H-3 (default)
    val notificationTime: Int = 480
) {
    /**
     * Get hour dari notification time
     */
    fun getHour(): Int = notificationTime / 60

    /**
     * Get minute dari notification time
     */
    fun getMinute(): Int = notificationTime % 60

    /**
     * Get formatted time string (HH:mm)
     */
    fun getFormattedTime(): String {
        return String.format("%02d:%02d", getHour(), getMinute())
    }
}