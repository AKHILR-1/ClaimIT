package com.example.claimit.data.repository

import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.model.ReportReason
import com.example.claimit.domain.model.ReportStatus
import com.example.claimit.domain.model.ReportedContent
import com.example.claimit.domain.model.TargetType
import com.example.claimit.domain.repository.ItemRepository
import com.example.claimit.domain.repository.ModerationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ModerationRepositoryImpl(
    private val itemRepository: ItemRepository,
    private val dispatchers: CoroutineDispatchers
) : ModerationRepository {

    private val reportsStateFlow = MutableStateFlow<List<ReportedContent>>(emptyList())

    private val blockedUsersMap = MutableStateFlow<Map<String, Set<String>>>(emptyMap()) // userId -> set of blockedUserIds

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
            val report = ReportedContent(
                id = "report_${UUID.randomUUID()}",
                organizationId = organizationId,
                reporterId = reporterId,
                reporterEmail = reporterEmail,
                targetType = targetType,
                targetId = targetId,
                targetTitleOrName = targetTitleOrName,
                reason = reason,
                details = details,
                status = ReportStatus.PENDING,
                createdAtTimestamp = System.currentTimeMillis()
            )
            val updated = reportsStateFlow.value.toMutableList()
            updated.add(0, report)
            reportsStateFlow.value = updated
            Result.success(report.id)
        } catch (e: Exception) {
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

    override suspend fun resolveReport(reportId: String, status: ReportStatus, adminId: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            var targetIdToHide: String? = null
            var isItemTarget = false

            val updated = reportsStateFlow.value.map { report ->
                if (report.id == reportId) {
                    targetIdToHide = report.targetId
                    isItemTarget = report.targetType == TargetType.ITEM
                    report.copy(
                        status = status,
                        resolvedByAdminId = adminId
                    )
                } else report
            }
            reportsStateFlow.value = updated

            val targetId = targetIdToHide
            if (status == ReportStatus.RESOLVED_REMOVED && isItemTarget && targetId != null) {
                itemRepository.updatePostStatus(targetId, PostStatus.REMOVED)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
