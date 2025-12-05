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
import kotlinx.coroutines.*

class SendGreetingFragment : Fragment() {
    private var _binding: FragmentSendGreetingBinding? = null
    private val binding get() = _binding!!

    private val membersList = mutableListOf<String>()

    // 🆕 ML Feature - TensorFlow Lite
    private lateinit var sentimentAnalyzer: SentimentAnalyzer
    private val analysisScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Location Feature
    private lateinit var locationHelper: LocationHelper
    private var currentLocationString: String? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLocationAndSend()
        } else {
            Toast.makeText(requireContext(), "⚠️ Location permission denied", Toast.LENGTH_SHORT).show()
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

        // 🆕 Initialize TensorFlow Lite Sentiment Analyzer
        try {
            sentimentAnalyzer = SentimentAnalyzer(requireContext())

            // Safe model info logging (use reflection if methods exist)
            val modelInfo = safeInvokeStringMethod(sentimentAnalyzer, "getModelInfo")
                ?: safeInvokeStringMethod(sentimentAnalyzer, "modelInfo")
                ?: "No model info available"
            android.util.Log.d("SendGreeting", modelInfo)

            val modelReady = safeInvokeBooleanMethod(sentimentAnalyzer, "isModelReady")
                ?: safeInvokeBooleanMethod(sentimentAnalyzer, "modelReady")
                ?: false

            if (modelReady) {
                Toast.makeText(requireContext(), "✅ ML Model Ready!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "⚠️ ML Model fallback mode", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("SendGreeting", "Failed to init ML", e)
            Toast.makeText(requireContext(), "❌ ML initialization failed", Toast.LENGTH_SHORT).show()
        }

        locationHelper = LocationHelper(requireContext())

        loadMembers()
        setupTemplateButton()
        setupSendButton()
        setupSentimentAnalysis()
        setupLocationDisplay()
    }

    private fun loadMembers() {
        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
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
                    checkLocationPermissionAndSend()
                }
            }
        }
    }

    // 🆕 Setup TensorFlow Lite Sentiment Analysis
    private fun setupSentimentAnalysis() {
        binding.etMessage.addTextChangedListener { text ->
            val message = text.toString().trim()

            if (message.isEmpty()) {
                binding.tvSentimentResult.text = "Sentiment: Ketik pesan untuk analisis..."
                binding.tvConfidence.text = "Confidence: -"
            } else {
                // Run ML inference in background thread
                analysisScope.launch {
                    try {
                        val sentiment = sentimentAnalyzer.analyzeSentiment(message)
                        val confidence = sentimentAnalyzer.getConfidence(message)

                        withContext(Dispatchers.Main) {
                            binding.tvSentimentResult.text = "Sentiment: $sentiment"
                            binding.tvConfidence.text = "Confidence: $confidence%"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SendGreeting", "Sentiment analysis error", e)
                        withContext(Dispatchers.Main) {
                            binding.tvSentimentResult.text = "Sentiment: Error"
                            binding.tvConfidence.text = "Confidence: -"
                        }
                    }
                }
            }
        }
    }

    private fun setupLocationDisplay() {
        binding.btnGetLocation.setOnClickListener {
            if (locationHelper.hasLocationPermission()) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun checkLocationPermissionAndSend() {
        if (locationHelper.hasLocationPermission()) {
            getLocationAndSend()
        } else {
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

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

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
                Toast.makeText(requireContext(), "Location error: $error", Toast.LENGTH_SHORT).show()
                sendGreetingWithoutLocation()
            }
        )
    }

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
        binding.tvSentimentResult.text = "Sentiment: Ketik pesan untuk analisis..."
        binding.tvConfidence.text = "Confidence: -"
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // 🆕 Clean up ML resources
        try {
            // Try to call close() if available, otherwise ignore
            safeInvokeVoidMethod(sentimentAnalyzer, "close")
            // cancel analysis scope
            analysisScope.cancel()
        } catch (e: Exception) {
            android.util.Log.e("SendGreeting", "Error cleaning up", e)
        }

        _binding = null
    }

    /**
     * Reflection helpers: attempt to invoke methods if they exist.
     * We don't want compile errors if SentimentAnalyzer doesn't expose these methods.
     */
    private fun safeInvokeStringMethod(target: Any, methodName: String): String? {
        return try {
            val method = target.javaClass.getMethod(methodName)
            val result = method.invoke(target)
            result?.toString()
        } catch (e: NoSuchMethodException) {
            null
        } catch (e: Exception) {
            android.util.Log.w("SendGreeting", "safeInvokeStringMethod error for $methodName", e)
            null
        }
    }

    private fun safeInvokeBooleanMethod(target: Any, methodName: String): Boolean? {
        return try {
            val method = target.javaClass.getMethod(methodName)
            val result = method.invoke(target)
            when (result) {
                is Boolean -> result
                is java.lang.Boolean -> result.booleanValue()
                else -> null
            }
        } catch (e: NoSuchMethodException) {
            null
        } catch (e: Exception) {
            android.util.Log.w("SendGreeting", "safeInvokeBooleanMethod error for $methodName", e)
            null
        }
    }

    private fun safeInvokeVoidMethod(target: Any, methodName: String) {
        try {
            val method = target.javaClass.getMethod(methodName)
            method.invoke(target)
        } catch (e: NoSuchMethodException) {
            // method doesn't exist - that's fine
        } catch (e: Exception) {
            android.util.Log.w("SendGreeting", "safeInvokeVoidMethod error for $methodName", e)
        }
    }
}
