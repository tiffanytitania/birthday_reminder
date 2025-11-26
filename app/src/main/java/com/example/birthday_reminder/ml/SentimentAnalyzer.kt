package com.example.birthday_reminder.ml

import android.content.Context

class SentimentAnalyzer(private val context: Context) {

    /**
     * Analyze sentiment dari text greeting
     * Returns: "Positive ✨", "Negative 😢", atau "Neutral 😐"
     */
    fun analyzeSentiment(text: String): String {
        if (text.isEmpty()) return "Neutral 😐"

        // List kata-kata positif
        val positiveWords = listOf(
            "selamat", "bahagia", "sukses", "cinta", "hebat",
            "amazing", "wonderful", "best", "love", "happy",
            "semangat", "mantap", "keren", "bagus", "senang",
            "gembira", "indah", "cantik", "tampan", "pintar"
        )

        // List kata-kata negatif
        val negativeWords = listOf(
            "sedih", "kecewa", "buruk", "gagal", "susah",
            "bad", "sad", "hate", "worst", "angry",
            "marah", "kesal", "sakit", "jelek"
        )

        val lowerText = text.lowercase()

        var positiveCount = 0
        var negativeCount = 0

        // Hitung kata positif
        for (word in positiveWords) {
            if (lowerText.contains(word)) {
                positiveCount++
            }
        }

        // Hitung kata negatif
        for (word in negativeWords) {
            if (lowerText.contains(word)) {
                negativeCount++
            }
        }

        // Tentukan sentiment
        return when {
            positiveCount > negativeCount -> "Positive ✨ (Score: $positiveCount)"
            negativeCount > positiveCount -> "Negative 😢 (Score: $negativeCount)"
            else -> "Neutral 😐"
        }
    }

    /**
     * Get confidence level (0-100%)
     */
    fun getConfidence(text: String): Int {
        val words = text.split(" ").filter { it.isNotBlank() }.size
        return minOf(words * 10, 100)
    }
}