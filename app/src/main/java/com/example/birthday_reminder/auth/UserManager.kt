package com.example.birthday_reminder.auth

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    private const val PREF_NAME = "user_prefs"

    // Keys untuk session aktif
    private const val KEY_USERNAME = "current_username"
    private const val KEY_ROLE = "current_role"

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
        }
    }

    // ========================= REGISTER ============================
    fun register(username: String, password: String, role: String = "member"): Boolean {
        if (prefs.contains("$username-password")) return false // username sudah dipakai

        prefs.edit().apply {
            putString("$username-password", password)
            putString("$username-role", role)
            putString("$username-fullname", username)
            putString("$username-birthDate", null)
            putString("$username-phone", null)
            putString("$username-photo", null)
            apply()
        }
        return true
    }

    // ========================= LOGIN ============================
    fun login(username: String, password: String): Boolean {
        val storedPassword = prefs.getString("$username-password", null)
        val storedRole = prefs.getString("$username-role", "member")

        return if (storedPassword == password) {
            prefs.edit().apply {
                putString(KEY_USERNAME, username)
                putString(KEY_ROLE, storedRole)
                apply()
            }
            true
        } else {
            false
        }
    }

    // ========================= SESSION ============================
    fun getCurrentUser(): String? = prefs.getString(KEY_USERNAME, null)
    fun getCurrentRole(): String? = prefs.getString(KEY_ROLE, null)
    fun isLoggedIn(): Boolean = getCurrentUser() != null
    fun isAdmin(): Boolean = getCurrentRole() == "admin"

    fun logout() {
        prefs.edit().apply {
            remove(KEY_USERNAME)
            remove(KEY_ROLE)
            apply()
        }
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
        return true
    }

    // ========================= ROLE CONTROL (opsional) ============================
    fun promoteToAdmin(username: String): Boolean {
        val current = getCurrentUser()
        if (current == null || getCurrentRole() != "admin") return false
        prefs.edit().putString("$username-role", "admin").apply()
        return true
    }

    fun demoteToMember(username: String): Boolean {
        val current = getCurrentUser()
        if (current == null || getCurrentRole() != "admin") return false
        if (username == current) return false // gak bisa demote diri sendiri
        prefs.edit().putString("$username-role", "member").apply()
        return true
    }
}
