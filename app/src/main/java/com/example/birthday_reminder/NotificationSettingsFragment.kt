package com.example.birthday_reminder

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.birthday_reminder.databinding.FragmentNotificationSettingsBinding
import com.example.birthday_reminder.settings.NotificationSettingsManager

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

        // Notification time
        val hour = settings.notificationTime
        binding.tvNotificationTime.text = String.format("%02d:00", hour)

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
        }

        // Day checkboxes
        val checkboxListener = { _: android.widget.CompoundButton, _: Boolean ->
            saveDaysAhead()
        }

        binding.cbToday.setOnCheckedChangeListener(checkboxListener)
        binding.cbTomorrow.setOnCheckedChangeListener(checkboxListener)
        binding.cbThreeDays.setOnCheckedChangeListener(checkboxListener)
        binding.cbOneWeek.setOnCheckedChangeListener(checkboxListener)

        // Time picker
        binding.btnChangeTime.setOnClickListener {
            showTimePicker()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
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
    }

    private fun showTimePicker() {
        val currentHour = NotificationSettingsManager.getSettings().notificationTime

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, _ ->
                NotificationSettingsManager.setNotificationTime(hourOfDay)
                binding.tvNotificationTime.text = String.format("%02d:00", hourOfDay)
                Toast.makeText(
                    requireContext(),
                    "⏰ Notifikasi akan dikirim jam $hourOfDay:00",
                    Toast.LENGTH_SHORT
                ).show()
            },
            currentHour,
            0,
            true // 24 hour format
        ).show()
    }

    private fun saveSettings() {
        val settings = NotificationSettingsManager.getSettings()

        Toast.makeText(
            requireContext(),
            "✅ Pengaturan notifikasi berhasil disimpan!",
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