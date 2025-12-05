package com.example.birthday_reminder.utils

object ImageKitConfig {
    const val URL_ENDPOINT = "https://ik.imagekit.io/feliciaaaa/"

    const val PUBLIC_KEY = "public_n/BpAbR0Ntk5CR5CxsIIFoRlLvs="
    const val PRIVATE_KEY = "private_Q4xM2TBsXH7zl8akDrBw7dFYpqs="

    // Upload endpoint ImageKit
    const val UPLOAD_ENDPOINT = "https://upload.imagekit.io/api/v1/files/upload"

    // Folder structure
    const val PROFILE_FOLDER = "profiles/"
    const val BANNER_FOLDER = "banners/"
    const val BIRTHDAY_FOLDER = "birthdays/"

    fun getTransformedUrl(
        imageUrl: String,
        width: Int? = null,
        height: Int? = null,
        quality: Int = 80
    ): String {
        if (!imageUrl.startsWith(URL_ENDPOINT)) return imageUrl

        val transformations = mutableListOf<String>()
        width?.let { transformations.add("w-$it") }
        height?.let { transformations.add("h-$it") }
        transformations.add("q-$quality")

        val transformation = transformations.joinToString(",")

        // Extract path after URL_ENDPOINT
        val path = imageUrl.removePrefix(URL_ENDPOINT)

        return "${URL_ENDPOINT}tr:$transformation/$path"
    }

    /**
     * Get thumbnail URL (150x150)
     */
    fun getThumbnailUrl(imageUrl: String): String {
        return getTransformedUrl(imageUrl, width = 150, height = 150, quality = 70)
    }

    /**
     * Get medium size URL (500x500)
     */
    fun getMediumUrl(imageUrl: String): String {
        return getTransformedUrl(imageUrl, width = 500, height = 500, quality = 80)
    }
}