package com.example.claimit.domain.usecase

import android.graphics.Bitmap
import com.example.claimit.domain.model.AIVectorMetadata
import com.example.claimit.domain.repository.ItemClassificationRepository

class AnalyzeItemImageUseCase(
    private val classificationRepository: ItemClassificationRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<AIVectorMetadata> {
        return classificationRepository.classifyItemImage(bitmap)
    }
}
