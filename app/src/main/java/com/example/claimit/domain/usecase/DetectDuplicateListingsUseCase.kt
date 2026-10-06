package com.example.claimit.domain.usecase

import com.example.claimit.domain.model.AIVectorMetadata
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.DuplicateMatchResult
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.repository.DuplicateDetectionRepository

class DetectDuplicateListingsUseCase(
    private val duplicateDetectionRepository: DuplicateDetectionRepository
) {
    suspend operator fun invoke(
        type: ItemType,
        category: ItemCategory,
        title: String,
        description: String,
        location: CampusLocation?,
        aiMetadata: AIVectorMetadata?
    ): Result<List<DuplicateMatchResult>> {
        if (title.length < 3) {
            return Result.success(emptyList())
        }
        return duplicateDetectionRepository.detectDuplicates(
            type = type,
            category = category,
            title = title,
            description = description,
            location = location,
            aiMetadata = aiMetadata,
            threshold = 0.72f
        )
    }
}
