package com.example.claimit.domain.usecase

import com.example.claimit.domain.model.NotificationItem
import com.example.claimit.domain.model.NotificationType
import com.example.claimit.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

class NotificationUseCases(
    private val notificationRepository: NotificationRepository
) {
    fun getNotificationsForUser(userId: String): Flow<List<NotificationItem>> {
        return notificationRepository.getNotificationsForUser(userId)
    }

    suspend fun markAsRead(userId: String, notificationId: String): Result<Unit> {
        return notificationRepository.markAsRead(userId, notificationId)
    }

    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return notificationRepository.markAllAsRead(userId)
    }

    suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit> {
        return notificationRepository.deleteNotification(userId, notificationId)
    }

    suspend fun sendNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType,
        itemId: String = "",
        claimId: String = "",
        senderUid: String = ""
    ): Result<Unit> {
        return notificationRepository.sendNotification(userId, title, message, type, itemId, claimId, senderUid)
    }
}
