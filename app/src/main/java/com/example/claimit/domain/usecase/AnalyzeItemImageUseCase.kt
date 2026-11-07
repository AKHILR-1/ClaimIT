package com.example.claimit.domain.usecase

import android.graphics.Bitmap
import com.example.claimit.domain.model.AIVectorMetadata
import com.example.claimit.domain.repository.ItemClassificationRepository

class AnalyzeItemImageUseCase(
    private val itemClassificationRepository: ItemClassificationRepository
) {
    suspend operator fun invoke(bitmap: Bitmap?): Result<AIVectorMetadata> {
        return itemClassificationRepository.classifyItemImage(bitmap)
    }
}
