package com.ssnexora.claimit.data.repository

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.ssnexora.claimit.domain.repository.ImageStorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

object DriveImageCache {
    private const val TAG = "DriveImageCache"
    // 30MB in-memory LRU bitmap cache
    private val cache = object : LruCache<String, Bitmap>(30 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    private val inFlight = ConcurrentHashMap<String, Deferred<Result<Bitmap>>>()

    fun get(fileId: String): Bitmap? = cache.get(fileId)

    fun put(fileId: String, bitmap: Bitmap) {
        cache.put(fileId, bitmap)
    }

    suspend fun fetchOrGet(
        fileId: String,
        imageStorageRepository: ImageStorageRepository
    ): Result<Bitmap> {
        if (fileId.isBlank()) {
            return Result.failure(Exception("Empty file ID."))
        }

        val cleanFileId = if (fileId.startsWith("drive:")) fileId.substringAfter("drive:") else fileId

        cache.get(cleanFileId)?.let {
            return Result.success(it)
        }

        val deferred = inFlight.computeIfAbsent(cleanFileId) {
            CoroutineScope(Dispatchers.IO).async {
                imageStorageRepository.getImage(cleanFileId)
            }
        }

        val result = try {
            deferred.await()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in inFlight fetch for $cleanFileId: ${e.message}", e)
            Result.failure(e)
        } finally {
            inFlight.remove(cleanFileId)
        }

        result.onSuccess { bitmap ->
            cache.put(cleanFileId, bitmap)
            cache.put(fileId, bitmap)
        }

        return result
    }
}

