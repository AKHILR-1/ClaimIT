package com.example.claimit.domain.repository

import android.graphics.Bitmap

data class ImageUploadResult(
    val imageUrl: String,
    val fileId: String = ""
)

interface ImageStorageRepository {
    suspend fun uploadImage(
        bitmap: Bitmap,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<ImageUploadResult>

    suspend fun getImage(fileId: String): Result<Bitmap>
}
