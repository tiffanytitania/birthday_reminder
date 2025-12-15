package com.example.birthday_reminder

import android.Manifest
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import java.util.Locale

class SendGreetingFragment : Fragment() {
    private var _binding: FragmentSendGreetingBinding? = null
    private val binding get() = _binding!!

    private val membersList = mutableListOf<String>()

    // ML Feature
    private lateinit var sentimentAnalyzer: SentimentAnalyzer
    private val analysisScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Location Feature
    private lateinit var locationHelper: LocationHelper
    private lateinit var geocoder: Geocoder
    private var currentLocationString: String? = null

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentCityName: String? = null

    // Flag untuk tracking location request
    private var isLocationLoading = false

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

        geocoder = Geocoder(requireContext(), Locale("id", "ID"))

        // Initialize ML
        try {
            sentimentAnalyzer = SentimentAnalyzer(requireContext())

            val modelReady = try {
                val method = sentimentAnalyzer.javaClass.getMethod("isModelReady")
                method.invoke(sentimentAnalyzer) as? Boolean ?: false
            } catch (e: Exception) {
                false
            }

            if (modelReady) {
                Toast.makeText(requireContext(), "✅ ML Model Ready!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SendGreeting", "Failed to init ML", e)
        }

        locationHelper = LocationHelper(requireContext())

        loadMembers()
        setupTemplateButton()
        setupSendButton()
        setupSentimentAnalysis()
        setupLocationDisplay()
        setupGoogleMapsButton()
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

    private fun setupSentimentAnalysis() {
        binding.etMessage.addTextChangedListener { text ->
            val message = text.toString().trim()

            if (message.isEmpty()) {
                binding.tvSentimentResult.text = "Sentiment: Ketik pesan untuk analisis..."
                binding.tvConfidence.text = "Confidence: -"
            } else {
                analysisScope.launch {
                    try {
                        val sentiment = sentimentAnalyzer.analyzeSentiment(message)
                        val confidence = sentimentAnalyzer.getConfidence(message)

                        withContext(Dispatchers.Main) {
                            binding.tvSentimentResult.text = "Sentiment: $sentiment"
                            binding.tvConfidence.text = "Confidence: $confidence%"
                        }
                    } catch (e: Exception) {
                        Log.e("SendGreeting", "Sentiment analysis error", e)
                    }
                }
            }
        }
    }

    private fun setupLocationDisplay() {
        binding.btnGetLocation.setOnClickListener {
            if (isLocationLoading) {
                Toast.makeText(requireContext(), "⏳ Tunggu sebentar...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (locationHelper.hasLocationPermission()) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun setupGoogleMapsButton() {
        binding.btnOpenMaps.setOnClickListener {
            openGoogleMaps()
        }
    }

    private fun openGoogleMaps() {
        if (currentLatitude == null || currentLongitude == null) {
            Toast.makeText(requireContext(), "Lokasi belum tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val label = "Lokasi Saya"
            val gmmIntentUri = Uri.parse("geo:$currentLatitude,$currentLongitude?q=$currentLatitude,$currentLongitude($label)")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$currentLatitude,$currentLongitude")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka Maps: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("SendGreeting", "Error opening maps", e)
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
        if (isLocationLoading) return

        isLocationLoading = true
        binding.progressLocation.visibility = View.VISIBLE
        binding.tvLocationResult.text = "Getting location..."
        binding.btnOpenMaps.visibility = View.GONE
        binding.btnGetLocation.isEnabled = false

        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                Log.d("SendGreeting", "Location success: ${location.latitude}, ${location.longitude}")
                currentLatitude = location.latitude
                currentLongitude = location.longitude
                currentLocationString = locationHelper.formatLocation(location)

                // Fetch city name dengan Geocoder
                fetchCityName(location)
            },
            onError = { error ->
                Log.e("SendGreeting", "Location error: $error")
                isLocationLoading = false
                binding.progressLocation.visibility = View.GONE
                binding.tvLocationResult.text = "❌ Failed: $error"
                binding.btnOpenMaps.visibility = View.GONE
                binding.btnGetLocation.isEnabled = true

                Toast.makeText(requireContext(), "⚠️ $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun fetchCityName(location: Location) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("SendGreeting", "Fetching city name for: ${location.latitude}, ${location.longitude}")

                val cityName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var result: String? = null
                    val latch = java.util.concurrent.CountDownLatch(1)

                    withContext(Dispatchers.Main) {
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    result = if (addresses.isNotEmpty()) {
                                        buildLocationString(addresses[0])
                                    } else {
                                        "Unknown Location"
                                    }
                                    latch.countDown()
                                }

                                override fun onError(errorMessage: String?) {
                                    Log.e("SendGreeting", "Geocoder error: $errorMessage")
                                    result = "Unknown Location"
                                    latch.countDown()
                                }
                            }
                        )
                    }

                    // Tunggu max 5 detik
                    latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                    result ?: "Unknown Location"

                } else {
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )

                        if (!addresses.isNullOrEmpty()) {
                            buildLocationString(addresses[0])
                        } else {
                            "Unknown Location"
                        }
                    } catch (e: Exception) {
                        Log.e("SendGreeting", "Geocoder error: ${e.message}")
                        "Unknown Location"
                    }
                }

                Log.d("SendGreeting", "City name resolved: $cityName")

                withContext(Dispatchers.Main) {
                    updateLocationUI(cityName)
                }

            } catch (e: Exception) {
                Log.e("SendGreeting", "Error fetching city name", e)
                withContext(Dispatchers.Main) {
                    updateLocationUI("Unknown Location")
                }
            }
        }
    }

    private fun buildLocationString(address: Address): String {
        val components = mutableListOf<String>()

        address.subLocality?.let { components.add(it) }
        address.locality?.let { components.add(it) }
        address.subAdminArea?.let {
            if (!components.contains(it)) components.add(it)
        }
        address.adminArea?.let {
            if (!components.contains(it)) components.add(it)
        }

        Log.d("SendGreeting", "Address components: $components")

        return if (components.isNotEmpty()) {
            components.take(2).joinToString(", ") // Ambil max 2 components
        } else {
            address.countryName ?: "Unknown Location"
        }
    }

    private fun updateLocationUI(cityName: String) {
        isLocationLoading = false
        currentCityName = cityName
        binding.progressLocation.visibility = View.GONE
        binding.tvLocationResult.text = "📍 $cityName\n$currentLocationString"
        binding.btnOpenMaps.visibility = View.VISIBLE
        binding.btnGetLocation.isEnabled = true
        Toast.makeText(requireContext(), "✅ Location obtained!", Toast.LENGTH_SHORT).show()
    }

    private fun getLocationAndSend() {
        if (isLocationLoading) return

        isLocationLoading = true
        binding.progressLocation.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false

        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                currentLatitude = location.latitude
                currentLongitude = location.longitude
                currentLocationString = locationHelper.formatLocation(location)

                // Fetch city name dulu sebelum kirim
                fetchCityNameAndSend(location)
            },
            onError = { error ->
                isLocationLoading = false
                binding.progressLocation.visibility = View.GONE
                binding.btnSend.isEnabled = true
                Toast.makeText(requireContext(), "Location error: $error", Toast.LENGTH_SHORT).show()
                sendGreetingWithoutLocation()
            }
        )
    }

    private fun fetchCityNameAndSend(location: Location) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cityName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var result: String? = null
                    val latch = java.util.concurrent.CountDownLatch(1)

                    withContext(Dispatchers.Main) {
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    result = if (addresses.isNotEmpty()) {
                                        buildLocationString(addresses[0])
                                    } else {
                                        "Unknown Location"
                                    }
                                    latch.countDown()
                                }

                                override fun onError(errorMessage: String?) {
                                    result = "Unknown Location"
                                    latch.countDown()
                                }
                            }
                        )
                    }

                    latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                    result ?: "Unknown Location"

                } else {
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )

                        if (!addresses.isNullOrEmpty()) {
                            buildLocationString(addresses[0])
                        } else {
                            "Unknown Location"
                        }
                    } catch (e: Exception) {
                        "Unknown Location"
                    }
                }

                withContext(Dispatchers.Main) {
                    isLocationLoading = false
                    binding.progressLocation.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                    sendGreetingWithLocation("$cityName ($currentLocationString)")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLocationLoading = false
                    binding.progressLocation.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                    sendGreetingWithLocation("Unknown Location ($currentLocationString)")
                }
            }
        }
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
        currentLatitude = null
        currentLongitude = null
        currentCityName = null
        binding.tvLocationResult.text = "📍 Tidak ada lokasi"
        binding.btnOpenMaps.visibility = View.GONE
        binding.tvSentimentResult.text = "Sentiment: Ketik pesan untuk analisis..."
        binding.tvConfidence.text = "Confidence: -"
        binding.btnGetLocation.isEnabled = true
        binding.btnSend.isEnabled = true
        isLocationLoading = false
    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            val method = sentimentAnalyzer.javaClass.getMethod("close")
            method.invoke(sentimentAnalyzer)
        } catch (e: Exception) {
            // Ignore
        }

        analysisScope.cancel()
        _binding = null
    }
}