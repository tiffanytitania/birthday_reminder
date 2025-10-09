package com.example.birthday_reminder.auth

object UserManager {
    // Data dummy users (bisa ditambah lebih banyak)
    private val registeredUsers = mutableMapOf(
        "admin" to "admin123",
        "user1" to "password1",
        "demo" to "demo123"
    )

    private var currentUser: String? = null

    // Register user baru
    fun register(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) return false
        if (registeredUsers.containsKey(username)) return false

        registeredUsers[username] = password
        return true
    }

    // Login user
    fun login(username: String, password: String): Boolean {
        val storedPassword = registeredUsers[username]
        if (storedPassword == password) {
            currentUser = username
            return true
        }
        return false
    }

    // Logout
    fun logout() {
        currentUser = null
    }

    // Cek apakah sudah login
    fun isLoggedIn(): Boolean = currentUser != null

    // Get current user
    fun getCurrentUser(): String? = currentUser
}