package com.example.birthday_reminder

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.birthday_reminder.databinding.FragmentNotificationSettingsBinding
import com.example.birthday_reminder.settings.NotificationSettingsManager
import com.example.birthday_reminder.worker.BirthdayWorker
import java.util.concurrent.TimeUnit

class NotificationSettingsFragment : Fragment() {
    private var _binding: FragmentNotificationSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        NotificationSettingsManager.init(requireContext())
        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        val settings = NotificationSettingsManager.getSettings()

        // Master switch
        binding.switchNotifications.isChecked = settings.enabled

        // Days ahead checkboxes
        binding.cbToday.isChecked = settings.daysAhead.contains(0)
        binding.cbTomorrow.isChecked = settings.daysAhead.contains(1)
        binding.cbThreeDays.isChecked = settings.daysAhead.contains(3)
        binding.cbOneWeek.isChecked = settings.daysAhead.contains(7)

        // Notification time (dengan menit)
        binding.tvNotificationTime.text = settings.getFormattedTime()

        // Enable/disable checkboxes based on master switch
        updateCheckboxesState(settings.enabled)
    }

    private fun setupListeners() {
        // Master switch
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            NotificationSettingsManager.setEnabled(isChecked)
            updateCheckboxesState(isChecked)

            val message = if (isChecked) {
                "✅ Notifikasi diaktifkan"
            } else {
                "❌ Notifikasi dinonaktifkan"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

            // Reschedule WorkManager
            rescheduleNotifications()
        }

        // Day checkboxes
        val checkboxListener = { _: android.widget.CompoundButton, _: Boolean ->
            saveDaysAhead()
        }

        binding.cbToday.setOnCheckedChangeListener(checkboxListener)
        binding.cbTomorrow.setOnCheckedChangeListener(checkboxListener)
        binding.cbThreeDays.setOnCheckedChangeListener(checkboxListener)
        binding.cbOneWeek.setOnCheckedChangeListener(checkboxListener)

        // Time picker - SUPPORT MENIT SEKARANG
        binding.btnChangeTime.setOnClickListener {
            showTimePicker()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        // Test button
        binding.btnTestNotification?.setOnClickListener {
            testNotificationNow()
        }
    }

    private fun testNotificationNow() {
        com.example.birthday_reminder.utils.NotificationHelper.showBirthdayNotification(
            requireContext(),
            "Test User",
            "🎉 Ini test notifikasi! Jika muncul, berarti notifikasi berfungsi."
        )
        Toast.makeText(requireContext(), "📢 Test notifikasi dikirim!", Toast.LENGTH_SHORT).show()
    }

    private fun updateCheckboxesState(enabled: Boolean) {
        binding.cbToday.isEnabled = enabled
        binding.cbTomorrow.isEnabled = enabled
        binding.cbThreeDays.isEnabled = enabled
        binding.cbOneWeek.isEnabled = enabled
        binding.btnChangeTime.isEnabled = enabled
    }

    private fun saveDaysAhead() {
        val days = mutableListOf<Int>()

        if (binding.cbToday.isChecked) days.add(0)
        if (binding.cbTomorrow.isChecked) days.add(1)
        if (binding.cbThreeDays.isChecked) days.add(3)
        if (binding.cbOneWeek.isChecked) days.add(7)

        if (days.isEmpty()) {
            Toast.makeText(requireContext(), "⚠️ Minimal pilih 1 opsi", Toast.LENGTH_SHORT).show()
            // Reset to default
            binding.cbToday.isChecked = true
            days.add(0)
        }

        NotificationSettingsManager.setDaysAhead(days)

        // Reschedule setelah ubah pengaturan
        rescheduleNotifications()
    }

    private fun showTimePicker() {
        val settings = NotificationSettingsManager.getSettings()
        val currentHour = settings.getHour()
        val currentMinute = settings.getMinute()

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                // Hitung total menit dari jam dan menit
                val totalMinutes = (hourOfDay * 60) + minute

                // Simpan ke SharedPreferences
                NotificationSettingsManager.setNotificationTime(totalMinutes)

                // Update tampilan
                binding.tvNotificationTime.text = String.format("%02d:%02d", hourOfDay, minute)

                Toast.makeText(
                    requireContext(),
                    "⏰ Notifikasi akan dikirim jam ${String.format("%02d:%02d", hourOfDay, minute)}",
                    Toast.LENGTH_SHORT
                ).show()

                // LANGSUNG RESCHEDULE WORKMANAGER
                rescheduleNotifications()
            },
            currentHour,
            currentMinute,
            true // 24 hour format
        ).show()
    }

    /**
     * Reschedule WorkManager agar langsung pakai jadwal baru
     */
    private fun rescheduleNotifications() {
        val workManager = WorkManager.getInstance(requireContext())

        // Cancel pekerjaan lama
        workManager.cancelUniqueWork("birthday_notification_work")

        // Hitung delay baru
        val delay = calculateInitialDelay()

        // Buat work request baru
        val dailyWorkRequest = PeriodicWorkRequestBuilder<BirthdayWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        // Enqueue dengan policy REPLACE
        workManager.enqueueUniquePeriodicWork(
            "birthday_notification_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )

        val settings = NotificationSettingsManager.getSettings()
        android.util.Log.d("NotificationSettings", "✅ WorkManager rescheduled untuk jam ${settings.getFormattedTime()}")
        android.util.Log.d("NotificationSettings", "⏰ Next run in ${delay / 1000 / 60} minutes")
    }

    private fun calculateInitialDelay(): Long {
        val settings = NotificationSettingsManager.getSettings()
        val notificationHour = settings.getHour()
        val notificationMinute = settings.getMinute()

        val currentTime = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, notificationHour)
            set(java.util.Calendar.MINUTE, notificationMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)

            // Jika waktu sudah lewat hari ini, jadwalkan untuk besok
            if (timeInMillis <= currentTime) {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }

        return calendar.timeInMillis - currentTime
    }

    private fun saveSettings() {
        Toast.makeText(
            requireContext(),
            "✅ Pengaturan notifikasi berhasil disimpan dan dijadwalkan ulang!",
            Toast.LENGTH_SHORT
        ).show()

        // Optionally go back
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}