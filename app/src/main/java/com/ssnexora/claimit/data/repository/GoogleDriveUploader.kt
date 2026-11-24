package com.ssnexora.claimit.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.ssnexora.claimit.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

data class GoogleDriveUploadResult(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val organizationId: String
)

class GoogleDriveUploader {

    private val TAG = "GoogleDriveUploader"

    suspend fun uploadImage(bitmap: Bitmap): Result<GoogleDriveUploadResult> = withContext(Dispatchers.IO) {
        try {
            val backendUrl = BuildConfig.CLAIMIT_DRIVE_BACKEND_URL
            if (backendUrl.isBlank()) {
                return@withContext Result.failure(
                    Exception("Google Drive backend URL is not configured. Please set CLAIMIT_DRIVE_BACKEND_URL in local.properties.")
                )
            }

            val idToken = getFirebaseIdToken()
            if (idToken.isNullOrBlank()) {
                return@withContext Result.failure(
                    Exception("Authentication required: Unable to obtain Firebase ID token. Please log in.")
                )
            }

            val maxDimension = 1280
            val width = bitmap.width
            val height = bitmap.height
            val max = maxOf(width, height)

            val scaledBitmap = if (max > maxDimension) {
                val scale = maxDimension.toFloat() / max
                Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                bitmap
            }

            var quality = 80
            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            var bytes = baos.toByteArray()

            val maxBytes = 2 * 1024 * 1024
            while (bytes.size > maxBytes && quality > 30) {
                quality -= 10
                baos.reset()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                bytes = baos.toByteArray()
            }

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }

            if (bytes.size > maxBytes) {
                return@withContext Result.failure(Exception("Image exceeds maximum allowed size of 2MB after compression."))
            }

            val imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val requestJson = JSONObject().apply {
                put("action", "upload")
                put("idToken", idToken)
                put("fileName", "item_photo.jpg")
                put("mimeType", "image/jpeg")
                put("imageBase64", imageBase64)
            }

            val responseText = executeHttpPost(backendUrl, requestJson.toString())
            val jsonResponse = JSONObject(responseText)

            val isOk = jsonResponse.optBoolean("ok", false)
            if (isOk) {
                val fileId = jsonResponse.optString("fileId", "")
                val fileName = jsonResponse.optString("fileName", "item_photo.jpg")
                val mimeType = jsonResponse.optString("mimeType", "image/jpeg")
                val sizeBytes = jsonResponse.optLong("sizeBytes", bytes.size.toLong())
                val organizationId = jsonResponse.optString("organizationId", "lpu_main_campus")

                if (fileId.isNotBlank()) {
                    Log.i(TAG, "Google Drive upload success: fileId=$fileId, sizeBytes=$sizeBytes")
                    Result.success(
                        GoogleDriveUploadResult(
                            fileId = fileId,
                            fileName = fileName,
                            mimeType = mimeType,
                            sizeBytes = sizeBytes,
                            organizationId = organizationId
                        )
                    )
                } else {
                    Result.failure(Exception("Google Drive backend returned success but fileId was empty."))
                }
            } else {
                val errorMsg = jsonResponse.optString("error", "Google Drive backend upload failed.")
                Log.e(TAG, "Google Drive upload failed response: $responseText")
                Result.failure(Exception("Google Drive upload failed: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Drive upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getImage(fileId: String): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val backendUrl = BuildConfig.CLAIMIT_DRIVE_BACKEND_URL
            if (backendUrl.isBlank() || fileId.isBlank()) {
                return@withContext Result.failure(Exception("Invalid request: Missing backend URL or file ID."))
            }

            val idToken = getFirebaseIdToken()
            if (idToken.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Authentication required: Missing ID token."))
            }

            val requestJson = JSONObject().apply {
                put("action", "getImage")
                put("idToken", idToken)
                put("fileId", fileId)
            }

            val responseText = executeHttpPost(backendUrl, requestJson.toString())
            val jsonResponse = JSONObject(responseText)

            val isOk = jsonResponse.optBoolean("ok", false)
            if (isOk) {
                val imageBase64 = jsonResponse.optString("imageBase64", "")
                if (imageBase64.isNotBlank()) {
                    val decodedBytes = Base64.decode(imageBase64, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) {
                        Result.success(bitmap)
                    } else {
                        Result.failure(Exception("Failed to decode image bitmap from Google Drive response."))
                    }
                } else {
                    Result.failure(Exception("Google Drive getImage response contained empty imageBase64."))
                }
            } else {
                val errorMsg = jsonResponse.optString("error", "Google Drive getImage failed.")
                Result.failure(Exception("Google Drive getImage failed: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Drive getImage exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteImage(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backendUrl = BuildConfig.CLAIMIT_DRIVE_BACKEND_URL
            if (backendUrl.isBlank() || fileId.isBlank()) {
                return@withContext Result.failure(Exception("Invalid request: Missing backend URL or file ID."))
            }

            val idToken = getFirebaseIdToken()
            if (idToken.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Authentication required: Missing ID token."))
            }

            val requestJson = JSONObject().apply {
                put("action", "deleteImage")
                put("idToken", idToken)
                put("fileId", fileId)
            }

            val responseText = executeHttpPost(backendUrl, requestJson.toString())
            val jsonResponse = JSONObject(responseText)

            val isOk = jsonResponse.optBoolean("ok", false)
            if (isOk) {
                Log.i(TAG, "Google Drive photo deleted successfully: fileId=$fileId")
                Result.success(Unit)
            } else {
                val errorMsg = jsonResponse.optString("error", "Google Drive deleteImage failed.")
                Log.e(TAG, "Google Drive deleteImage failed: $errorMsg")
                Result.failure(Exception("Google Drive deleteImage failed: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Drive deleteImage exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun getFirebaseIdToken(): String? = suspendCancellableCoroutine { continuation ->
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                user.getIdToken(false)
                    .addOnSuccessListener { tokenResult ->
                        if (continuation.isActive) continuation.resume(tokenResult.token)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to obtain Firebase ID token: ${e.message}")
                        if (continuation.isActive) continuation.resume(null)
                    }
            } else {
                if (continuation.isActive) continuation.resume(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception getting Firebase ID token: ${e.message}")
            if (continuation.isActive) continuation.resume(null)
        }
    }

    private fun executeHttpPost(endpointUrl: String, jsonBody: String): String {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpointUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 30000
                readTimeout = 30000
            }

            connection.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            return responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        } finally {
            connection?.disconnect()
        }
    }
}

