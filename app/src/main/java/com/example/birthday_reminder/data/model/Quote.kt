package com.example.birthday_reminder.data.model

import com.squareup.moshi.Json

data class Quote(
    @Json(name = "q")
    val text: String?,
    @Json(name = "a")
    val author: String?,
    @Json(name = "i")
    val image: String?
)
