package com.example.birthday_reminder

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.birthday_reminder.databinding.FragmentHomeBinding
import com.google.firebase.database.*
import com.prolificinteractive.materialcalendarview.*
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    private val birthdays = mutableListOf<Pair<String, String>>() // Pair(date, name)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance("https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference

        fetchBirthdays()

        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            showBirthdayList(date)
        }

        return binding.root
    }

    private fun fetchBirthdays() {
        database.child("birthdays").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                birthdays.clear()
                for (data in snapshot.children) {
                    val date = data.child("date").getValue(String::class.java)
                    val name = data.child("name").getValue(String::class.java)
                    if (date != null && name != null) {
                        birthdays.add(Pair(date, name))
                    }
                }
                highlightBirthdays()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun highlightBirthdays() {
        val decorator = object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay): Boolean {
                val calendarDay = day.day
                val calendarMonth = day.month + 1
                return birthdays.any { dateMatches(it.first, calendarDay, calendarMonth) }
            }

            override fun decorate(view: DayViewFacade) {
                view.addSpan(DotSpan(8f, Color.RED))
            }
        }

        binding.calendarView.removeDecorators()
        binding.calendarView.addDecorator(decorator)
    }

    private fun showBirthdayList(date: CalendarDay) {
        val day = date.day
        val month = date.month + 1
        val yearNow = Calendar.getInstance().get(Calendar.YEAR)

        val people = birthdays.filter { dateMatches(it.first, day, month) }

        if (people.isEmpty()) return

        val names = people.map { it.second }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Ulang Tahun 🎉 (${day}/${month})")
            .setItems(names) { _, which ->
                val selected = people[which]
                val birthDate = selected.first
                val name = selected.second

                val age = calculateAge(birthDate, yearNow)

                AlertDialog.Builder(requireContext())
                    .setTitle(name)
                    .setMessage("Tanggal Lahir: $birthDate\nUsia saat ini: $age tahun")
                    .setPositiveButton("OK", null)
                    .show()
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun calculateAge(dateString: String, currentYear: Int): Int {
        val parts = dateString.split("/")
        if (parts.size != 3) return 0
        val birthYear = parts[2].toIntOrNull() ?: return 0
        return currentYear - birthYear
    }

    private fun dateMatches(dateString: String, day: Int, month: Int): Boolean {
        val parts = dateString.split("/")
        if (parts.size < 2) return false

        val dayPart = parts[0].toIntOrNull() ?: return false
        val monthPart = parts[1].toIntOrNull() ?: return false

        return (dayPart == day && monthPart == month)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
