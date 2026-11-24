package com.ssnexora.claimit.domain.repository

import com.ssnexora.claimit.domain.model.Claim
import com.ssnexora.claimit.domain.model.DeviceSession
import com.ssnexora.claimit.domain.model.ItemPost
import com.ssnexora.claimit.domain.model.ModerationAudit
import com.ssnexora.claimit.domain.model.ReportedContent
import com.ssnexora.claimit.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed class AdminAuthState {
    object Idle : AdminAuthState()
    object Loading : AdminAuthState()
    data class Authorized(
        val adminUid: String,
        val email: String,
        val lastVerifiedTimestamp: Long
    ) : AdminAuthState()
    data class Unauthorized(val reason: String) : AdminAuthState()
    data class Error(val message: String) : AdminAuthState()
}

data class AdminOverviewStats(
    val totalReportsCount: Int = 0,
    val lostReportsCount: Int = 0,
    val foundReportsCount: Int = 0,
    val activeReportsCount: Int = 0,
    val removedReportsCount: Int = 0,
    val pendingFlaggedReportsCount: Int = 0,

    val totalUsersCount: Int = 0,
    val verifiedUsersCount: Int = 0,
    val activeUsersCount: Int = 0,
    val suspendedUsersCount: Int = 0,

    val totalClaimsCount: Int = 0,
    val pendingClaimsCount: Int = 0,
    val approvedClaimsCount: Int = 0,
    val rejectedClaimsCount: Int = 0,

    val activeSessionsCount: Int = 0
)

interface AdminRepository {
    val adminAuthState: StateFlow<AdminAuthState>
    suspend fun verifySuperAdminClaims(forceRefresh: Boolean = true): Result<Boolean>
    fun resetAdminAuth()
    fun getOverviewStats(): Flow<AdminOverviewStats>
    fun getAllItemPosts(): Flow<List<ItemPost>>
    fun getReportedContentQueue(): Flow<List<ReportedContent>>
    fun getModerationAuditLogs(): Flow<List<ModerationAudit>>
    fun getAllRegisteredUsers(): Flow<List<User>>
    fun getActiveDeviceSessions(): Flow<List<DeviceSession>>
    fun getClaimsList(): Flow<List<Claim>>
}

