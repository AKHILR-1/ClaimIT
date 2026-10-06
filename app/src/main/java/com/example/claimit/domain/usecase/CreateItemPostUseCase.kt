package com.example.claimit.domain.usecase

import com.example.claimit.domain.model.AIVectorMetadata
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.repository.ItemRepository
import java.util.UUID

class CreateItemPostUseCase(
    private val itemRepository: ItemRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        type: ItemType,
        category: ItemCategory,
        location: CampusLocation,
        physicalAttributes: PhysicalAttributes,
        aiMetadata: AIVectorMetadata?,
        acceptedTags: List<String>,
        imageUrls: List<String>,
        contactInfo: String,
        actionTakenRemarks: String = ""
    ): Result<String> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Item title cannot be empty."))
        }
        if (contactInfo.isBlank()) {
            return Result.failure(IllegalArgumentException("Contact info is required to verify ownership or coordinate item recovery."))
        }

        val trackingCode = "CLM-" + UUID.randomUUID().toString().take(8).uppercase()
        val newPost = ItemPost(
            id = UUID.randomUUID().toString(),
            trackingCode = trackingCode,
            title = trimmedTitle,
            description = description.trim(),
            type = type,
            category = category,
            location = location,
            physicalAttributes = physicalAttributes,
            aiMetadata = aiMetadata,
            acceptedTags = acceptedTags,
            imageUrls = imageUrls,
            createdAtTimestamp = System.currentTimeMillis(),
            status = PostStatus.ACTIVE,
            finderOrOwnerContact = contactInfo.trim(),
            actionTakenRemarks = actionTakenRemarks.trim()
        )

        return itemRepository.createItemPost(newPost)
    }
}
