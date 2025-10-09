package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import java.util.*

class StatisticsFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var tvTotalMembers: TextView
    private lateinit var tvBirthdaysToday: TextView
    private lateinit var tvBirthdaysThisWeek: TextView
    private lateinit var tvBirthdaysThisMonth: TextView
    private lateinit var tvAverageAge: TextView
    private lateinit var tvOldestMember: TextView
    private lateinit var tvYoungestMember: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        tvTotalMembers = view.findViewById(R.id.tvTotalMembers)
        tvBirthdaysToday = view.findViewById(R.id.tvBirthdaysToday)
        tvBirthdaysThisWeek = view.findViewById(R.id.tvBirthdaysThisWeek)
        tvBirthdaysThisMonth = view.findViewById(R.id.tvBirthdaysThisMonth)
        tvAverageAge = view.findViewById(R.id.tvAverageAge)
        tvOldestMember = view.findViewById(R.id.tvOldestMember)
        tvYoungestMember = view.findViewById(R.id.tvYoungestMember)

        loadStatistics()
        return view
    }

    private fun loadStatistics() {
        database.child("birthdays").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val birthdays = mutableListOf<BirthdayData>()

                for (data in snapshot.children) {
                    val name = data.child("name").getValue(String::class.java) ?: ""
                    val date = data.child("date").getValue(String::class.java) ?: ""
                    if (name.isNotEmpty() && date.isNotEmpty()) {
                        birthdays.add(BirthdayData(name, date))
                    }
                }

                calculateStatistics(birthdays)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun calculateStatistics(birthdays: List<BirthdayData>) {
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

            // Check oldest
            if (age > oldestAge) {
                oldestAge = age
                oldestMember = birthday.name
            }

            // Check youngest
            if (age < youngestAge) {
                youngestAge = age
                youngestMember = birthday.name
            }

            // Today
            if (day == todayDay && month == todayMonth) {
                birthdaysToday++
            }

            // This month
            if (month == todayMonth) {
                birthdaysThisMonth++
            }

            // This week
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

        // Update UI
        activity?.runOnUiThread {
            if (isAdded) {
                tvTotalMembers.text = birthdays.size.toString()
                tvBirthdaysToday.text = birthdaysToday.toString()
                tvBirthdaysThisWeek.text = birthdaysThisWeek.toString()
                tvBirthdaysThisMonth.text = birthdaysThisMonth.toString()
                tvAverageAge.text = "$averageAge tahun"
                tvOldestMember.text = if (oldestMember.isNotEmpty()) "$oldestMember ($oldestAge tahun)" else "-"
                tvYoungestMember.text = if (youngestMember.isNotEmpty()) "$youngestMember ($youngestAge tahun)" else "-"
            }
        }
    }

    data class BirthdayData(val name: String, val date: String)
}