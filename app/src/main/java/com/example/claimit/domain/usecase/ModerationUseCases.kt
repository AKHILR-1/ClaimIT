package com.example.claimit.domain.usecase

import com.example.claimit.domain.model.ReportReason
import com.example.claimit.domain.model.ReportStatus
import com.example.claimit.domain.model.ReportedContent
import com.example.claimit.domain.model.TargetType
import com.example.claimit.domain.repository.ModerationRepository
import kotlinx.coroutines.flow.Flow

class ModerationUseCases(
    private val moderationRepository: ModerationRepository
) {
    suspend fun reportContent(
        reporterId: String,
        reporterEmail: String,
        targetType: TargetType,
        targetId: String,
        targetTitleOrName: String,
        reason: ReportReason,
        details: String
    ): Result<String> {
        return moderationRepository.reportContent(
            reporterId, reporterEmail, targetType, targetId, targetTitleOrName, reason, details
        )
    }

    suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit> {
        return moderationRepository.blockUser(userId, blockedUserId)
    }

    suspend fun unblockUser(userId: String, blockedUserId: String): Result<Unit> {
        return moderationRepository.unblockUser(userId, blockedUserId)
    }

    fun getBlockedUserIds(userId: String): Flow<List<String>> {
        return moderationRepository.getBlockedUserIds(userId)
    }

    fun getReportedContentQueue(): Flow<List<ReportedContent>> {
        return moderationRepository.getReportedContentQueue()
    }

    suspend fun resolveReport(reportId: String, status: ReportStatus, adminId: String): Result<Unit> {
        return moderationRepository.resolveReport(reportId, status, adminId)
    }
}
