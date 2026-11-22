package com.example.claimit.data.repository

import android.graphics.Bitmap
import com.example.claimit.domain.repository.ImageStorageRepository
import com.example.claimit.domain.repository.ImageUploadResult

class ImageStorageRepositoryImpl(
    private val googleDriveUploader: GoogleDriveUploader = GoogleDriveUploader()
) : ImageStorageRepository {

    override suspend fun uploadImage(
        bitmap: Bitmap,
        onProgress: ((Long, Long) -> Unit)?
    ): Result<ImageUploadResult> {
        val result = googleDriveUploader.uploadImage(bitmap)
        return result.map { driveRes ->
            ImageUploadResult(
                imageUrl = "drive:${driveRes.fileId}",
                fileId = driveRes.fileId
            )
        }
    }

    override suspend fun getImage(fileId: String): Result<Bitmap> {
        return googleDriveUploader.getImage(fileId)
    }
}
