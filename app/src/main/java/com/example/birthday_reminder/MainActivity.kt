package com.example.birthday_reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.*
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.databinding.ActivityMainBinding
import com.example.birthday_reminder.worker.BirthdayWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val CHANNEL_ID = "birthday_channel"
        const val NOTIFICATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek apakah user sudah login
        if (!UserManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Catch uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Log.e("MainActivity", "Uncaught exception", e)
            e.printStackTrace()
        }

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Buat Notification Channel (sekali di awal)
            createNotificationChannel()
            requestNotificationPermission()

            // 🆕 Setup WorkManager untuk notifikasi otomatis
            setupBirthdayNotifications()

            // Default fragment saat aplikasi dibuka (ganti ke UpcomingBirthdaysFragment)
            replaceFragment(UpcomingBirthdaysFragment())

            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> replaceFragment(HomeFragment())
                    R.id.nav_upcoming -> replaceFragment(UpcomingBirthdaysFragment())
                    R.id.nav_add -> replaceFragment(AddBirthdayFragment())
                    R.id.nav_statistics -> replaceFragment(StatisticsFragment())
                    R.id.nav_more -> replaceFragment(MoreFragment())
                }
                true
            }

            // Set default selected item
            binding.bottomNavigation.selectedItemId = R.id.nav_upcoming

            // Tambahkan logout button di header
            binding.btnLogout.setOnClickListener {
                showLogoutDialog()
            }

            // Update welcome text dengan username
            binding.tvWelcome.text = "Halo, ${UserManager.getCurrentUser()}! 👋"

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            e.printStackTrace()
        }
    }

    // 🆕 Setup WorkManager untuk cek ulang tahun setiap hari
    private fun setupBirthdayNotifications() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Jalankan setiap hari jam 8 pagi
        val dailyWorkRequest = PeriodicWorkRequestBuilder<BirthdayWorker>(
            24, TimeUnit.HOURS  // Setiap 24 jam
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "birthday_notification_work",
            ExistingPeriodicWorkPolicy.KEEP,  // Jangan duplikat
            dailyWorkRequest
        )

        Log.d("MainActivity", "WorkManager setup complete")
    }

    // Hitung delay hingga jam 8 pagi besok
    private fun calculateInitialDelay(): Long {
        val currentTime = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 8)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)

            // Jika sudah lewat jam 8, set untuk besok
            if (timeInMillis <= currentTime) {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }

        return calendar.timeInMillis - currentTime
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                UserManager.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun replaceFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error replacing fragment", e)
            e.printStackTrace()
        }
    }

    // Membuat Notification Channel (dibutuhkan Android 8+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Birthday Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel untuk ucapan ulang tahun"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    // Minta izin notifikasi jika Android 13+
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }
    }
}