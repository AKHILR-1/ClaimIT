package com.ssnexora.claimit.domain.repository

import com.ssnexora.claimit.domain.model.AIVectorMetadata
import com.ssnexora.claimit.domain.model.CampusLocation
import com.ssnexora.claimit.domain.model.DuplicateMatchResult
import com.ssnexora.claimit.domain.model.ItemCategory
import com.ssnexora.claimit.domain.model.ItemType

interface DuplicateDetectionRepository {
    suspend fun detectDuplicates(
        type: ItemType,
        category: ItemCategory,
        title: String,
        description: String,
        location: CampusLocation?,
        aiMetadata: AIVectorMetadata?,
        threshold: Float = 0.72f,
        weightVector: Float = 0.50f,
        weightLocation: Float = 0.30f,
        weightTime: Float = 0.20f
    ): Result<List<DuplicateMatchResult>>
}

