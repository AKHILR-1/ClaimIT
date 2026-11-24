package com.ssnexora.claimit.domain.usecase

import com.ssnexora.claimit.domain.model.AIVectorMetadata
import com.ssnexora.claimit.domain.model.CampusLocation
import com.ssnexora.claimit.domain.model.DuplicateMatchResult
import com.ssnexora.claimit.domain.model.ItemCategory
import com.ssnexora.claimit.domain.model.ItemType
import com.ssnexora.claimit.domain.repository.DuplicateDetectionRepository

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

