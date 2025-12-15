package com.example.birthday_reminder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.birthday_reminder.databinding.FragmentMemberDetailBinding
import com.example.birthday_reminder.messaging.MessageManager
import java.util.*

class MemberDetailFragment : Fragment() {
    private var _binding: FragmentMemberDetailBinding? = null
    private val binding get() = _binding!!

    private var memberName: String = ""
    private var memberPhone: String = ""
    private var memberEmail: String = ""
    private var memberBirthDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get data from arguments
        arguments?.let {
            memberName = it.getString("member_name", "")
            memberPhone = it.getString("member_phone", "")
            memberEmail = it.getString("member_email", "")
            memberBirthDate = it.getString("member_birthdate", "")
        }

        setupUI()
        setupButtons()
    }

    private fun setupUI() {
        binding.tvName.text = memberName

        // Phone
        if (memberPhone.isNotEmpty()) {
            binding.tvPhone.text = memberPhone
        } else {
            binding.tvPhone.text = "Tidak tersedia"
            binding.cardPhone.alpha = 0.5f
        }

        // Email
        if (memberEmail.isNotEmpty()) {
            binding.tvEmail.text = memberEmail
        } else {
            binding.tvEmail.text = "Tidak tersedia"
            binding.cardEmail.alpha = 0.5f
        }

        // Birth date
        if (memberBirthDate.isNotEmpty()) {
            binding.tvBirthDate.text = formatBirthDate(memberBirthDate)

            // Calculate age
            val age = calculateAge(memberBirthDate)
            if (age != null) {
                binding.tvAge.text = "$age tahun"
            } else {
                binding.tvAge.text = "-"
            }

            // Days until birthday
            val daysUntil = calculateDaysUntilBirthday(memberBirthDate)
            if (daysUntil == 0) {
                binding.tvDaysUntilBirthday.text = "🎉 Hari ini!"
            } else if (daysUntil > 0) {
                binding.tvDaysUntilBirthday.text = "$daysUntil hari lagi"
            }
        } else {
            binding.tvBirthDate.text = "Tidak tersedia"
            binding.tvAge.text = "-"
            binding.tvDaysUntilBirthday.text = "-"
        }

        // Profile photo
        binding.ivProfilePhoto.setImageResource(R.drawable.ic_person)
    }

    private fun setupButtons() {
        val hasPhone = memberPhone.isNotEmpty()
        val hasEmail = memberEmail.isNotEmpty()

        // WhatsApp
        binding.btnWhatsAppDetail.isEnabled = hasPhone
        binding.btnWhatsAppDetail.alpha = if (hasPhone) 1.0f else 0.5f
        binding.btnWhatsAppDetail.setOnClickListener {
            if (hasPhone) openWhatsApp()
        }

        // Call
        binding.btnCallDetail.isEnabled = hasPhone
        binding.btnCallDetail.alpha = if (hasPhone) 1.0f else 0.5f
        binding.btnCallDetail.setOnClickListener {
            if (hasPhone) callMember()
        }

        // SMS
        binding.btnSmsDetail.isEnabled = hasPhone
        binding.btnSmsDetail.alpha = if (hasPhone) 1.0f else 0.5f
        binding.btnSmsDetail.setOnClickListener {
            if (hasPhone) sendSms()
        }

        // Email
        binding.btnEmailDetail.isEnabled = hasEmail
        binding.btnEmailDetail.alpha = if (hasEmail) 1.0f else 0.5f
        binding.btnEmailDetail.setOnClickListener {
            if (hasEmail) sendEmail()
        }

        // Send message (internal)
        binding.btnSendMessage.setOnClickListener {
            sendInternalMessage()
        }

        // Back button
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun formatBirthDate(date: String): String {
        val parts = date.split("/")
        if (parts.size < 3) return date

        val months = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )

        val day = parts[0].toIntOrNull() ?: return date
        val month = parts[1].toIntOrNull() ?: return date
        val year = parts[2]

        if (month < 1 || month > 12) return date

        return "$day ${months[month - 1]} $year"
    }

    private fun calculateAge(birthDate: String): Int? {
        val parts = birthDate.split("/")
        if (parts.size < 3) return null

        val birthYear = parts[2].toIntOrNull() ?: return null
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return currentYear - birthYear
    }

    private fun calculateDaysUntilBirthday(birthDate: String): Int {
        val parts = birthDate.split("/")
        if (parts.size < 2) return -1

        val birthDay = parts[0].toIntOrNull() ?: return -1
        val birthMonth = parts[1].toIntOrNull() ?: return -1

        val today = Calendar.getInstance()
        val birthday = Calendar.getInstance().apply {
            set(Calendar.YEAR, today.get(Calendar.YEAR))
            set(Calendar.MONTH, birthMonth - 1)
            set(Calendar.DAY_OF_MONTH, birthDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (birthday.before(today)) {
            birthday.add(Calendar.YEAR, 1)
        }

        val diff = birthday.timeInMillis - today.timeInMillis
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun openWhatsApp() {
        try {
            val phone = memberPhone.replace("[^0-9]".toRegex(), "")
            val phoneWithCountryCode = if (phone.startsWith("0")) {
                "62${phone.substring(1)}"
            } else {
                phone
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$phoneWithCountryCode")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
        }
    }

    private fun callMember() {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$memberPhone")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka dialer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSms() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$memberPhone")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka SMS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$memberEmail")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka email", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendInternalMessage() {
        // Navigate to SendGreetingFragment dengan pre-filled recipient
        val bundle = Bundle().apply {
            putString("recipient", memberName)
        }

        val fragment = SendGreetingFragment().apply {
            arguments = bundle
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}