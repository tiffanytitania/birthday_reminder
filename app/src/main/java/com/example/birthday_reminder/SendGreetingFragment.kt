package com.example.birthday_reminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.data.model.MessageType
import com.example.birthday_reminder.databinding.FragmentSendGreetingBinding
import com.example.birthday_reminder.messaging.MessageManager
import com.example.birthday_reminder.ml.SentimentAnalyzer
import com.example.birthday_reminder.utils.LocationHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SendGreetingFragment : Fragment() {
    private var _binding: FragmentSendGreetingBinding? = null
    private val binding get() = _binding!!

    private val membersList = mutableListOf<String>()

    // 🆕 ML Feature
    private lateinit var sentimentAnalyzer: SentimentAnalyzer

    // 🆕 Location Feature
    private lateinit var locationHelper: LocationHelper
    private var currentLocationString: String? = null

    // 🆕 Location Permission Launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLocationAndSend()
        } else {
            Toast.makeText(requireContext(), "⚠️ Location permission denied. Mengirim tanpa lokasi.", Toast.LENGTH_SHORT).show()
            sendGreetingWithoutLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendGreetingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MessageManager.init(requireContext())

        // 🆕 Initialize ML
        sentimentAnalyzer = SentimentAnalyzer(requireContext())

        // 🆕 Initialize Location
        locationHelper = LocationHelper(requireContext())

        loadMembers()
        setupTemplateButton()
        setupSendButton()
        setupSentimentAnalysis()
        setupLocationDisplay()
    }

    private fun loadMembers() {
        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        database.child("birthdays").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                membersList.clear()
                for (data in snapshot.children) {
                    val name = data.child("name").getValue(String::class.java)
                    if (name != null) {
                        membersList.add(name)
                    }
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    membersList
                )
                binding.actvRecipient.setAdapter(adapter)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal load anggota", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupTemplateButton() {
        binding.btnSelectTemplate.setOnClickListener {
            showTemplateDialog()
        }
    }

    private fun showTemplateDialog() {
        val templates = MessageManager.getGreetingTemplates()

        AlertDialog.Builder(requireContext())
            .setTitle("🎉 Pilih Template Ucapan")
            .setItems(templates.toTypedArray()) { _, which ->
                binding.etMessage.setText(templates[which])
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            val recipient = binding.actvRecipient.text.toString().trim()
            val message = binding.etMessage.text.toString().trim()

            when {
                recipient.isEmpty() -> {
                    Toast.makeText(requireContext(), "Pilih penerima terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
                message.isEmpty() -> {
                    Toast.makeText(requireContext(), "Tulis pesan terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
                !membersList.contains(recipient) -> {
                    Toast.makeText(requireContext(), "Anggota tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // 🆕 Check location permission dulu sebelum kirim
                    checkLocationPermissionAndSend()
                }
            }
        }
    }

    // 🆕 Setup Sentiment Analysis
    private fun setupSentimentAnalysis() {
        binding.etMessage.addTextChangedListener { text ->
            val message = text.toString().trim()

            if (message.isEmpty()) {
                binding.tvSentimentResult.text = "Sentiment: Ketik pesan untuk analisis..."
                binding.tvConfidence.text = "Confidence: -"
            } else {
                val sentiment = sentimentAnalyzer.analyzeSentiment(message)
                val confidence = sentimentAnalyzer.getConfidence(message)

                binding.tvSentimentResult.text = "Sentiment: $sentiment"
                binding.tvConfidence.text = "Confidence: $confidence%"
            }
        }
    }

    // 🆕 Setup Location Display
    private fun setupLocationDisplay() {
        binding.btnGetLocation.setOnClickListener {
            if (locationHelper.hasLocationPermission()) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }
    }

    // 🆕 Check location permission sebelum kirim
    private fun checkLocationPermissionAndSend() {
        if (locationHelper.hasLocationPermission()) {
            getLocationAndSend()
        } else {
            // Tanya user apakah mau share location
            AlertDialog.Builder(requireContext())
                .setTitle("📍 Share Lokasi?")
                .setMessage("Kirim ucapan dengan lokasi kamu?\n\n(Opsional - bisa skip)")
                .setPositiveButton("Ya, Share") { _, _ ->
                    requestLocationPermission()
                }
                .setNegativeButton("Skip") { _, _ ->
                    sendGreetingWithoutLocation()
                }
                .show()
        }
    }

    // 🆕 Request location permission
    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // 🆕 Get current location
    private fun getCurrentLocation() {
        binding.progressLocation.visibility = View.VISIBLE
        binding.tvLocationResult.text = "Getting location..."

        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                val locationStr = locationHelper.formatLocation(location)
                val cityName = locationHelper.getCityName(location)

                currentLocationString = locationStr

                binding.progressLocation.visibility = View.GONE
                binding.tvLocationResult.text = "📍 $cityName\n$locationStr"

                Toast.makeText(requireContext(), "✅ Location obtained!", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                binding.progressLocation.visibility = View.GONE
                binding.tvLocationResult.text = "❌ Failed to get location"

                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 🆕 Get location and send greeting
    private fun getLocationAndSend() {
        binding.progressLocation.visibility = View.VISIBLE

        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                binding.progressLocation.visibility = View.GONE

                val locationStr = locationHelper.formatLocation(location)
                val cityName = locationHelper.getCityName(location)

                sendGreetingWithLocation("$cityName ($locationStr)")
            },
            onError = { error ->
                binding.progressLocation.visibility = View.GONE
                Toast.makeText(requireContext(), "Location error: $error. Sending without location.", Toast.LENGTH_SHORT).show()
                sendGreetingWithoutLocation()
            }
        )
    }

    // 🆕 Send greeting WITH location
    private fun sendGreetingWithLocation(location: String) {
        val recipient = binding.actvRecipient.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()
        val sender = UserManager.getCurrentUser() ?: "Anonymous"

        val fullMessage = "$message\n\n📍 Sent from: $location"

        val success = MessageManager.sendMessage(
            from = sender,
            to = recipient,
            message = fullMessage,
            type = MessageType.GREETING
        )

        if (success) {
            Toast.makeText(requireContext(), "✅ Ucapan berhasil dikirim dengan lokasi!", Toast.LENGTH_LONG).show()
            clearForm()
        } else {
            Toast.makeText(requireContext(), "❌ Gagal mengirim ucapan", Toast.LENGTH_SHORT).show()
        }
    }

    // Send greeting WITHOUT location (fallback)
    private fun sendGreetingWithoutLocation() {
        val recipient = binding.actvRecipient.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()
        val sender = UserManager.getCurrentUser() ?: "Anonymous"

        val success = MessageManager.sendMessage(
            from = sender,
            to = recipient,
            message = message,
            type = MessageType.GREETING
        )

        if (success) {
            Toast.makeText(requireContext(), "✅ Ucapan berhasil dikirim!", Toast.LENGTH_LONG).show()
            clearForm()
        } else {
            Toast.makeText(requireContext(), "❌ Gagal mengirim ucapan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearForm() {
        binding.actvRecipient.setText("")
        binding.etMessage.setText("")
        currentLocationString = null
        binding.tvLocationResult.text = "📍 Tidak ada lokasi"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}