package com.example.birthday_reminder.messaging

import android.content.Context
import com.example.birthday_reminder.data.model.Message
import com.example.birthday_reminder.data.model.MessageType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MessageManager {
    private const val PREF_NAME = "messages_prefs"
    private const val KEY_MESSAGES = "all_messages"

    private lateinit var context: Context
    private val gson = Gson()

    fun init(context: Context) {
        this.context = context
    }

    /**
     * Kirim pesan/ucapan
     */
    fun sendMessage(from: String, to: String, message: String, type: MessageType = MessageType.GREETING): Boolean {
        return try {
            val newMessage = Message(
                from = from,
                to = to,
                message = message,
                type = type
            )

            val messages = getAllMessages().toMutableList()
            messages.add(newMessage)
            saveMessages(messages)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Ambil semua pesan yang diterima user
     */
    fun getReceivedMessages(username: String): List<Message> {
        return getAllMessages().filter { it.to == username }
            .sortedByDescending { it.timestamp }
    }

    /**
     * Ambil semua pesan yang dikirim user
     */
    fun getSentMessages(username: String): List<Message> {
        return getAllMessages().filter { it.from == username }
            .sortedByDescending { it.timestamp }
    }

    /**
     * Ambil pesan yang belum dibaca
     */
    fun getUnreadMessages(username: String): List<Message> {
        return getReceivedMessages(username).filter { !it.isRead }
    }

    /**
     * Hitung jumlah pesan belum dibaca
     */
    fun getUnreadCount(username: String): Int {
        return getUnreadMessages(username).size
    }

    /**
     * Tandai pesan sebagai sudah dibaca
     */
    fun markAsRead(messageId: String) {
        val messages = getAllMessages().toMutableList()
        val index = messages.indexOfFirst { it.id == messageId }

        if (index != -1) {
            messages[index] = messages[index].copy(isRead = true)
            saveMessages(messages)
        }
    }

    /**
     * Hapus pesan
     */
    fun deleteMessage(messageId: String): Boolean {
        return try {
            val messages = getAllMessages().toMutableList()
            messages.removeIf { it.id == messageId }
            saveMessages(messages)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ambil semua pesan (internal)
     */
    private fun getAllMessages(): List<Message> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MESSAGES, "[]") ?: "[]"
        val type = object : TypeToken<List<Message>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     * Simpan messages ke SharedPreferences
     */
    private fun saveMessages(messages: List<Message>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(messages)
        prefs.edit().putString(KEY_MESSAGES, json).apply()
    }

    /**
     * Template ucapan ulang tahun
     */
    fun getGreetingTemplates(): List<String> {
        return listOf(
            "🎉 Selamat ulang tahun! Semoga panjang umur dan sehat selalu!",
            "🎂 Happy Birthday! Semoga semua impianmu tercapai tahun ini!",
            "🎈 Selamat bertambah usia! Bahagia selalu ya!",
            "🎁 Wishing you a wonderful birthday filled with joy and happiness!",
            "🌟 Selamat ulang tahun! Semoga makin sukses dan bahagia!",
            "🎊 Happy Birthday! May all your wishes come true!",
            "💝 Selamat ulang tahun! Tetap jadi orang baik dan inspiratif!",
            "🎵 Selamat ulang tahun! Semoga hari ini penuh kebahagiaan!",
            "✨ Happy Birthday! Terima kasih sudah menjadi bagian dari komunitas kita!",
            "🌈 Selamat ulang tahun! Semoga tahun ini lebih berwarna!"
        )
    }

    /**
     * Kirim ucapan massal (untuk admin)
     */
    fun sendBulkMessage(from: String, recipients: List<String>, message: String): Int {
        var successCount = 0
        recipients.forEach { recipient ->
            if (sendMessage(from, recipient, message, MessageType.ANNOUNCEMENT)) {
                successCount++
            }
        }
        return successCount
    }
}