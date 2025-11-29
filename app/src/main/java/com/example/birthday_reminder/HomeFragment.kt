package com.example.birthday_reminder

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
    private val birthdays = mutableListOf<Pair<String, String>>()
    private var birthdayListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        try {
            database = FirebaseDatabase.getInstance("https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .reference

            fetchBirthdays()

            binding.calendarView.setOnDateChangedListener { _, date, _ ->
                showBirthdayList(date)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in onCreateView", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    private fun fetchBirthdays() {
        try {
            birthdayListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    birthdays.clear()
                    for (data in snapshot.children) {
                        try {
                            val date = data.child("date").getValue(String::class.java)
                            val name = data.child("name").getValue(String::class.java)
                            if (date != null && name != null) {
                                birthdays.add(Pair(date, name))
                            }
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error parsing birthday data", e)
                        }
                    }

                    activity?.runOnUiThread {
                        if (isAdded && _binding != null) {
                            highlightBirthdays()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeFragment", "Firebase error: ${error.message}")
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            database.child("birthdays").addValueEventListener(birthdayListener!!)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error fetching birthdays", e)
        }
    }

    private fun highlightBirthdays() {
        try {
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

            if (_binding != null) {
                binding.calendarView.removeDecorators()
                binding.calendarView.addDecorator(decorator)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error highlighting birthdays", e)
        }
    }

    private fun showBirthdayList(date: CalendarDay) {
        if (!isAdded || context == null) return

        try {
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
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error showing birthday list", e)
            Toast.makeText(context, "Error menampilkan data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateAge(dateString: String, currentYear: Int): Int {
        return try {
            val parts = dateString.split("/")
            if (parts.size != 3) return 0
            val birthYear = parts[2].toIntOrNull() ?: return 0
            currentYear - birthYear
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error calculating age", e)
            0
        }
    }

    private fun dateMatches(dateString: String, day: Int, month: Int): Boolean {
        return try {
            val parts = dateString.split("/")
            if (parts.size < 2) return false

            val dayPart = parts[0].toIntOrNull() ?: return false
            val monthPart = parts[1].toIntOrNull() ?: return false

            (dayPart == day && monthPart == month)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error matching date", e)
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listener to prevent memory leak
        birthdayListener?.let {
            database.child("birthdays").removeEventListener(it)
        }
        _binding = null
    }
}