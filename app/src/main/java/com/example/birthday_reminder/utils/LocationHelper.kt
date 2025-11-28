package com.example.birthday_reminder.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Check apakah location permission sudah granted
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get current location
     */
    fun getCurrentLocation(
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission()) {
            onError("Location permission not granted")
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    onSuccess(location)
                } else {
                    onError("Location not available")
                }
            }.addOnFailureListener { e ->
                onError("Failed to get location: ${e.message}")
            }
        } catch (e: SecurityException) {
            onError("Security exception: ${e.message}")
        }
    }

    /**
     * Format location ke string yang readable
     */
    fun formatLocation(location: Location): String {
        return "Lat: ${String.format("%.4f", location.latitude)}, " +
                "Long: ${String.format("%.4f", location.longitude)}"
    }

    /**
     * Get city name dari koordinat (optional, untuk display yang lebih bagus)
     */
    fun getCityName(location: Location): String {
        return try {
            val geocoder = android.location.Geocoder(context)
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: "Unknown City"
            } else {
                formatLocation(location)
            }
        } catch (e: Exception) {
            formatLocation(location)
        }
    }
}