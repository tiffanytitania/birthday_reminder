package com.example.birthday_reminder.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * UserManager dengan persistence dan role management
 * Menggantikan file UserManager.kt yang lama
 */
object UserManager {
    private const val PREF_NAME = "birthday_reminder_prefs"
    private const val KEY_USERNAME = "current_username"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ROLE = "user_role"

    // User roles
    const val ROLE_ADMIN = "admin"
    const val ROLE_USER = "user"

    private lateinit var prefs: SharedPreferences
    private var currentUser: String? = null
    private var currentRole: String? = null

    // Database pengguna (dalam production, gunakan Firebase/Room)
    private val registeredUsers = mutableMapOf(
        "admin" to UserData("admin123", ROLE_ADMIN, "Admin Komunitas", null, null),
        "user1" to UserData("password1", ROLE_USER, "User Demo", null, null),
        "demo" to UserData("demo123", ROLE_USER, "Demo User", null, null)
    )

    /**
     * Inisialisasi SharedPreferences
     * PANGGIL INI DI Application.onCreate() atau MainActivity.onCreate()
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Restore session jika ada
        if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            currentUser = prefs.getString(KEY_USERNAME, null)
            currentRole = prefs.getString(KEY_USER_ROLE, ROLE_USER)
        }
    }

    /**
     * Register user baru
     */
    fun register(username: String, password: String, fullName: String = username): Boolean {
        if (username.isBlank() || password.isBlank()) return false
        if (registeredUsers.containsKey(username)) return false

        // User baru default sebagai ROLE_USER
        registeredUsers[username] = UserData(password, ROLE_USER, fullName, null, null)
        return true
    }

    /**
     * Login user dengan persistence
     */
    fun login(username: String, password: String): Boolean {
        val userData = registeredUsers[username]

        if (userData?.password == password) {
            currentUser = username
            currentRole = userData.role

            // Simpan ke SharedPreferences
            prefs.edit().apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_USERNAME, username)
                putString(KEY_USER_ROLE, userData.role)
                apply()
            }

            return true
        }
        return false
    }

    /**
     * Logout user
     */
    fun logout() {
        currentUser = null
        currentRole = null

        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USERNAME)
            remove(KEY_USER_ROLE)
            apply()
        }
    }

    /**
     * Cek apakah user sudah login
     */
    fun isLoggedIn(): Boolean = currentUser != null

    /**
     * Get username yang sedang login
     */
    fun getCurrentUser(): String? = currentUser

    /**
     * Get role user yang sedang login
     */
    fun getCurrentRole(): String? = currentRole

    /**
     * Cek apakah user adalah admin
     */
    fun isAdmin(): Boolean = currentRole == ROLE_ADMIN

    /**
     * Get user data lengkap
     */
    fun getUserData(username: String = currentUser ?: ""): UserData? {
        return registeredUsers[username]
    }

    /**
     * Update profil user
     */
    fun updateProfile(
        username: String = currentUser ?: "",
        fullName: String? = null,
        photoUrl: String? = null,
        birthDate: String? = null,
        phone: String? = null
    ): Boolean {
        val userData = registeredUsers[username] ?: return false

        registeredUsers[username] = userData.copy(
            fullName = fullName ?: userData.fullName,
            photoUrl = photoUrl ?: userData.photoUrl,
            birthDate = birthDate ?: userData.birthDate,
            phone = phone ?: userData.phone
        )

        return true
    }

    /**
     * Promote user menjadi admin (hanya admin yang bisa)
     */
    fun promoteToAdmin(username: String): Boolean {
        if (!isAdmin()) return false

        val userData = registeredUsers[username] ?: return false
        registeredUsers[username] = userData.copy(role = ROLE_ADMIN)

        return true
    }

    /**
     * Demote admin menjadi user biasa
     */
    fun demoteToUser(username: String): Boolean {
        if (!isAdmin()) return false
        if (username == currentUser) return false // Tidak bisa demote diri sendiri

        val userData = registeredUsers[username] ?: return false
        registeredUsers[username] = userData.copy(role = ROLE_USER)

        return true
    }
}

/**
 * Data class untuk menyimpan informasi user
 */
data class UserData(
    val password: String,
    val role: String,
    val fullName: String,
    val photoUrl: String? = null,
    val birthDate: String? = null,
    val phone: String? = null
)