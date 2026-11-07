package com.example.claimit.data.repository

import android.util.Log
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.NotificationItem
import com.example.claimit.domain.model.NotificationType
import com.example.claimit.domain.repository.NotificationRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class NotificationRepositoryImpl(
    private val dispatchers: CoroutineDispatchers,
    private val realtimeDatabase: FirebaseDatabase? = try {
        FirebaseDatabase.getInstance("https://claimit-322b5-default-rtdb.asia-southeast1.firebasedatabase.app")
    } catch (_: Exception) {
        try { FirebaseDatabase.getInstance() } catch (_: Exception) { null }
    }
) : NotificationRepository {

    private val TAG = "NotificationRepo"
    private val memoryNotifications = MutableStateFlow<List<NotificationItem>>(emptyList())

    override fun getNotificationsForUser(userId: String): Flow<List<NotificationItem>> {
        val rtdb = realtimeDatabase
        if (rtdb == null || userId.isBlank()) {
            return memoryNotifications.map { list -> list.filter { it.userId == userId } }
        }

        return callbackFlow {
            val ref = rtdb.reference.child("notifications").child(userId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<NotificationItem>()
                    for (child in snapshot.children) {
                        try {
                            val id = child.key ?: continue
                            val title = child.child("title").getValue(String::class.java) ?: ""
                            val message = child.child("message").getValue(String::class.java) ?: ""
                            val typeStr = child.child("type").getValue(String::class.java) ?: NotificationType.SYSTEM_ANNOUNCEMENT.name
                            val type = try { NotificationType.valueOf(typeStr) } catch (_: Exception) { NotificationType.SYSTEM_ANNOUNCEMENT }
                            val itemId = child.child("itemId").getValue(String::class.java) ?: ""
                            val claimId = child.child("claimId").getValue(String::class.java) ?: ""
                            val senderUid = child.child("senderUid").getValue(String::class.java) ?: ""
                            val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                            val timestamp = child.child("createdAt").getValue(Long::class.java)
                                ?: child.child("timestamp").getValue(Long::class.java)
                                ?: System.currentTimeMillis()

                            list.add(
                                NotificationItem(
                                    id = id,
                                    userId = userId,
                                    title = title,
                                    message = message,
                                    type = type,
                                    itemId = itemId,
                                    claimId = claimId,
                                    senderUid = senderUid,
                                    isRead = isRead,
                                    timestamp = timestamp
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing notification snapshot: ${e.message}")
                        }
                    }
                    // Sort newest first
                    list.sortByDescending { it.timestamp }
                    trySend(list)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Notification listener cancelled: ${error.message}")
                }
            }

            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
    }

    override suspend fun markAsRead(userId: String, notificationId: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            val rtdb = realtimeDatabase
            if (rtdb != null && userId.isNotBlank()) {
                rtdb.reference
                    .child("notifications")
                    .child(userId)
                    .child(notificationId)
                    .child("isRead")
                    .setValue(true)
                    .await()
            }

            val updated = memoryNotifications.value.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            memoryNotifications.value = updated
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark notification as read: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            val rtdb = realtimeDatabase
            if (rtdb != null && userId.isNotBlank()) {
                val snapshot = rtdb.reference.child("notifications").child(userId).get().await()
                if (snapshot != null && snapshot.exists()) {
                    val updates = mutableMapOf<String, Any>()
                    for (child in snapshot.children) {
                        child.key?.let { notifId ->
                            updates["$notifId/isRead"] = true
                        }
                    }
                    if (updates.isNotEmpty()) {
                        rtdb.reference.child("notifications").child(userId).updateChildren(updates).await()
                    }
                }
            }

            val updated = memoryNotifications.value.map {
                if (it.userId == userId) it.copy(isRead = true) else it
            }
            memoryNotifications.value = updated
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark all as read: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            val rtdb = realtimeDatabase
            if (rtdb != null && userId.isNotBlank()) {
                rtdb.reference
                    .child("notifications")
                    .child(userId)
                    .child(notificationId)
                    .removeValue()
                    .await()
            }

            val updated = memoryNotifications.value.filterNot { it.id == notificationId }
            memoryNotifications.value = updated
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete notification: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun sendNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType,
        itemId: String,
        claimId: String,
        senderUid: String
    ): Result<Unit> = withContext(dispatchers.io) {
        try {
            val notifId = "notif_${UUID.randomUUID()}"
            val notif = NotificationItem(
                id = notifId,
                userId = userId,
                title = title,
                message = message,
                type = type,
                itemId = itemId,
                claimId = claimId,
                senderUid = senderUid,
                isRead = false,
                timestamp = System.currentTimeMillis()
            )

            val rtdb = realtimeDatabase
            if (rtdb != null && userId.isNotBlank()) {
                val notifMap = mapOf(
                    "title" to title,
                    "message" to message,
                    "type" to type.name,
                    "itemId" to itemId,
                    "claimId" to claimId,
                    "senderUid" to senderUid,
                    "isRead" to false,
                    "createdAt" to ServerValue.TIMESTAMP
                )
                rtdb.reference.child("notifications").child(userId).child(notifId).setValue(notifMap).await()
            }

            val updated = memoryNotifications.value.toMutableList()
            updated.add(0, notif)
            memoryNotifications.value = updated

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun broadcastNotificationToOrganization(
        organizationId: String,
        senderUid: String,
        title: String,
        message: String,
        type: NotificationType,
        itemId: String
    ): Result<Unit> = withContext(dispatchers.io) {
        try {
            val rtdb = realtimeDatabase
            if (rtdb != null) {
                val usersSnapshot = rtdb.reference.child("users").get().await()
                if (usersSnapshot != null && usersSnapshot.exists()) {
                    for (userDoc in usersSnapshot.children) {
                        val recipientUid = userDoc.key ?: continue
                        if (recipientUid == senderUid) continue // Exclude sender

                        val userOrg = userDoc.child("organizationId").getValue(String::class.java) ?: "lpu_main_campus"
                        if (userOrg == organizationId) {
                            sendNotification(
                                userId = recipientUid,
                                title = title,
                                message = message,
                                type = type,
                                itemId = itemId,
                                senderUid = senderUid
                            )
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast notification: ${e.message}")
            Result.failure(e)
        }
    }
}
