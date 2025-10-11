package com.example.birthday_reminder.data.model

/**
 * Data class untuk Notification Settings
 */
data class NotificationSettings(
    val enabled: Boolean = true,
    val daysAhead: List<Int> = listOf(0, 1, 3), // H-0, H-1, H-3 (default)
    val notificationTime: Int = 8 // Jam 08:00 (default)
)