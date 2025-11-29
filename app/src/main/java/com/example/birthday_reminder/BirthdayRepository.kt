package com.example.birthday_reminder.data.repository

import com.example.birthday_reminder.BirthdayItem
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk handle semua operasi database Birthday
 * Single source of truth untuk data birthday
 */
class BirthdayRepository {

    private val database: DatabaseReference = FirebaseDatabase.getInstance(
        "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).reference.child("birthdays")

    /**
     * Get all birthdays as Flow (real-time updates)
     */
    fun getAllBirthdays(): Flow<Result<List<BirthdayItem>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BirthdayItem>()
                for (data in snapshot.children) {
                    try {
                        val name = data.child("name").getValue(String::class.java) ?: ""
                        val date = data.child("date").getValue(String::class.java) ?: ""
                        val key = data.key ?: ""
                        if (name.isNotEmpty() && date.isNotEmpty()) {
                            list.add(BirthdayItem(key, name, date))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                trySend(Result.success(list))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(Exception(error.message)))
            }
        }

        database.addValueEventListener(listener)

        awaitClose {
            database.removeEventListener(listener)
        }
    }

    /**
     * Add new birthday
     */
    suspend fun addBirthday(name: String, date: String): Result<Unit> {
        return try {
            val birthdayMap = mapOf(
                "name" to name,
                "date" to date
            )
            database.push().setValue(birthdayMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update existing birthday
     */
    suspend fun updateBirthday(key: String, name: String, date: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "date" to date
            )
            database.child(key).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete birthday
     */
    suspend fun deleteBirthday(key: String): Result<Unit> {
        return try {
            database.child(key).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search birthdays by name
     */
    fun searchBirthdays(query: String, allBirthdays: List<BirthdayItem>): List<BirthdayItem> {
        if (query.isEmpty()) return allBirthdays
        return allBirthdays.filter {
            it.name.lowercase().contains(query.lowercase())
        }
    }
}