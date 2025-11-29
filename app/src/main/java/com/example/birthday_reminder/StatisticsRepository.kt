package com.example.birthday_reminder.data.repository

import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import java.util.*

data class StatisticsData(
    val totalMembers: Int = 0,
    val birthdaysToday: Int = 0,
    val birthdaysThisWeek: Int = 0,
    val birthdaysThisMonth: Int = 0,
    val averageAge: Int = 0,
    val oldestMember: String = "-",
    val oldestAge: Int = 0,
    val youngestMember: String = "-",
    val youngestAge: Int = 999
)

class StatisticsRepository {

    private val database: DatabaseReference = FirebaseDatabase.getInstance(
        "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).reference.child("birthdays")

    suspend fun getStatistics(): Result<StatisticsData> {
        return try {
            val snapshot = database.get().await()
            val birthdays = mutableListOf<BirthdayData>()

            for (data in snapshot.children) {
                val name = data.child("name").getValue(String::class.java) ?: ""
                val date = data.child("date").getValue(String::class.java) ?: ""
                if (name.isNotEmpty() && date.isNotEmpty()) {
                    birthdays.add(BirthdayData(name, date))
                }
            }

            val stats = calculateStatistics(birthdays)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateStatistics(birthdays: List<BirthdayData>): StatisticsData {
        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        val todayMonth = today.get(Calendar.MONTH) + 1

        var birthdaysToday = 0
        var birthdaysThisWeek = 0
        var birthdaysThisMonth = 0
        val ages = mutableListOf<Int>()
        var oldestMember = ""
        var oldestAge = 0
        var youngestMember = ""
        var youngestAge = 999

        val weekLater = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, 1) }

        for (birthday in birthdays) {
            val parts = birthday.date.split("/")
            if (parts.size < 3) continue

            val day = parts[0].toIntOrNull() ?: continue
            val month = parts[1].toIntOrNull() ?: continue
            val year = parts[2].toIntOrNull() ?: continue

            val age = currentYear - year
            ages.add(age)

            if (age > oldestAge) {
                oldestAge = age
                oldestMember = birthday.name
            }

            if (age < youngestAge) {
                youngestAge = age
                youngestMember = birthday.name
            }

            if (day == todayDay && month == todayMonth) {
                birthdaysToday++
            }

            if (month == todayMonth) {
                birthdaysThisMonth++
            }

            val birthdayCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.MONTH, month - 1)
                if (before(today)) {
                    add(Calendar.YEAR, 1)
                }
            }

            if (birthdayCal.timeInMillis <= weekLater.timeInMillis && birthdayCal >= today) {
                birthdaysThisWeek++
            }
        }

        val averageAge = if (ages.isNotEmpty()) ages.average().toInt() else 0

        return StatisticsData(
            totalMembers = birthdays.size,
            birthdaysToday = birthdaysToday,
            birthdaysThisWeek = birthdaysThisWeek,
            birthdaysThisMonth = birthdaysThisMonth,
            averageAge = averageAge,
            oldestMember = if (oldestMember.isNotEmpty()) "$oldestMember ($oldestAge tahun)" else "-",
            oldestAge = oldestAge,
            youngestMember = if (youngestMember.isNotEmpty()) "$youngestMember ($youngestAge tahun)" else "-",
            youngestAge = youngestAge
        )
    }

    data class BirthdayData(val name: String, val date: String)
}