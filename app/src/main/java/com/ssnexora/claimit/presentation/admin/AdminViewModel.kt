package com.ssnexora.claimit.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssnexora.claimit.core.dispatchers.CoroutineDispatchers
import com.ssnexora.claimit.domain.model.Claim
import com.ssnexora.claimit.domain.model.DeviceSession
import com.ssnexora.claimit.domain.model.ItemPost
import com.ssnexora.claimit.domain.model.ModerationAudit
import com.ssnexora.claimit.domain.model.PostStatus
import com.ssnexora.claimit.domain.model.ReportStatus
import com.ssnexora.claimit.domain.model.ReportedContent
import com.ssnexora.claimit.domain.model.User
import com.ssnexora.claimit.domain.repository.AdminAuthState
import com.ssnexora.claimit.domain.repository.AdminOverviewStats
import com.ssnexora.claimit.domain.repository.AdminRepository
import com.ssnexora.claimit.domain.repository.AuthRepository
import com.ssnexora.claimit.domain.repository.ImageStorageRepository
import com.ssnexora.claimit.domain.repository.ItemRepository
import com.ssnexora.claimit.domain.repository.ModerationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository,
    private val itemRepository: ItemRepository,
    private val moderationRepository: ModerationRepository,
    private val imageStorageRepository: ImageStorageRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    val adminAuthState: StateFlow<AdminAuthState> = adminRepository.adminAuthState

    val overviewStats: StateFlow<AdminOverviewStats> = adminRepository.getOverviewStats()
        .stateIn(viewModelScope, SharingStarted.Lazily, AdminOverviewStats())

    val allItemPosts: StateFlow<List<ItemPost>> = adminRepository.getAllItemPosts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    fun hideReport(itemId: String, reason: String) {
        viewModelScope.launch(dispatchers.main) {
            itemRepository.updatePostStatus(itemId, PostStatus.REMOVED)
            val currentAuth = adminAuthState.value
            val adminUid = if (currentAuth is AdminAuthState.Authorized) currentAuth.adminUid else "admin_system"
            moderationRepository.adminRemoveListing(itemId, adminUid, "REPORT_HIDDEN", reason)
        }
    }

    fun restoreReport(itemId: String) {
        viewModelScope.launch(dispatchers.main) {
            itemRepository.updatePostStatus(itemId, PostStatus.ACTIVE)
        }
    }

    fun markReportNonGenuine(itemId: String, reason: String) {
        viewModelScope.launch(dispatchers.main) {
            itemRepository.updatePostStatus(itemId, PostStatus.REMOVED)
            val currentAuth = adminAuthState.value
            val adminUid = if (currentAuth is AdminAuthState.Authorized) currentAuth.adminUid else "admin_system"
            moderationRepository.adminRemoveListing(itemId, adminUid, "REPORT_MARKED_NON_GENUINE", reason)
        }
    }

    fun permanentlyDeleteReport(itemId: String, reason: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(dispatchers.main) {
            val item = itemRepository.getItemPostById(itemId)
            val imageFileIds = item?.imageFileIds ?: emptyList()

            // 1. Delete associated Google Drive photos
            for (fileId in imageFileIds) {
                if (fileId.isNotBlank()) {
                    imageStorageRepository.deleteImage(fileId)
                }
            }

            // 2. Permanently delete database record
            val success = itemRepository.deletePost(itemId)
            if (success) {
                val currentAuth = adminAuthState.value
                val adminUid = if (currentAuth is AdminAuthState.Authorized) currentAuth.adminUid else "admin_system"
                moderationRepository.adminRemoveListing(itemId, adminUid, "REPORT_PERMANENTLY_DELETED", reason)
                onResult(true, "Report and associated Drive photos permanently deleted.")
            } else {
                onResult(false, "Failed to permanently delete report from database.")
            }
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

    fun suspendStudentAccount(userId: String, reason: String = "Policy violation") {
        viewModelScope.launch(dispatchers.main) {
            authRepository.suspendUserAccount(userId)
        }
    }

    fun reactivateStudentAccount(userId: String) {
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

