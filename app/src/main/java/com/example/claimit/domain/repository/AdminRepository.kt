package com.example.claimit.domain.repository

import com.example.claimit.domain.model.Claim
import com.example.claimit.domain.model.DeviceSession
import com.example.claimit.domain.model.ModerationAudit
import com.example.claimit.domain.model.ReportedContent
import com.example.claimit.domain.model.User
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
    val pendingReportsCount: Int = 0,
    val totalUsersCount: Int = 0,
    val activeSessionsCount: Int = 0,
    val auditLogsCount: Int = 0,
    val pendingClaimsCount: Int = 0
)

interface AdminRepository {
    val adminAuthState: StateFlow<AdminAuthState>
    suspend fun verifySuperAdminClaims(forceRefresh: Boolean = true): Result<Boolean>
    fun resetAdminAuth()
    fun getOverviewStats(): Flow<AdminOverviewStats>
    fun getReportedContentQueue(): Flow<List<ReportedContent>>
    fun getModerationAuditLogs(): Flow<List<ModerationAudit>>
    fun getAllRegisteredUsers(): Flow<List<User>>
    fun getActiveDeviceSessions(): Flow<List<DeviceSession>>
    fun getClaimsList(): Flow<List<Claim>>
}
