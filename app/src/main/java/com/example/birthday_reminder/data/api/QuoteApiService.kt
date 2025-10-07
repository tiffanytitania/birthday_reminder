package com.example.birthday_reminder.data.api

import com.example.birthday_reminder.data.model.Quote
import retrofit2.http.GET

interface QuoteApiService {
    @GET("api/quotes")
    suspend fun getQuotes(): List<Quote>

    @GET("api/random")
    suspend fun getRandomQuote(): List<Quote>  // meskipun acak, formatnya array juga
}
