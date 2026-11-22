package com.example.claimit.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.Claim
import com.example.claimit.domain.model.DeviceSession
import com.example.claimit.domain.model.ModerationAudit
import com.example.claimit.domain.model.ReportStatus
import com.example.claimit.domain.model.ReportedContent
import com.example.claimit.domain.model.User
import com.example.claimit.domain.repository.AdminAuthState
import com.example.claimit.domain.repository.AdminOverviewStats
import com.example.claimit.domain.repository.AdminRepository
import com.example.claimit.domain.repository.AuthRepository
import com.example.claimit.domain.repository.ModerationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository,
    private val moderationRepository: ModerationRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    val adminAuthState: StateFlow<AdminAuthState> = adminRepository.adminAuthState

    val overviewStats: StateFlow<AdminOverviewStats> = adminRepository.getOverviewStats()
        .stateIn(viewModelScope, SharingStarted.Lazily, AdminOverviewStats())

    val reportedContentList: StateFlow<List<ReportedContent>> = adminRepository.getReportedContentQueue()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val auditLogs: StateFlow<List<ModerationAudit>> = adminRepository.getModerationAuditLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val registeredUsers: StateFlow<List<User>> = adminRepository.getAllRegisteredUsers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeDeviceSessions: StateFlow<List<DeviceSession>> = adminRepository.getActiveDeviceSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val claimsList: StateFlow<List<Claim>> = adminRepository.getClaimsList()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch(dispatchers.main) {
            authRepository.currentUserState.collect { user ->
                if (user != null) {
                    verifySuperAdminClaims(forceRefresh = true)
                } else {
                    adminRepository.resetAdminAuth()
                }
            }
        }
    }

    fun verifySuperAdminClaims(forceRefresh: Boolean = true) {
        viewModelScope.launch(dispatchers.main) {
            adminRepository.verifySuperAdminClaims(forceRefresh)
        }
    }

    fun signOutAdmin() {
        viewModelScope.launch(dispatchers.main) {
            adminRepository.resetAdminAuth()
            authRepository.logout()
        }
    }

    fun resolveReportWithDecision(
        reportId: String,
        status: ReportStatus,
        decision: String,
        explanation: String
    ) {
        val currentAuth = adminAuthState.value
        if (currentAuth is AdminAuthState.Authorized) {
            viewModelScope.launch(dispatchers.main) {
                moderationRepository.resolveReportWithDecision(
                    reportId = reportId,
                    status = status,
                    adminId = currentAuth.adminUid,
                    decision = decision,
                    explanation = explanation
                )
            }
        }
    }

    fun removeListing(itemId: String, reason: String, explanation: String) {
        val currentAuth = adminAuthState.value
        if (currentAuth is AdminAuthState.Authorized) {
            viewModelScope.launch(dispatchers.main) {
                moderationRepository.adminRemoveListing(
                    itemId = itemId,
                    adminUid = currentAuth.adminUid,
                    reason = reason,
                    explanation = explanation
                )
            }
        }
    }

    fun suspendUserAccount(userId: String) {
        viewModelScope.launch(dispatchers.main) {
            authRepository.suspendUserAccount(userId)
        }
    }

    fun unsuspendUserAccount(userId: String) {
        viewModelScope.launch(dispatchers.main) {
            authRepository.unsuspendUserAccount(userId)
        }
    }

    fun revokeDeviceSession(userId: String, sessionId: String) {
        viewModelScope.launch(dispatchers.main) {
            authRepository.revokeSession(userId, sessionId)
        }
    }
}
