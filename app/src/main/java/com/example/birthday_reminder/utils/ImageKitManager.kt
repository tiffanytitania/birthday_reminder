package com.example.birthday_reminder.utils

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * ImageKit Upload Manager
 * Handle upload gambar ke ImageKit.io
 */
object ImageKitManager {
    private const val TAG = "ImageKitManager"

    private val client = OkHttpClient.Builder()
        .build()

    /**
     * Upload image dari URI (Gallery/Camera)
     *
     * @param context Android Context
     * @param imageUri URI gambar
     * @param folder Folder di ImageKit (profiles/, banners/, dll)
     * @param fileName Nama file (opsional, auto-generate jika null)
     * @return URL gambar yang berhasil diupload, atau null jika gagal
     */
    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        folder: String = ImageKitConfig.PROFILE_FOLDER,
        fileName: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Starting upload: $imageUri")
            Log.d(TAG, "📁 Target folder: $folder")

            // 1. Convert URI to byte array
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null) {
                Log.e(TAG, "❌ Failed to read image bytes")
                return@withContext null
            }

            Log.d(TAG, "✅ Image bytes read: ${bytes.size} bytes")

            // 2. Encode to Base64
            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Log.d(TAG, "✅ Image encoded to Base64")

            // 3. Generate filename
            val finalFileName = fileName ?: "img_${System.currentTimeMillis()}.jpg"
            Log.d(TAG, "📝 Filename: $finalFileName")

            // 4. Create request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", base64Image)
                .addFormDataPart("fileName", finalFileName)
                .addFormDataPart("folder", folder)
                .addFormDataPart("useUniqueFileName", "true")
                .build()

            // 5. Create authentication header
            val authString = "${ImageKitConfig.PRIVATE_KEY}:"
            val authHeader = "Basic " + Base64.encodeToString(
                authString.toByteArray(),
                Base64.NO_WRAP
            )

            // 6. Build request
            val request = Request.Builder()
                .url(ImageKitConfig.UPLOAD_ENDPOINT)
                .addHeader("Authorization", authHeader)
                .post(requestBody)
                .build()

            Log.d(TAG, "🚀 Sending request to ImageKit...")

            // 7. Execute upload
            val response = client.newCall(request).execute()

            return@withContext if (response.isSuccessful) {
                // 8. Parse response
                val responseBody = response.body?.string()
                Log.d(TAG, "📤 Upload response: $responseBody")

                val json = JSONObject(responseBody ?: "{}")
                val imageUrl = json.optString("url", "")
                val fileId = json.optString("fileId", "")

                if (imageUrl.isEmpty()) {
                    Log.e(TAG, "❌ No URL in response")
                    null
                } else {
                    Log.d(TAG, "✅ Upload success!")
                    Log.d(TAG, "🔗 Image URL: $imageUrl")
                    Log.d(TAG, "🆔 File ID: $fileId")
                    imageUrl
                }
            } else {
                Log.e(TAG, "❌ Upload failed: ${response.code} - ${response.message}")
                val errorBody = response.body?.string()
                Log.e(TAG, "❌ Error response: $errorBody")
                null
            }

        } catch (e: IOException) {
            Log.e(TAG, "❌ Network error", e)
            e.printStackTrace()
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload error", e)
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Upload dari File
     */
    suspend fun uploadImageFromFile(
        file: File,
        folder: String = ImageKitConfig.PROFILE_FOLDER,
        fileName: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Uploading file: ${file.name}")

            val bytes = file.readBytes()
            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val finalFileName = fileName ?: file.name

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", base64Image)
                .addFormDataPart("fileName", finalFileName)
                .addFormDataPart("folder", folder)
                .addFormDataPart("useUniqueFileName", "true")
                .build()

            val authString = "${ImageKitConfig.PRIVATE_KEY}:"
            val authHeader = "Basic " + Base64.encodeToString(
                authString.toByteArray(),
                Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url(ImageKitConfig.UPLOAD_ENDPOINT)
                .addHeader("Authorization", authHeader)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            return@withContext if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "{}")
                val imageUrl = json.optString("url", "")

                if (imageUrl.isEmpty()) {
                    Log.e(TAG, "❌ No URL in response")
                    null
                } else {
                    Log.d(TAG, "✅ Upload success: $imageUrl")
                    imageUrl
                }
            } else {
                Log.e(TAG, "❌ Upload failed: ${response.code}")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload error", e)
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Delete image dari ImageKit
     */
    suspend fun deleteImage(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🗑️ Deleting file: $fileId")

            val authString = "${ImageKitConfig.PRIVATE_KEY}:"
            val authHeader = "Basic " + Base64.encodeToString(
                authString.toByteArray(),
                Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url("https://api.imagekit.io/v1/files/$fileId")
                .addHeader("Authorization", authHeader)
                .delete()
                .build()

            val response = client.newCall(request).execute()

            return@withContext if (response.isSuccessful) {
                Log.d(TAG, "✅ Delete success")
                true
            } else {
                Log.e(TAG, "❌ Delete failed: ${response.code}")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete error", e)
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Test connection to ImageKit
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Testing ImageKit connection...")

            val authString = "${ImageKitConfig.PRIVATE_KEY}:"
            val authHeader = "Basic " + Base64.encodeToString(
                authString.toByteArray(),
                Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url("https://api.imagekit.io/v1/files")
                .addHeader("Authorization", authHeader)
                .get()
                .build()

            val response = client.newCall(request).execute()

            return@withContext if (response.isSuccessful) {
                Log.d(TAG, "✅ Connection OK")
                true
            } else {
                Log.e(TAG, "❌ Connection failed: ${response.code}")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Connection error", e)
            return@withContext false
        }
    }
}