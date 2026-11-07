package com.example.claimit.domain.repository

import com.example.claimit.domain.model.ReportReason
import com.example.claimit.domain.model.ReportStatus
import com.example.claimit.domain.model.ReportedContent
import com.example.claimit.domain.model.TargetType
import kotlinx.coroutines.flow.Flow

interface ModerationRepository {
    suspend fun reportContent(
        reporterId: String,
        reporterEmail: String,
        targetType: TargetType,
        targetId: String,
        targetTitleOrName: String,
        reason: ReportReason,
        details: String,
        organizationId: String = "lpu_main_campus"
    ): Result<String>

    suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit>
    suspend fun unblockUser(userId: String, blockedUserId: String): Result<Unit>
    fun getBlockedUserIds(userId: String): Flow<List<String>>

    // Admin functions
    fun getReportedContentQueue(organizationId: String = "lpu_main_campus"): Flow<List<ReportedContent>>
    suspend fun resolveReport(reportId: String, status: ReportStatus, adminId: String): Result<Unit>
}
