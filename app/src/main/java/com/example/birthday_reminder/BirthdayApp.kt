package com.example.birthday_reminder

import android.app.Application
import com.example.birthday_reminder.auth.UserManager

class BirthdayApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inisialisasi UserManager sekali di awal
        UserManager.init(this)

        // Log untuk debugging
        android.util.Log.d("BirthdayApp", "UserManager initialized")
    }
}