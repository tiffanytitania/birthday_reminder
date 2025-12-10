package com.example.birthday_reminder.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder = Geocoder(context, Locale("id", "ID"))

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getCurrentLocation(
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission()) {
            onError("Location permission not granted")
            return
        }

        try {
            // Gunakan getCurrentLocation dengan priority HIGH_ACCURACY
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("LocationHelper", "Location obtained: ${location.latitude}, ${location.longitude}")
                    onSuccess(location)
                } else {
                    Log.e("LocationHelper", "Location is null, trying lastLocation")
                    // Fallback ke last known location
                    getLastKnownLocation(onSuccess, onError)
                }
            }.addOnFailureListener { exception ->
                Log.e("LocationHelper", "Failed to get location: ${exception.message}")
                // Fallback ke last known location
                getLastKnownLocation(onSuccess, onError)
            }
        } catch (e: SecurityException) {
            Log.e("LocationHelper", "Security exception: ${e.message}")
            onError("Security exception: ${e.message}")
        } catch (e: Exception) {
            Log.e("LocationHelper", "Exception: ${e.message}")
            onError("Error getting location: ${e.message}")
        }
    }

    private fun getLastKnownLocation(
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d("LocationHelper", "Last location obtained: ${location.latitude}, ${location.longitude}")
                        onSuccess(location)
                    } else {
                        Log.e("LocationHelper", "Last location is also null")
                        onError("Unable to get location. Please enable GPS and try again.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("LocationHelper", "Failed to get last location: ${exception.message}")
                    onError("Failed to get location: ${exception.message}")
                }
        } catch (e: SecurityException) {
            onError("Security exception")
        }
    }

    fun getCityName(location: Location): String {
        return try {
            Log.d("LocationHelper", "Getting city name for: ${location.latitude}, ${location.longitude}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+)
                var cityResult = "Unknown Location"
                val listener = object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            cityResult = buildLocationString(address)
                            Log.d("LocationHelper", "City name (API 33+): $cityResult")
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        Log.e("LocationHelper", "Geocoder error: $errorMessage")
                    }
                }

                geocoder.getFromLocation(location.latitude, location.longitude, 1, listener)

                // Tunggu sebentar untuk hasil
                Thread.sleep(1500)
                cityResult
            } else {
                // Android 12 ke bawah
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val result = buildLocationString(address)
                    Log.d("LocationHelper", "City name (legacy): $result")
                    result
                } else {
                    Log.e("LocationHelper", "No addresses found")
                    "Unknown Location"
                }
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error getting city name: ${e.message}", e)
            "Unknown Location"
        }
    }

    private fun buildLocationString(address: Address): String {
        val components = mutableListOf<String>()

        // Coba berbagai kombinasi
        address.subLocality?.let { components.add(it) }
        address.locality?.let { components.add(it) }
        address.subAdminArea?.let {
            if (!components.contains(it)) components.add(it)
        }
        address.adminArea?.let {
            if (!components.contains(it)) components.add(it)
        }

        return if (components.isNotEmpty()) {
            components.joinToString(", ")
        } else {
            address.countryName ?: "Unknown Location"
        }
    }

    fun formatLocation(location: Location): String {
        return try {
            val lat = "%.6f".format(location.latitude)
            val lng = "%.6f".format(location.longitude)
            "$lat, $lng"
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error formatting location: ${e.message}")
            "Unknown Coordinates"
        }
    }
}