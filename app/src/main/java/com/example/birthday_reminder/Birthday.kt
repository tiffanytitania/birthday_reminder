package com.example.birthday_reminder.data.model

data class Birthday(
    val name: String = "",
    val date: String = "",
    val note: String? = null,

    // 🔽 Tambahan baru, tapi opsional (biar gak ganggu Firebase lama)
    val message: String? = null,        // untuk menyimpan ucapan yang mau dikirim
    val qrisUrl: String? = null,        // link gambar QRIS (kalau fitur angpao aktif)
    val photoUrl: String? = null,       // opsional, buat foto profil teman
    val remindDaysBefore: Int? = null   // buat atur notifikasi H-3, H-1, dll
)
