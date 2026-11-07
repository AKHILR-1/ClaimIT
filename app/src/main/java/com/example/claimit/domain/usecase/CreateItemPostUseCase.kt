package com.example.claimit.domain.usecase

import com.example.claimit.domain.model.AIVectorMetadata
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.model.NotificationType
import com.example.claimit.domain.repository.ItemRepository
import com.example.claimit.domain.repository.NotificationRepository
import java.util.UUID

class CreateItemPostUseCase(
    private val itemRepository: ItemRepository,
    private val notificationRepository: NotificationRepository? = null
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
        actionTakenRemarks: String = "",
        userId: String = "user_student_1",
        userDisplayName: String = "Campus Member",
        organizationId: String = "lpu_main_campus",
        privateVerificationQuestion: String = "",
        privateVerificationAnswer: String = ""
    ): Result<String> {
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be empty"))
        if (contactInfo.isBlank()) return Result.failure(IllegalArgumentException("Contact info cannot be empty"))

        val uuid = UUID.randomUUID().toString()
        val trackingCode = "CLM-" + uuid.take(6).uppercase()

        val post = ItemPost(
            id = "post_$uuid",
            trackingCode = trackingCode,
            organizationId = organizationId,
            userId = userId,
            userDisplayName = userDisplayName,
            title = title.trim(),
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
            actionTakenRemarks = actionTakenRemarks.trim(),
            privateVerificationQuestion = privateVerificationQuestion.trim(),
            privateVerificationAnswer = privateVerificationAnswer.trim()
        )

        val result = itemRepository.createItemPost(post)
        if (result.isSuccess) {
            try {
                notificationRepository?.broadcastNotificationToOrganization(
                    organizationId = organizationId,
                    senderUid = userId,
                    title = if (type == ItemType.FOUND) "New Found Item" else "New Lost Item",
                    message = "A new ${category.displayName} item '${title.trim()}' has been reported in your college. Open ClaimIT to view details.",
                    type = if (type == ItemType.FOUND) NotificationType.NEW_FOUND_ITEM else NotificationType.NEW_LOST_ITEM,
                    itemId = post.id
                )
            } catch (_: Exception) {}
        }
        return result
    }
}
