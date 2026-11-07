package com.example.claimit.domain.model

enum class NotificationType {
    NEW_LOST_ITEM,
    NEW_FOUND_ITEM,
    NEW_CLAIM_REQUEST,
    CLAIM_APPROVED,
    CLAIM_REJECTED,
    ITEM_RETURNED,
    ITEM_STATUS_UPDATED,
    AI_MATCH_FOUND,
    CLAIM_RECEIVED,
    SYSTEM_ANNOUNCEMENT
}

data class NotificationItem(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val itemId: String = "",
    val claimId: String = "",
    val senderUid: String = "",
    val targetId: String = itemId.ifEmpty { claimId },
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
