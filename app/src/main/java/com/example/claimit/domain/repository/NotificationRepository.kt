package com.example.claimit.domain.repository

import com.example.claimit.domain.model.NotificationItem
import com.example.claimit.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotificationsForUser(userId: String): Flow<List<NotificationItem>>
    suspend fun markAsRead(userId: String, notificationId: String): Result<Unit>
    suspend fun markAllAsRead(userId: String): Result<Unit>
    suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit>
    suspend fun sendNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType,
        itemId: String = "",
        claimId: String = "",
        senderUid: String = ""
    ): Result<Unit>
    suspend fun broadcastNotificationToOrganization(
        organizationId: String,
        senderUid: String,
        title: String,
        message: String,
        type: NotificationType,
        itemId: String = ""
    ): Result<Unit>
}
