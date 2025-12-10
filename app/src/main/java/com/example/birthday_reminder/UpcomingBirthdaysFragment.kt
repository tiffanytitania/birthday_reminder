package com.example.birthday_reminder

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.birthday_reminder.databinding.FragmentUpcomingBirthdaysBinding
import com.example.birthday_reminder.ui.viewmodel.BirthdayViewModel
import com.example.birthday_reminder.ui.viewmodel.BirthdayItem
import com.example.birthday_reminder.utils.ImageKitConfig
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.util.*

class UpcomingBirthdaysFragment : Fragment() {

    private var _binding: FragmentUpcomingBirthdaysBinding? = null
    private val binding get() = _binding!!

    // ✅ Use ViewModel instead of direct Firebase
    private val viewModel: BirthdayViewModel by viewModels()

    // Firebase reference untuk banner only (bukan untuk birthdays)
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
    private lateinit var imgBanner: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpcomingBirthdaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase only for banner
        database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        // Initialize views
        initializeViews()

        // Load banner from Firebase
        loadCommunityBanner()

        // ✅ Observe ViewModel instead of direct Firebase
        observeViewModel()
    }

    private fun initializeViews() {
        imgBanner = binding.imgCommunityBannerTop
        rvToday = binding.rvToday
        rvThisWeek = binding.rvThisWeek
        rvThisMonth = binding.rvThisMonth
        tvTodayCount = binding.tvTodayCount
        tvWeekCount = binding.tvWeekCount
        tvMonthCount = binding.tvMonthCount
        tvEmptyToday = binding.tvEmptyToday
        tvEmptyWeek = binding.tvEmptyWeek
        tvEmptyMonth = binding.tvEmptyMonth

        rvToday.layoutManager = LinearLayoutManager(requireContext())
        rvThisWeek.layoutManager = LinearLayoutManager(requireContext())
        rvThisMonth.layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * ✅ Observe ViewModel untuk mendapatkan data birthdays
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.birthdays.collect { birthdays ->
                categorizeBirthdays(birthdays)
            }
        }
    }

    /**
     * Kategorisasi birthdays ke Today, This Week, This Month
     */
    private fun categorizeBirthdays(allBirthdays: List<BirthdayItem>) {
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

            // Calculate next occurrence
            val birthdayCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.MONTH, month - 1)
                if (before(today)) {
                    add(Calendar.YEAR, 1)
                }
            }

            // Within this week
            if (birthdayCal.timeInMillis <= weekLater.timeInMillis && birthdayCal >= today) {
                weekList.add(birthday)
            }

            // Within this month
            if (birthdayCal.timeInMillis <= monthLater.timeInMillis && birthdayCal >= today) {
                monthList.add(birthday)
            }
        }

        updateUI(todayList, weekList, monthList)
    }

    /**
     * Update UI dengan data yang sudah dikategorisasi
     */
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

    /**
     * Load banner komunitas dari Firebase Database
     * Menggunakan ImageKit untuk optimasi gambar
     */
    private fun loadCommunityBanner() {
        database.child("community_info").child("bannerUrl").get()
            .addOnSuccessListener { snapshot ->
                val bannerUrl = snapshot.getValue(String::class.java)

                if (!bannerUrl.isNullOrEmpty()) {
                    // Optimasi gambar menggunakan ImageKit
                    val optimizedUrl = ImageKitConfig.getTransformedUrl(
                        imageUrl = bannerUrl,
                        width = 800,
                        height = 300,
                        quality = 80
                    )

                    Glide.with(requireContext())
                        .load(optimizedUrl)
                        .placeholder(R.drawable.banner_placeholder)
                        .error(R.drawable.banner_placeholder)
                        .into(imgBanner)
                } else {
                    // Fallback ke local banner jika URL kosong
                    loadLocalBanner()
                }
            }
            .addOnFailureListener {
                // Fallback ke local banner jika gagal mengambil dari Firebase
                loadLocalBanner()
            }
    }

    /**
     * Fallback: Load banner dari SharedPreferences (local URI)
     */
    private fun loadLocalBanner() {
        val prefs = requireContext().getSharedPreferences("community_prefs", Activity.MODE_PRIVATE)
        val localUri = prefs.getString("localBannerUri", null)

        if (localUri != null) {
            try {
                Glide.with(requireContext())
                    .load(Uri.parse(localUri))
                    .placeholder(R.drawable.banner_placeholder)
                    .error(R.drawable.banner_placeholder)
                    .into(imgBanner)
            } catch (e: Exception) {
                imgBanner.setImageResource(R.drawable.banner_placeholder)
            }
        } else {
            imgBanner.setImageResource(R.drawable.banner_placeholder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * ✅ Simple adapter untuk upcoming birthdays
 * Hanya menampilkan name dan date
 */
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