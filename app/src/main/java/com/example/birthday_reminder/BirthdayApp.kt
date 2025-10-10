package com.example.birthday_reminder

import android.app.Application
import com.example.birthday_reminder.auth.UserManager

/**
 * Application class untuk inisialisasi global
 * UserManager.init() akan dipanggil SEKALI saat app pertama kali dibuka
 *
 * JANGAN LUPA REGISTER DI AndroidManifest.xml!
 */
class BirthdayApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inisialisasi UserManager sekali di awal
        UserManager.init(this)

        // Log untuk debugging
        android.util.Log.d("BirthdayApp", "UserManager initialized")
    }
}