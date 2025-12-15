package com.example.birthday_reminder.utils

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

object TestHelper {

    fun generateDummyBirthdays(context: Context) {
        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val dummyData = listOf(
            // Hari ini
            Pair("Ahmad (Hari Ini)", getTodayDate()),
            Pair("Siti (Hari Ini)", getTodayDate()),

            // Besok (H-1)
            Pair("Budi (Besok)", getTomorrowDate()),

            // 3 hari lagi (H-3)
            Pair("Dewi (3 Hari Lagi)", getDateAfterDays(3)),

            // Minggu ini
            Pair("Eko (Minggu Ini)", getDateAfterDays(5)),
            Pair("Fitri (Minggu Ini)", getDateAfterDays(6)),

            // Bulan ini
            Pair("Gita (Bulan Ini)", getDateAfterDays(15)),
            Pair("Hadi (Bulan Ini)", getDateAfterDays(20)),
            Pair("Indah (Bulan Ini)", getDateAfterDays(25)),

            // Tahun berbeda untuk testing statistik
            Pair("Senior (60th)", "10/10/1964"),
            Pair("Junior (18th)", "10/10/2006"),
        )

        dummyData.forEach { (name, date) ->
            val birthdayMap = mapOf(
                "name" to name,
                "date" to date
            )

            database.child("birthdays").push().setValue(birthdayMap)
                .addOnSuccessListener {
                    Log.d("TestHelper", "✅ Added: $name - $date")
                }
                .addOnFailureListener { e ->
                    Log.e("TestHelper", "❌ Failed to add: $name - ${e.message}")
                }
        }

        Log.d("TestHelper", "🎉 Dummy data generation started!")
    }

    /**
     * Hapus semua data birthday (untuk reset testing)
     */
    fun clearAllBirthdays() {
        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        database.child("birthdays").removeValue()
            .addOnSuccessListener {
                Log.d("TestHelper", "🗑️ All birthdays cleared!")
            }
            .addOnFailureListener { e ->
                Log.e("TestHelper", "❌ Failed to clear: ${e.message}")
            }
    }

    /**
     * Test notifikasi langsung (tanpa tunggu WorkManager)
     */
    fun testNotificationNow(context: Context) {
        NotificationHelper.showBirthdayNotification(
            context,
            "Test User",
            "🎉 Ini adalah test notifikasi! Jika muncul, berarti notifikasi berfungsi dengan baik."
        )
        Log.d("TestHelper", "📢 Test notification sent!")
    }

    /**
     * Print semua birthday dari Firebase ke Logcat
     */
    fun debugPrintAllBirthdays() {
        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        database.child("birthdays").get()
            .addOnSuccessListener { snapshot ->
                Log.d("TestHelper", "=== ALL BIRTHDAYS IN DATABASE ===")
                for (data in snapshot.children) {
                    val name = data.child("name").getValue(String::class.java)
                    val date = data.child("date").getValue(String::class.java)
                    Log.d("TestHelper", "📅 $name - $date")
                }
                Log.d("TestHelper", "=== END OF DATA ===")
            }
            .addOnFailureListener { e ->
                Log.e("TestHelper", "❌ Failed to fetch: ${e.message}")
            }
    }

    // Helper functions untuk generate tanggal
    private fun getTodayDate(): String {
        val today = Calendar.getInstance()
        return "${today.get(Calendar.DAY_OF_MONTH)}/${today.get(Calendar.MONTH) + 1}/${today.get(Calendar.YEAR)}"
    }

    private fun getTomorrowDate(): String {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return "${tomorrow.get(Calendar.DAY_OF_MONTH)}/${tomorrow.get(Calendar.MONTH) + 1}/${tomorrow.get(Calendar.YEAR)}"
    }

    private fun getDateAfterDays(days: Int): String {
        val future = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, days)
        }
        return "${future.get(Calendar.DAY_OF_MONTH)}/${future.get(Calendar.MONTH) + 1}/${future.get(Calendar.YEAR)}"
    }
}

