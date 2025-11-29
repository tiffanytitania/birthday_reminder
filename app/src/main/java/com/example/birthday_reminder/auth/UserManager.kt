package com.example.birthday_reminder.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object UserManager {
    private const val PREF_NAME = "user_prefs"
    private const val TAG = "UserManager"

    // Keys untuk session aktif
    private const val KEY_USERNAME = "current_username"
    private const val KEY_ROLE = "current_role"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private lateinit var prefs: SharedPreferences

    data class UserData(
        val username: String,
        val fullName: String,
        val birthDate: String?,
        val phone: String?,
        val photoUrl: String?,
        val role: String
    )

    // ========================= INIT ============================
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "✅ UserManager initialized")

        // ✅ Buat akun admin default kalau belum ada
        if (!prefs.contains("admin-password")) {
            prefs.edit().apply {
                putString("admin-password", "admin123")
                putString("admin-role", "admin")
                putString("admin-fullname", "Administrator")
                putString("admin-birthDate", null)
                putString("admin-phone", null)
                putString("admin-photo", null)
                apply()
            }
            Log.d(TAG, "✅ Default admin account created")
        }

        // Debug: Log semua users yang ada
        debugPrintAllUsers()
    }

    // ========================= REGISTER ============================
    fun register(username: String, password: String, role: String = "member"): Boolean {
        if (prefs.contains("$username-password")) {
            Log.d(TAG, "❌ Username already exists: $username")
            return false
        }

        prefs.edit().apply {
            putString("$username-password", password)
            putString("$username-role", role)
            putString("$username-fullname", username)
            putString("$username-birthDate", null)
            putString("$username-phone", null)
            putString("$username-photo", null)
            apply()
        }

        Log.d(TAG, "✅ User registered: $username (role: $role)")
        return true
    }

    // ========================= LOGIN ============================
    fun login(username: String, password: String): Boolean {
        Log.d(TAG, "🔐 Login attempt for: $username")

        val storedPassword = prefs.getString("$username-password", null)
        val storedRole = prefs.getString("$username-role", "member")

        Log.d(TAG, "Stored password: ${if (storedPassword != null) "EXISTS" else "NULL"}")
        Log.d(TAG, "Stored role: $storedRole")
        Log.d(TAG, "Input password matches: ${storedPassword == password}")

        return if (storedPassword == password) {
            prefs.edit().apply {
                putString(KEY_USERNAME, username)
                putString(KEY_ROLE, storedRole)
                putBoolean(KEY_IS_LOGGED_IN, true)
                apply()
            }
            Log.d(TAG, "✅ Login successful for: $username (role: $storedRole)")
            true
        } else {
            Log.d(TAG, "❌ Login failed for: $username")
            false
        }
    }

    // ========================= SESSION ============================
    fun getCurrentUser(): String? {
        val user = prefs.getString(KEY_USERNAME, null)
        Log.d(TAG, "Current user: ${user ?: "none"}")
        return user
    }

    fun getCurrentRole(): String? {
        val role = prefs.getString(KEY_ROLE, null)
        Log.d(TAG, "Current role: ${role ?: "none"}")
        return role
    }

    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getCurrentUser() != null
        Log.d(TAG, "Is logged in: $loggedIn")
        return loggedIn
    }

    fun isAdmin(): Boolean {
        val role = getCurrentRole()
        val admin = role == "admin"
        Log.d(TAG, "Is admin: $admin (role: $role)")
        return admin
    }

    fun logout() {
        prefs.edit().apply {
            remove(KEY_USERNAME)
            remove(KEY_ROLE)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
        Log.d(TAG, "✅ User logged out")
    }

    // ========================= PROFILE ============================
    fun getUserData(username: String? = null): UserData? {
        val finalUsername = username ?: getCurrentUser() ?: return null

        val fullName = prefs.getString("$finalUsername-fullname", null) ?: return null
        val birthDate = prefs.getString("$finalUsername-birthDate", null)
        val phone = prefs.getString("$finalUsername-phone", null)
        val photo = prefs.getString("$finalUsername-photo", null)
        val role = prefs.getString("$finalUsername-role", "member") ?: "member"

        return UserData(finalUsername, fullName, birthDate, phone, photo, role)
    }

    fun updateProfile(
        fullName: String,
        birthDate: String?,
        phone: String?,
        photoUrl: String?
    ): Boolean {
        val username = getCurrentUser() ?: return false
        prefs.edit().apply {
            putString("$username-fullname", fullName)
            putString("$username-birthDate", birthDate)
            putString("$username-phone", phone)
            putString("$username-photo", photoUrl)
            apply()
        }
        Log.d(TAG, "✅ Profile updated for: $username")
        return true
    }

    // ========================= DEBUG ============================
    private fun debugPrintAllUsers() {
        val allKeys = prefs.all.keys
        Log.d(TAG, "=== ALL USERS IN SYSTEM ===")

        val users = allKeys.filter { it.endsWith("-password") }
            .map { it.removeSuffix("-password") }

        users.forEach { username ->
            val role = prefs.getString("$username-role", "unknown")
            Log.d(TAG, "User: $username | Role: $role")
        }

        Log.d(TAG, "=== END OF USER LIST ===")
    }

    // ========================= RESET (FOR DEBUGGING) ============================
    fun resetAllData(context: Context) {
        prefs.edit().clear().apply()
        Log.d(TAG, "⚠️ ALL USER DATA CLEARED!")
        init(context)
    }
}