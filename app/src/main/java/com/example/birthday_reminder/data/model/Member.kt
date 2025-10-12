package com.example.birthday_reminder.data.model

/**
 * Data class untuk Member Directory
 */
data class Member(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val birthDate: String = "",
    val profilePhotoUrl: String = "",
    val role: String = "member" // admin atau member
) {
    /**
     * Hitung umur dari tanggal lahir
     */
    fun getAge(): Int? {
        if (birthDate.isEmpty()) return null

        val parts = birthDate.split("/")
        if (parts.size < 3) return null

        val birthYear = parts[2].toIntOrNull() ?: return null
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        return currentYear - birthYear
    }

    /**
     * Format tanggal lahir yang lebih friendly
     */
    fun getFormattedBirthDate(): String {
        if (birthDate.isEmpty()) return "Belum diatur"

        val parts = birthDate.split("/")
        if (parts.size < 3) return birthDate

        val months = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )

        val day = parts[0].toIntOrNull() ?: return birthDate
        val month = parts[1].toIntOrNull() ?: return birthDate
        val year = parts[2]

        if (month < 1 || month > 12) return birthDate

        return "$day ${months[month - 1]} $year"
    }
}