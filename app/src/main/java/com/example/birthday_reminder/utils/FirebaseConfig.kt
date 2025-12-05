package com.example.birthday_reminder.utils

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseConfig {
    const val DATABASE_URL = "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"

    /**
     * Helper function untuk get Database Reference dengan URL yang benar
     */
    fun getDatabaseReference(): DatabaseReference {
        return FirebaseDatabase
            .getInstance(DATABASE_URL)
            .reference
    }
}