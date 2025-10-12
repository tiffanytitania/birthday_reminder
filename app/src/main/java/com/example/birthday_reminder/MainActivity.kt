package com.example.birthday_reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.databinding.ActivityMainBinding
import com.example.birthday_reminder.messaging.MessageManager
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

        // Inisialisasi UserManager dengan context
        UserManager.init(this)

        // Inisialisasi MessageManager (untuk fitur messaging)
        MessageManager.init(this)

        // Cek apakah user sudah login (dengan persistence)
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

            // Buat Notification Channel
            createNotificationChannel()
            requestNotificationPermission()

            // Setup WorkManager untuk notifikasi otomatis
            setupBirthdayNotifications()

            // Default fragment saat aplikasi dibuka
            replaceFragment(HomeFragment())

            // Setup bottom navigation dengan fitur admin
            setupBottomNavigation()

            // Logout button
            binding.btnLogout.setOnClickListener {
                showLogoutDialog()
            }

            // Update welcome text dengan role
            updateWelcomeText()

            // Show admin badge jika user adalah admin
            if (UserManager.isAdmin()) {
                binding.tvAdminBadge.visibility = View.VISIBLE
            } else {
                binding.tvAdminBadge.visibility = View.GONE
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            e.printStackTrace()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_upcoming -> {
                    replaceFragment(UpcomingBirthdaysFragment())
                    true
                }
                R.id.nav_add -> {
                    replaceFragment(AddBirthdayFragment())
                    true
                }
                R.id.nav_statistics -> {
                    replaceFragment(StatisticsFragment())
                    true
                }
                R.id.nav_more -> {
                    showMoreMenu()
                    true
                }
                else -> false
            }
        }
    }

    private fun showMoreMenu() {
        val options = mutableListOf<String>()
        options.add("👤 Profil Saya")
        options.add("👥 Direktori Anggota") // 🆕 BARU!
        options.add("💬 Ucapan & Quotes")
        options.add("📜 Riwayat Pesan")
        options.add("🔔 Pengaturan Notifikasi")

        // Tambahkan opsi admin jika user adalah admin
        if (UserManager.isAdmin()) {
            options.add("👑 Panel Admin")
        }

        options.add("❓ Tentang Aplikasi")

        AlertDialog.Builder(this)
            .setTitle("Menu Lainnya")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "👤 Profil Saya" -> replaceFragment(ProfileFragment())
                    "👥 Direktori Anggota" -> replaceFragment(MemberDirectoryFragment()) // 🆕 BARU!
                    "💬 Ucapan & Quotes" -> replaceFragment(MoreFragment())
                    "📜 Riwayat Pesan" -> replaceFragment(HistoryFragment())
                    "🔔 Pengaturan Notifikasi" -> replaceFragment(NotificationSettingsFragment())
                    "👑 Panel Admin" -> replaceFragment(AdminPanelFragment())
                    "❓ Tentang Aplikasi" -> showAboutDialog()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateWelcomeText() {
        val username = UserManager.getCurrentUser() ?: "Guest"
        val role = if (UserManager.isAdmin()) "Admin" else "Anggota"
        binding.tvWelcome.text = "Halo, $username ($role)! 👋"
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Tentang Aplikasi")
            .setMessage("""
                Birthday Reminder
                Versi 2.0
                
                Aplikasi pengingat ulang tahun untuk komunitas dengan fitur:
                • Kalender ulang tahun
                • Notifikasi otomatis
                • Statistik komunitas
                • Ucapan & quotes
                • Panel admin
                • Sistem messaging internal
                • Role management
                • Dan lainnya!
                
                © 2025 Birthday Reminder Team
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupBirthdayNotifications() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<BirthdayWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "birthday_notification_work",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )

        Log.d("MainActivity", "WorkManager setup complete")
    }

    private fun calculateInitialDelay(): Long {
        val currentTime = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 8)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)

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