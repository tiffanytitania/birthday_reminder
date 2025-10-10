package com.example.birthday_reminder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.birthday_reminder.utils.NotificationHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class BirthdayWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            checkBirthdays()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun checkBirthdays() {
        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        val snapshot = database.child("birthdays").get().await()
        val today = Calendar.getInstance()
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        val todayMonth = today.get(Calendar.MONTH) + 1

        // H-1 (besok)
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        val tomorrowDay = tomorrow.get(Calendar.DAY_OF_MONTH)
        val tomorrowMonth = tomorrow.get(Calendar.MONTH) + 1

        // H-3 (3 hari lagi)
        val threeDaysLater = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 3) }
        val threeDaysDay = threeDaysLater.get(Calendar.DAY_OF_MONTH)
        val threeDaysMonth = threeDaysLater.get(Calendar.MONTH) + 1

        for (data in snapshot.children) {
            val name = data.child("name").getValue(String::class.java) ?: continue
            val dateStr = data.child("date").getValue(String::class.java) ?: continue

            val parts = dateStr.split("/")
            if (parts.size < 2) continue

            val birthDay = parts[0].toIntOrNull() ?: continue
            val birthMonth = parts[1].toIntOrNull() ?: continue

            // H-0: Hari ini ulang tahun
            if (birthDay == todayDay && birthMonth == todayMonth) {
                NotificationHelper.showBirthdayNotification(
                    applicationContext,
                    name,
                    "🎉 Hari ini ulang tahun $name! Jangan lupa ucapkan selamat!"
                )
            }

            // H-1: Besok ulang tahun
            if (birthDay == tomorrowDay && birthMonth == tomorrowMonth) {
                NotificationHelper.showBirthdayNotification(
                    applicationContext,
                    name,
                    "⏰ Besok ulang tahun $name! Siapkan ucapanmu!"
                )
            }

            // H-3: 3 hari lagi ulang tahun
            if (birthDay == threeDaysDay && birthMonth == threeDaysMonth) {
                NotificationHelper.showBirthdayNotification(
                    applicationContext,
                    name,
                    "📅 3 hari lagi ulang tahun $name!"
                )
            }
        }
    }
}