package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.util.*

class UpcomingBirthdaysFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var rvToday: RecyclerView
    private lateinit var rvThisWeek: RecyclerView
    private lateinit var rvThisMonth: RecyclerView
    private lateinit var tvTodayCount: TextView
    private lateinit var tvWeekCount: TextView
    private lateinit var tvMonthCount: TextView
    private lateinit var tvEmptyToday: TextView
    private lateinit var tvEmptyWeek: TextView
    private lateinit var tvEmptyMonth: TextView

    private val allBirthdays = mutableListOf<BirthdayItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_upcoming_birthdays, container, false)

        database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        rvToday = view.findViewById(R.id.rvToday)
        rvThisWeek = view.findViewById(R.id.rvThisWeek)
        rvThisMonth = view.findViewById(R.id.rvThisMonth)
        tvTodayCount = view.findViewById(R.id.tvTodayCount)
        tvWeekCount = view.findViewById(R.id.tvWeekCount)
        tvMonthCount = view.findViewById(R.id.tvMonthCount)
        tvEmptyToday = view.findViewById(R.id.tvEmptyToday)
        tvEmptyWeek = view.findViewById(R.id.tvEmptyWeek)
        tvEmptyMonth = view.findViewById(R.id.tvEmptyMonth)

        rvToday.layoutManager = LinearLayoutManager(requireContext())
        rvThisWeek.layoutManager = LinearLayoutManager(requireContext())
        rvThisMonth.layoutManager = LinearLayoutManager(requireContext())

        loadBirthdays()
        return view
    }

    private fun loadBirthdays() {
        database.child("birthdays").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allBirthdays.clear()
                for (data in snapshot.children) {
                    val name = data.child("name").getValue(String::class.java) ?: ""
                    val date = data.child("date").getValue(String::class.java) ?: ""
                    val key = data.key ?: ""
                    if (name.isNotEmpty() && date.isNotEmpty()) {
                        allBirthdays.add(BirthdayItem(key, name, date))
                    }
                }
                categorizeBirthdays()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun categorizeBirthdays() {
        val today = Calendar.getInstance()
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        val todayMonth = today.get(Calendar.MONTH) + 1

        val weekLater = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, 1) }
        val monthLater = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }

        val todayList = mutableListOf<BirthdayItem>()
        val weekList = mutableListOf<BirthdayItem>()
        val monthList = mutableListOf<BirthdayItem>()

        for (birthday in allBirthdays) {
            val parts = birthday.date.split("/")
            if (parts.size < 2) continue

            val day = parts[0].toIntOrNull() ?: continue
            val month = parts[1].toIntOrNull() ?: continue

            // Today
            if (day == todayDay && month == todayMonth) {
                todayList.add(birthday)
            }

            // This week (rough calculation)
            val birthdayCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.MONTH, month - 1)
                if (before(today)) {
                    add(Calendar.YEAR, 1)
                }
            }

            if (birthdayCal.timeInMillis <= weekLater.timeInMillis && birthdayCal >= today) {
                weekList.add(birthday)
            }

            if (birthdayCal.timeInMillis <= monthLater.timeInMillis && birthdayCal >= today) {
                monthList.add(birthday)
            }
        }

        updateUI(todayList, weekList, monthList)
    }

    private fun updateUI(
        todayList: List<BirthdayItem>,
        weekList: List<BirthdayItem>,
        monthList: List<BirthdayItem>
    ) {
        // Today
        tvTodayCount.text = "${todayList.size} orang"
        if (todayList.isEmpty()) {
            tvEmptyToday.visibility = View.VISIBLE
            rvToday.visibility = View.GONE
        } else {
            tvEmptyToday.visibility = View.GONE
            rvToday.visibility = View.VISIBLE
            rvToday.adapter = UpcomingAdapter(todayList)
        }

        // This Week
        tvWeekCount.text = "${weekList.size} orang"
        if (weekList.isEmpty()) {
            tvEmptyWeek.visibility = View.VISIBLE
            rvThisWeek.visibility = View.GONE
        } else {
            tvEmptyWeek.visibility = View.GONE
            rvThisWeek.visibility = View.VISIBLE
            rvThisWeek.adapter = UpcomingAdapter(weekList)
        }

        // This Month
        tvMonthCount.text = "${monthList.size} orang"
        if (monthList.isEmpty()) {
            tvEmptyMonth.visibility = View.VISIBLE
            rvThisMonth.visibility = View.GONE
        } else {
            tvEmptyMonth.visibility = View.GONE
            rvThisMonth.visibility = View.VISIBLE
            rvThisMonth.adapter = UpcomingAdapter(monthList)
        }
    }
}

class UpcomingAdapter(private val items: List<BirthdayItem>) :
    RecyclerView.Adapter<UpcomingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvUpcomingName)
        val tvDate: TextView = view.findViewById(R.id.tvUpcomingDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_upcoming_birthday, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvDate.text = item.date
    }

    override fun getItemCount() = items.size
}