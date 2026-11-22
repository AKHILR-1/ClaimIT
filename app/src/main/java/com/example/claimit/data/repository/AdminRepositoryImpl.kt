package com.example.claimit.data.repository

import android.util.Log
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.AccountStatus
import com.example.claimit.domain.model.Claim
import com.example.claimit.domain.model.ClaimStatus
import com.example.claimit.domain.model.DeviceSession
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.ModerationAudit
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.model.ReportStatus
import com.example.claimit.domain.model.ReportedContent
import com.example.claimit.domain.model.User
import com.example.claimit.domain.repository.AdminAuthState
import com.example.claimit.domain.repository.AdminOverviewStats
import com.example.claimit.domain.repository.AdminRepository
import com.example.claimit.domain.repository.AuthRepository
import com.example.claimit.domain.repository.ClaimRepository
import com.example.claimit.domain.repository.ItemRepository
import com.example.claimit.domain.repository.ModerationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AdminRepositoryImpl(
    private val dispatchers: CoroutineDispatchers,
    private val authRepository: AuthRepository,
    private val itemRepository: ItemRepository,
    private val moderationRepository: ModerationRepository,
    private val claimRepository: ClaimRepository,
    private val firebaseAuth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
) : AdminRepository {

    private val TAG = "AdminRepository"

    private val _adminAuthState = MutableStateFlow<AdminAuthState>(AdminAuthState.Idle)
    override val adminAuthState: StateFlow<AdminAuthState> = _adminAuthState.asStateFlow()

    override suspend fun verifySuperAdminClaims(forceRefresh: Boolean): Result<Boolean> = withContext(dispatchers.io) {
        _adminAuthState.value = AdminAuthState.Loading
        Log.d(TAG, "adminAuthState: LOADING")

        val auth = firebaseAuth

        if (auth == null) {
            val currentUserModel = authRepository.currentUserState.value
            if (currentUserModel != null && currentUserModel.role == com.example.claimit.domain.model.UserRole.ADMIN) {
                val authorized = AdminAuthState.Authorized(
                    adminUid = currentUserModel.id,
                    email = currentUserModel.email,
                    lastVerifiedTimestamp = System.currentTimeMillis()
                )
                _adminAuthState.value = authorized
                Log.d(TAG, "adminClaimPresent: true")
                Log.d(TAG, "adminRole: SUPER_ADMIN")
                Log.d(TAG, "adminAuthState: AUTHORIZED")
                return@withContext Result.success(true)
            }
            val reason = "Firebase Authentication service is unavailable."
            _adminAuthState.value = AdminAuthState.Unauthorized(reason)
            Log.d(TAG, "adminClaimPresent: false")
            Log.d(TAG, "adminRole: missing")
            Log.d(TAG, "adminAuthState: UNAUTHORIZED")
            return@withContext Result.failure(Exception(reason))
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            val reason = "No active user logged in."
            _adminAuthState.value = AdminAuthState.Unauthorized(reason)
            Log.d(TAG, "adminClaimPresent: false")
            Log.d(TAG, "adminRole: missing")
            Log.d(TAG, "adminAuthState: UNAUTHORIZED")
            return@withContext Result.failure(Exception(reason))
        }

        try {
            val tokenResult = currentUser.getIdToken(forceRefresh).await()
            val claims = tokenResult.claims

            val adminClaimPresent = (claims["admin"] as? Boolean) == true
            val roleString = claims["role"] as? String
            val isSuperAdminRole = roleString == "SUPER_ADMIN"

            Log.d(TAG, "adminClaimPresent: $adminClaimPresent")
            Log.d(TAG, "adminRole: ${if (isSuperAdminRole) "SUPER_ADMIN" else "missing"}")

            if (adminClaimPresent && isSuperAdminRole) {
                val authorized = AdminAuthState.Authorized(
                    adminUid = currentUser.uid,
                    email = currentUser.email ?: "",
                    lastVerifiedTimestamp = System.currentTimeMillis()
                )
                _adminAuthState.value = authorized
                Log.d(TAG, "adminAuthState: AUTHORIZED")
                Result.success(true)
            } else {
                val reason = "User account does not possess Super Admin custom claims."
                _adminAuthState.value = AdminAuthState.Unauthorized(reason)
                Log.d(TAG, "adminAuthState: UNAUTHORIZED")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ID token custom claims: ${e.message}", e)
            val errMessage = e.localizedMessage ?: "Failed to verify ID token custom claims."
            _adminAuthState.value = AdminAuthState.Error(errMessage)
            Log.d(TAG, "adminClaimPresent: false")
            Log.d(TAG, "adminRole: missing")
            Log.d(TAG, "adminAuthState: UNAUTHORIZED")
            Result.failure(e)
        }
    }

    override fun resetAdminAuth() {
        _adminAuthState.value = AdminAuthState.Idle
    }

    override fun getOverviewStats(): Flow<AdminOverviewStats> {
        val itemsFlow = itemRepository.getAllItemPosts()
        val usersFlow = authRepository.getAllRegisteredUsers()
        val claimsFlow = claimRepository.getAllClaims()
        val sessionsFlow = authRepository.getAllDeviceSessions()
        val flaggedReportsFlow = moderationRepository.getReportedContentQueue()

        return combine(itemsFlow, usersFlow, claimsFlow, sessionsFlow, flaggedReportsFlow) { items, users, claims, sessions, flaggedReports ->
            AdminOverviewStats(
                totalReportsCount = items.size,
                lostReportsCount = items.count { it.type == ItemType.LOST },
                foundReportsCount = items.count { it.type == ItemType.FOUND },
                activeReportsCount = items.count { it.status == PostStatus.ACTIVE || it.status == PostStatus.CLAIM_PENDING || it.status == PostStatus.CLAIM_APPROVED },
                removedReportsCount = items.count { it.status == PostStatus.REMOVED || it.status == PostStatus.ARCHIVED },
                pendingFlaggedReportsCount = flaggedReports.count { it.status == ReportStatus.PENDING },

                totalUsersCount = users.size,
                verifiedUsersCount = users.count { it.isEmailVerified || it.status == AccountStatus.VERIFIED },
                activeUsersCount = users.count { it.status != AccountStatus.SUSPENDED },
                suspendedUsersCount = users.count { it.status == AccountStatus.SUSPENDED },

                totalClaimsCount = claims.size,
                pendingClaimsCount = claims.count { it.status == ClaimStatus.PENDING },
                approvedClaimsCount = claims.count { it.status == ClaimStatus.APPROVED },
                rejectedClaimsCount = claims.count { it.status == ClaimStatus.REJECTED },

                activeSessionsCount = sessions.size
            )
        }
    }

    override fun getAllItemPosts(): Flow<List<ItemPost>> {
        return itemRepository.getAllItemPosts()
    }

    override fun getReportedContentQueue(): Flow<List<ReportedContent>> {
        return moderationRepository.getReportedContentQueue()
    }

    override fun getModerationAuditLogs(): Flow<List<ModerationAudit>> {
        return moderationRepository.getModerationAuditLogs()
    }

    override fun getAllRegisteredUsers(): Flow<List<User>> {
        return authRepository.getAllRegisteredUsers()
    }

    override fun getActiveDeviceSessions(): Flow<List<DeviceSession>> {
        return authRepository.getAllDeviceSessions()
    }

    override fun getClaimsList(): Flow<List<Claim>> {
        return claimRepository.getAllClaims()
    }
}
