package com.example.birthday_reminder.data.repository

import android.util.Log
import com.example.birthday_reminder.ui.viewmodel.BirthdayItem
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BirthdayRepository {

    private val database: DatabaseReference = FirebaseDatabase.getInstance(
        "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).reference.child("birthdays")

    fun getAllBirthdays(): Flow<Result<List<BirthdayItem>>> = callbackFlow {
        Log.d("BirthdayRepository", "Starting fetch birthdays...")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val list = mutableListOf<BirthdayItem>()
                    // Efficient iteration
                    snapshot.children.forEach { data ->
                        val key = data.key
                        val name = data.child("name").getValue(String::class.java)
                        val date = data.child("date").getValue(String::class.java)

                        // Skip invalid items tanpa exception
                        if (!key.isNullOrEmpty() && !name.isNullOrEmpty() && !date.isNullOrEmpty()) {
                            list.add(BirthdayItem(key, name, date))
                        }
                    }

                    Log.d("BirthdayRepository", "✅ Loaded ${list.size} birthdays")
                    trySend(Result.success(list))
                } catch (e: Exception) {
                    Log.e("BirthdayRepository", "Error parsing: ${e.message}")
                    trySend(Result.failure(e))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BirthdayRepository", "Database cancelled: ${error.message}")
                trySend(Result.failure(Exception(error.message)))
            }
        }

        // Attach listener (non-blocking)
        database.addValueEventListener(listener)

        awaitClose {
            database.removeEventListener(listener)
        }
    }

    suspend fun addBirthday(name: String, date: String): Result<Unit> {
        return try {
            val birthdayMap = mapOf(
                "name" to name,
                "date" to date,
                "createdAt" to System.currentTimeMillis()
            )
            database.push().setValue(birthdayMap).await()
            Log.d("BirthdayRepository", "✅ Birthday added")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BirthdayRepository", "Add error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateBirthday(key: String, name: String, date: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "date" to date,
                "updatedAt" to System.currentTimeMillis()
            )
            database.child(key).updateChildren(updates).await()
            Log.d("BirthdayRepository", "✅ Birthday updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BirthdayRepository", "Update error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteBirthday(key: String): Result<Unit> {
        return try {
            database.child(key).removeValue().await()
            Log.d("BirthdayRepository", "✅ Birthday deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BirthdayRepository", "Delete error: ${e.message}")
            Result.failure(e)
        }
    }

    fun searchBirthdays(query: String, allBirthdays: List<BirthdayItem>): List<BirthdayItem> {
        if (query.isEmpty()) return allBirthdays
        val lowerQuery = query.lowercase()
        return allBirthdays.filter { it.name.lowercase().contains(lowerQuery) }
    }
}