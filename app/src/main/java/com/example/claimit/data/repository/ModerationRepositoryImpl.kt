package com.example.claimit.data.repository

import android.util.Log
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.ModerationAudit
import com.example.claimit.domain.model.NotificationType
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.model.ReportReason
import com.example.claimit.domain.model.ReportStatus
import com.example.claimit.domain.model.ReportedContent
import com.example.claimit.domain.model.TargetType
import com.example.claimit.domain.repository.ItemRepository
import com.example.claimit.domain.repository.ModerationRepository
import com.example.claimit.domain.repository.NotificationRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class ModerationRepositoryImpl(
    private val itemRepository: ItemRepository,
    private val dispatchers: CoroutineDispatchers,
    private val notificationRepository: NotificationRepository? = null,
    private val realtimeDatabase: FirebaseDatabase? = try {
        FirebaseDatabase.getInstance("https://claimit-322b5-default-rtdb.asia-southeast1.firebasedatabase.app")
    } catch (_: Exception) {
        try { FirebaseDatabase.getInstance() } catch (_: Exception) { null }
    }
) : ModerationRepository {

    private val TAG = "ModerationRepository"
    private val reportsStateFlow = MutableStateFlow<List<ReportedContent>>(emptyList())
    private val auditLogsStateFlow = MutableStateFlow<List<ModerationAudit>>(emptyList())
    private val blockedUsersMap = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    init {
        listenToModerationReports()
        listenToModerationAuditLogs()
    }

    private fun listenToModerationReports() {
        val rtdb = realtimeDatabase ?: return
        try {
            rtdb.reference.child("moderationReports")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = mutableListOf<ReportedContent>()
                        if (snapshot.exists()) {
                            for (child in snapshot.children) {
                                try {
                                    val id = child.key ?: continue
                                    val orgId = child.child("organizationId").getValue(String::class.java) ?: "lpu_main_campus"
                                    val rId = child.child("reporterId").getValue(String::class.java) ?: child.child("reporterUid").getValue(String::class.java) ?: ""
                                    val rEmail = child.child("reporterEmail").getValue(String::class.java) ?: ""
                                    val targetId = child.child("targetId").getValue(String::class.java) ?: child.child("itemId").getValue(String::class.java) ?: ""
                                    val targetName = child.child("targetTitleOrName").getValue(String::class.java) ?: child.child("itemName").getValue(String::class.java) ?: ""
                                    val reasonStr = child.child("reason").getValue(String::class.java) ?: child.child("reportCategory").getValue(String::class.java) ?: ReportReason.OTHER.name
                                    val details = child.child("details").getValue(String::class.java) ?: child.child("explanation").getValue(String::class.java) ?: ""
                                    val statusStr = child.child("status").getValue(String::class.java) ?: child.child("reviewStatus").getValue(String::class.java) ?: ReportStatus.PENDING.name
                                    val resolvedBy = child.child("resolvedByAdminId").getValue(String::class.java)
                                    val created = child.child("createdAtTimestamp").getValue(Long::class.java) ?: child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                                    val reason = try { ReportReason.valueOf(reasonStr) } catch (_: Exception) { ReportReason.OTHER }
                                    val status = try { ReportStatus.valueOf(statusStr) } catch (_: Exception) { ReportStatus.PENDING }

                                    list.add(
                                        ReportedContent(
                                            id = id,
                                            organizationId = orgId,
                                            reporterId = rId,
                                            reporterEmail = rEmail,
                                            targetType = TargetType.ITEM,
                                            targetId = targetId,
                                            targetTitleOrName = targetName,
                                            reason = reason,
                                            details = details,
                                            status = status,
                                            resolvedByAdminId = resolvedBy,
                                            createdAtTimestamp = created,
                                            reportReference = child.child("reportReference").getValue(String::class.java) ?: ("REF-" + id.takeLast(6).uppercase()),
                                            reporterName = child.child("reporterName").getValue(String::class.java) ?: rEmail.substringBefore("@"),
                                            reporterUid = rId,
                                            itemId = targetId,
                                            itemName = targetName,
                                            itemType = child.child("itemType").getValue(String::class.java) ?: "FOUND",
                                            itemOwnerDisplayName = child.child("itemOwnerDisplayName").getValue(String::class.java) ?: "Campus Member",
                                            reportCategory = reason.name,
                                            explanation = details,
                                            evidenceDetails = child.child("evidenceDetails").getValue(String::class.java) ?: "",
                                            reviewStatus = status,
                                            adminDecision = child.child("adminDecision").getValue(String::class.java) ?: "",
                                            adminExplanation = child.child("adminExplanation").getValue(String::class.java) ?: "",
                                            resolvedAtTimestamp = child.child("resolvedAtTimestamp").getValue(Long::class.java)
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing moderation report: ${e.message}")
                                }
                            }
                        }
                        reportsStateFlow.value = list.sortedByDescending { it.createdAtTimestamp }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "Moderation reports listener cancelled: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing moderation listener: ${e.message}")
        }
    }

    private fun listenToModerationAuditLogs() {
        val rtdb = realtimeDatabase ?: return
        try {
            rtdb.reference.child("moderationAudit")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = mutableListOf<ModerationAudit>()
                        if (snapshot.exists()) {
                            for (child in snapshot.children) {
                                try {
                                    val auditId = child.key ?: continue
                                    val reportId = child.child("reportId").getValue(String::class.java) ?: ""
                                    val itemId = child.child("itemId").getValue(String::class.java) ?: ""
                                    val itemName = child.child("itemName").getValue(String::class.java) ?: ""
                                    val adminUid = child.child("adminUid").getValue(String::class.java) ?: ""
                                    val action = child.child("action").getValue(String::class.java) ?: ""
                                    val reason = child.child("reason").getValue(String::class.java) ?: ""
                                    val prevStatus = child.child("previousStatus").getValue(String::class.java) ?: ""
                                    val newStatus = child.child("newStatus").getValue(String::class.java) ?: ""
                                    val ts = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                                    list.add(
                                        ModerationAudit(
                                            auditId = auditId,
                                            reportId = reportId,
                                            itemId = itemId,
                                            itemName = itemName,
                                            adminUid = adminUid,
                                            action = action,
                                            reason = reason,
                                            previousStatus = prevStatus,
                                            newStatus = newStatus,
                                            timestamp = ts
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing audit log: ${e.message}")
                                }
                            }
                        }
                        auditLogsStateFlow.value = list.sortedByDescending { it.timestamp }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "Moderation audit listener cancelled: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing audit listener: ${e.message}")
        }
    }

    override suspend fun reportContent(
        reporterId: String,
        reporterEmail: String,
        targetType: TargetType,
        targetId: String,
        targetTitleOrName: String,
        reason: ReportReason,
        details: String,
        organizationId: String
    ): Result<String> = withContext(dispatchers.io) {
        try {
            val reportId = "report_${UUID.randomUUID()}"
            val reportRef = "REF-" + reportId.takeLast(6).uppercase()

            val targetItem = itemRepository.getItemPostById(targetId)

            val reportMap = mapOf(
                "id" to reportId,
                "reportReference" to reportRef,
                "organizationId" to organizationId,
                "reporterId" to reporterId,
                "reporterUid" to reporterId,
                "reporterName" to reporterEmail.substringBefore("@"),
                "reporterEmail" to reporterEmail,
                "targetType" to targetType.name,
                "targetId" to targetId,
                "itemId" to targetId,
                "targetTitleOrName" to targetTitleOrName,
                "itemName" to targetTitleOrName,
                "itemType" to (targetItem?.type?.name ?: "FOUND"),
                "itemOwnerDisplayName" to (targetItem?.userDisplayName ?: "Campus Member"),
                "reason" to reason.name,
                "reportCategory" to reason.name,
                "details" to details,
                "explanation" to details,
                "status" to ReportStatus.PENDING.name,
                "reviewStatus" to ReportStatus.PENDING.name,
                "createdAtTimestamp" to System.currentTimeMillis(),
                "createdAt" to ServerValue.TIMESTAMP
            )

            realtimeDatabase?.let { rtdb ->
                rtdb.reference.child("moderationReports").child(reportId).setValue(reportMap).await()
            }

            val newReport = ReportedContent(
                id = reportId,
                organizationId = organizationId,
                reporterId = reporterId,
                reporterEmail = reporterEmail,
                targetType = targetType,
                targetId = targetId,
                targetTitleOrName = targetTitleOrName,
                reason = reason,
                details = details,
                status = ReportStatus.PENDING,
                createdAtTimestamp = System.currentTimeMillis(),
                reportReference = reportRef,
                reporterName = reporterEmail.substringBefore("@"),
                reporterUid = reporterId,
                itemId = targetId,
                itemName = targetTitleOrName,
                itemType = targetItem?.type?.name ?: "FOUND",
                itemOwnerDisplayName = targetItem?.userDisplayName ?: "Campus Member",
                reportCategory = reason.name,
                explanation = details,
                reviewStatus = ReportStatus.PENDING
            )

            val updatedList = (listOf(newReport) + reportsStateFlow.value).distinctBy { it.id }
            reportsStateFlow.value = updatedList

            // Notify admin
            notificationRepository?.sendNotification(
                userId = "user_admin_1",
                title = "New Moderation Report",
                message = "New report [$reportRef] submitted for listing '$targetTitleOrName'.",
                type = NotificationType.SYSTEM_ANNOUNCEMENT,
                itemId = targetId,
                senderUid = reporterId
            )

            Result.success(reportId)
        } catch (e: Exception) {
            Log.e(TAG, "Report content exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit> = withContext(dispatchers.io) {
        val current = blockedUsersMap.value.toMutableMap()
        val blockedSet = current[userId]?.toMutableSet() ?: mutableSetOf()
        blockedSet.add(blockedUserId)
        current[userId] = blockedSet
        blockedUsersMap.value = current
        Result.success(Unit)
    }

    override suspend fun unblockUser(userId: String, blockedUserId: String): Result<Unit> = withContext(dispatchers.io) {
        val current = blockedUsersMap.value.toMutableMap()
        val blockedSet = current[userId]?.toMutableSet() ?: mutableSetOf()
        blockedSet.remove(blockedUserId)
        current[userId] = blockedSet
        blockedUsersMap.value = current
        Result.success(Unit)
    }

    override fun getBlockedUserIds(userId: String): Flow<List<String>> {
        return blockedUsersMap.map { map ->
            (map[userId] ?: emptySet()).toList()
        }
    }

    override fun getReportedContentQueue(organizationId: String): Flow<List<ReportedContent>> {
        return reportsStateFlow.map { list ->
            list.filter { it.organizationId == organizationId }
        }
    }

    override suspend fun resolveReport(reportId: String, status: ReportStatus, adminId: String): Result<Unit> {
        return resolveReportWithDecision(
            reportId = reportId,
            status = status,
            adminId = adminId,
            decision = if (status == ReportStatus.RESOLVED_REMOVED || status == ReportStatus.RESOLVED) "RESOLVED" else "DISMISSED",
            explanation = "Reviewed by campus administrator"
        )
    }

    override suspend fun resolveReportWithDecision(
        reportId: String,
        status: ReportStatus,
        adminId: String,
        decision: String,
        explanation: String
    ): Result<Unit> = withContext(dispatchers.io) {
        try {
            val targetReport = reportsStateFlow.value.firstOrNull { it.id == reportId }
            val itemId = targetReport?.targetId ?: ""

            val updates = mapOf<String, Any>(
                "status" to status.name,
                "reviewStatus" to status.name,
                "adminDecision" to decision,
                "adminExplanation" to explanation,
                "resolvedByAdminId" to adminId,
                "resolvedAtTimestamp" to System.currentTimeMillis(),
                "resolvedAt" to ServerValue.TIMESTAMP
            )

            realtimeDatabase?.let { rtdb ->
                rtdb.reference.child("moderationReports").child(reportId).updateChildren(updates).await()

                // Log to audit
                val auditId = "audit_${UUID.randomUUID()}"
                val auditMap = mapOf(
                    "auditId" to auditId,
                    "reportId" to reportId,
                    "itemId" to itemId,
                    "itemName" to (targetReport?.targetTitleOrName ?: ""),
                    "adminUid" to adminId,
                    "action" to decision,
                    "reason" to explanation,
                    "previousStatus" to (targetReport?.status?.name ?: "PENDING"),
                    "newStatus" to status.name,
                    "timestamp" to ServerValue.TIMESTAMP
                )
                rtdb.reference.child("moderationAudit").child(auditId).setValue(auditMap).await()
            }

            if ((status == ReportStatus.RESOLVED_REMOVED || decision == "REMOVED") && itemId.isNotBlank()) {
                adminRemoveListing(itemId, adminId, targetReport?.reason?.name ?: "Policy Violation", explanation)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Resolve report exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun adminRemoveListing(
        itemId: String,
        adminUid: String,
        reason: String,
        explanation: String
    ): Result<Unit> = withContext(dispatchers.io) {
        try {
            val targetItem = itemRepository.getItemPostById(itemId)
            val ownerUid = targetItem?.userId ?: ""
            val itemName = targetItem?.title ?: "Listing"

            // Set item status to REMOVED
            itemRepository.updatePostStatus(itemId, PostStatus.REMOVED)

            // Audit record
            realtimeDatabase?.let { rtdb ->
                val auditId = "audit_${UUID.randomUUID()}"
                val auditMap = mapOf(
                    "auditId" to auditId,
                    "reportId" to "",
                    "itemId" to itemId,
                    "itemName" to itemName,
                    "adminUid" to adminUid,
                    "action" to "REMOVED",
                    "reason" to "$reason: $explanation",
                    "previousStatus" to (targetItem?.status?.name ?: "ACTIVE"),
                    "newStatus" to PostStatus.REMOVED.name,
                    "timestamp" to ServerValue.TIMESTAMP
                )
                rtdb.reference.child("moderationAudit").child(auditId).setValue(auditMap).await()
            }

            // Notify Listing Owner
            if (ownerUid.isNotBlank()) {
                notificationRepository?.sendNotification(
                    userId = ownerUid,
                    title = "Your Listing Has Been Removed",
                    message = "Your ClaimIT listing '$itemName' has been removed by an administrator. Reason: $reason ($explanation)",
                    type = NotificationType.SYSTEM_ANNOUNCEMENT,
                    itemId = itemId,
                    senderUid = adminUid
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Admin remove listing exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getModerationAuditLogs(): Flow<List<ModerationAudit>> {
        return auditLogsStateFlow
    }
}
