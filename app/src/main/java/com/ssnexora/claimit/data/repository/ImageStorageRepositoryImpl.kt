package com.ssnexora.claimit.data.repository

import android.graphics.Bitmap
import com.ssnexora.claimit.domain.repository.ImageStorageRepository
import com.ssnexora.claimit.domain.repository.ImageUploadResult

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

    override suspend fun deleteImage(fileId: String): Result<Unit> {
        return googleDriveUploader.deleteImage(fileId)
    }
}

