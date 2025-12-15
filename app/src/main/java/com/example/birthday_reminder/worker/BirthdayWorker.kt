package com.example.birthday_reminder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.birthday_reminder.settings.NotificationSettingsManager
import com.example.birthday_reminder.utils.NotificationHelper
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.*

class BirthdayWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        android.util.Log.d("BirthdayWorker", "🔔 Worker started at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")

        return try {
            checkBirthdays()
            android.util.Log.d("BirthdayWorker", "✅ Worker completed successfully")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("BirthdayWorker", "❌ Worker failed: ${e.message}", e)
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun checkBirthdays() {
        // Inisialisasi NotificationSettingsManager
        NotificationSettingsManager.init(applicationContext)

        // Ambil settings dari user
        val settings = NotificationSettingsManager.getSettings()

        // Cek apakah notifikasi enabled
        if (!settings.enabled) {
            return // Skip jika notifikasi dimatikan
        }

        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        val snapshot = database.child("birthdays").get().await()

        for (data in snapshot.children) {
            val name = data.child("name").getValue(String::class.java) ?: continue
            val dateStr = data.child("date").getValue(String::class.java) ?: continue

            val parts = dateStr.split("/")
            if (parts.size < 2) continue

            val birthDay = parts[0].toIntOrNull() ?: continue
            val birthMonth = parts[1].toIntOrNull() ?: continue

            // Cek untuk setiap hari yang enabled di settings
            for (daysAhead in settings.daysAhead) {
                val targetDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, daysAhead)
                }
                val targetDay = targetDate.get(Calendar.DAY_OF_MONTH)
                val targetMonth = targetDate.get(Calendar.MONTH) + 1

                if (birthDay == targetDay && birthMonth == targetMonth) {
                    val message = when (daysAhead) {
                        0 -> "🎉 Hari ini ulang tahun $name! Jangan lupa ucapkan selamat!"
                        1 -> "⏰ Besok ulang tahun $name! Siapkan ucapanmu!"
                        3 -> "📅 3 hari lagi ulang tahun $name!"
                        7 -> "📆 Seminggu lagi ulang tahun $name!"
                        else -> "🎂 $daysAhead hari lagi ulang tahun $name!"
                    }

                    NotificationHelper.showBirthdayNotification(
                        applicationContext,
                        name,
                        message
                    )
                }
            }
        }
    }
}