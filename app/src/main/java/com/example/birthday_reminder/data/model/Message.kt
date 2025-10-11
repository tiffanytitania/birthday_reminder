package com.example.birthday_reminder.data.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class untuk Message/Ucapan
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val from: String, // username pengirim
    val to: String, // username penerima
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: MessageType = MessageType.GREETING
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

enum class MessageType {
    GREETING,      // Ucapan ulang tahun
    PERSONAL,      // Pesan pribadi
    ANNOUNCEMENT   // Pengumuman dari admin
}