package com.example.birthday_reminder.data.model

data class Birthday(
    val name: String = "",
    val date: String = "",
    val note: String? = null,

    val message: String? = null,        // untuk menyimpan ucapan yang mau dikirim
    val photoUrl: String? = null,       // opsional, buat foto profil teman
    val remindDaysBefore: Int? = null   // buat atur notifikasi H-3, H-1, dll
)
