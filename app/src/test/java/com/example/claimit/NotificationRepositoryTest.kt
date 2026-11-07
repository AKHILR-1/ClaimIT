package com.example.claimit

import com.example.claimit.data.repository.NotificationRepositoryImpl
import com.example.claimit.domain.model.NotificationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationRepositoryTest {

    private lateinit var notificationRepository: NotificationRepositoryImpl
    private val dispatchers = TestCoroutineDispatchers()

    @Before
    fun setup() {
        notificationRepository = NotificationRepositoryImpl(dispatchers)
    }

    @Test
    fun `sendNotification adds notification for user`() = runBlocking {
        val result = notificationRepository.sendNotification(
            userId = "user_student_1",
            title = "New Found Item",
            message = "A new wallet was found in Uni Mall.",
            type = NotificationType.NEW_FOUND_ITEM,
            itemId = "post_wallet_1"
        )

        assertTrue(result.isSuccess)
        val notifications = notificationRepository.getNotificationsForUser("user_student_1").first()
        assertEquals(1, notifications.size)
        assertEquals("New Found Item", notifications[0].title)
        assertEquals(NotificationType.NEW_FOUND_ITEM, notifications[0].type)
        assertEquals(false, notifications[0].isRead)
    }

    @Test
    fun `markAsRead marks notification as read`() = runBlocking {
        notificationRepository.sendNotification(
            userId = "user_student_1",
            title = "Claim Approved",
            message = "Your claim was approved.",
            type = NotificationType.CLAIM_APPROVED,
            claimId = "claim_99"
        )

        val initial = notificationRepository.getNotificationsForUser("user_student_1").first()
        val notifId = initial[0].id

        val markResult = notificationRepository.markAsRead("user_student_1", notifId)
        assertTrue(markResult.isSuccess)

        val updated = notificationRepository.getNotificationsForUser("user_student_1").first()
        assertEquals(true, updated[0].isRead)
    }

    @Test
    fun `markAllAsRead marks all user notifications as read`() = runBlocking {
        notificationRepository.sendNotification(
            userId = "user_student_1",
            title = "Notice 1",
            message = "Message 1",
            type = NotificationType.SYSTEM_ANNOUNCEMENT
        )
        notificationRepository.sendNotification(
            userId = "user_student_1",
            title = "Notice 2",
            message = "Message 2",
            type = NotificationType.SYSTEM_ANNOUNCEMENT
        )

        val result = notificationRepository.markAllAsRead("user_student_1")
        assertTrue(result.isSuccess)

        val list = notificationRepository.getNotificationsForUser("user_student_1").first()
        assertTrue(list.all { it.isRead })
    }

    @Test
    fun `deleteNotification removes notification from history`() = runBlocking {
        notificationRepository.sendNotification(
            userId = "user_student_1",
            title = "To Delete",
            message = "Test message",
            type = NotificationType.SYSTEM_ANNOUNCEMENT
        )

        val initial = notificationRepository.getNotificationsForUser("user_student_1").first()
        val notifId = initial[0].id

        val delResult = notificationRepository.deleteNotification("user_student_1", notifId)
        assertTrue(delResult.isSuccess)

        val remaining = notificationRepository.getNotificationsForUser("user_student_1").first()
        assertTrue(remaining.none { it.id == notifId })
    }
}
