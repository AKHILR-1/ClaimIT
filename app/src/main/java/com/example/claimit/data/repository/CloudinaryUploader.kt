package com.example.claimit.data.repository

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class CloudinaryUploadResult(
    val secureUrl: String,
    val publicId: String
)

class CloudinaryUploader(
    private val cloudName: String = "docxcvl6l",
    private val uploadPreset: String = "claimit_unsigned",
    private val folder: String = "claimit/items"
) {
    private val TAG = "CloudinaryUploader"
    private val uploadEndpoint = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

    /**
     * Resizes bitmap to max 1280px dimension, compresses to 80% JPEG quality,
     * enforces max 2MB size cap, and performs multipart unsigned HTTP upload to Cloudinary.
     */
    suspend fun uploadImage(bitmap: Bitmap): Result<CloudinaryUploadResult> = withContext(Dispatchers.IO) {
        try {
            // 1. Image Scaling & Compression (Max 1280px, ~80% quality)
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

            // Enforce max 2MB size cap
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

            // 2. Perform Multipart HTTP Request to Cloudinary
            val boundary = "---CloudinaryBoundary" + UUID.randomUUID().toString().take(12)
            val url = URL(uploadEndpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connectTimeout = 15000
                readTimeout = 20000
            }

            DataOutputStream(connection.outputStream).use { dos ->
                // Field: upload_preset
                dos.writeBytes("--$boundary\r\n")
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                dos.writeBytes("$uploadPreset\r\n")

                // Field: folder
                dos.writeBytes("--$boundary\r\n")
                dos.writeBytes("Content-Disposition: form-data; name=\"folder\"\r\n\r\n")
                dos.writeBytes("$folder\r\n")

                // File Part: file
                dos.writeBytes("--$boundary\r\n")
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"item_photo.jpg\"\r\n")
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                dos.write(bytes)
                dos.writeBytes("\r\n")

                // End of multipart
                dos.writeBytes("--$boundary--\r\n")
                dos.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = responseStream.bufferedReader().use { it.readText() }

            if (responseCode in 200..299) {
                val json = JSONObject(responseText)
                val secureUrl = json.getString("secure_url")
                val publicId = json.getString("public_id")
                Log.i(TAG, "Cloudinary upload success: $secureUrl (Public ID: $publicId)")
                Result.success(CloudinaryUploadResult(secureUrl = secureUrl, publicId = publicId))
            } else {
                Log.e(TAG, "Cloudinary HTTP $responseCode error: $responseText")
                val errorMsg = try {
                    JSONObject(responseText).getJSONObject("error").getString("message")
                } catch (_: Exception) {
                    "Cloudinary HTTP error $responseCode"
                }
                Result.failure(Exception("Cloudinary upload failed: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
