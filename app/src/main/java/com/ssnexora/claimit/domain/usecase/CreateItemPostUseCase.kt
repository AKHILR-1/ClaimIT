package com.ssnexora.claimit.domain.usecase

import com.ssnexora.claimit.domain.model.AIVectorMetadata
import com.ssnexora.claimit.domain.model.CampusLocation
import com.ssnexora.claimit.domain.model.ItemCategory
import com.ssnexora.claimit.domain.model.ItemPost
import com.ssnexora.claimit.domain.model.ItemType
import com.ssnexora.claimit.domain.model.PhysicalAttributes
import com.ssnexora.claimit.domain.model.PostStatus
import com.ssnexora.claimit.domain.model.NotificationType
import com.ssnexora.claimit.domain.repository.ItemRepository
import com.ssnexora.claimit.domain.repository.NotificationRepository
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
        imageFileIds: List<String> = emptyList(),
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
        if (imageUrls.isEmpty() && imageFileIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please upload at least one photograph of the item before publishing your report."))
        }

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
            imageFileIds = imageFileIds,
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

