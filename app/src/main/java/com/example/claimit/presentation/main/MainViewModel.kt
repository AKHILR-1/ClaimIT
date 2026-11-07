package com.example.claimit.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.Claim
import com.example.claimit.domain.model.ClaimStatus
import com.example.claimit.domain.model.NotificationItem
import com.example.claimit.domain.model.ReportReason
import com.example.claimit.domain.model.ReportStatus
import com.example.claimit.domain.model.ReportedContent
import com.example.claimit.domain.model.TargetType
import com.example.claimit.domain.model.User
import com.example.claimit.domain.model.UserRole
import com.example.claimit.domain.repository.AuthRepository
import com.example.claimit.domain.repository.ClaimRepository
import com.example.claimit.domain.repository.ItemRepository
import com.example.claimit.domain.repository.ModerationRepository
import com.example.claimit.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    val authRepository: AuthRepository,
    val itemRepository: ItemRepository,
    val claimRepository: ClaimRepository,
    val moderationRepository: ModerationRepository,
    val notificationRepository: NotificationRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    val currentUserState: StateFlow<User?> = authRepository.currentUserState

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val userNotifications: StateFlow<List<NotificationItem>> = currentUserState.flatMapLatest { user ->
        val userId = user?.id ?: ""
        notificationRepository.getNotificationsForUser(userId)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val unreadNotificationCount: StateFlow<Int> = userNotifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val mySubmittedClaims: StateFlow<List<Claim>> = currentUserState.flatMapLatest { user ->
        val userId = user?.id ?: ""
        claimRepository.getClaimsForUser(userId)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomingClaimsToReview: StateFlow<List<Claim>> = currentUserState.flatMapLatest { user ->
        val userId = user?.id ?: ""
        claimRepository.getClaimsForListingOwner(userId)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val reportedQueue: StateFlow<List<ReportedContent>> = moderationRepository.getReportedContentQueue()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val savedPosts: StateFlow<List<com.example.claimit.domain.model.ItemPost>> = currentUserState.flatMapLatest { user ->
        val userId = user?.id ?: ""
        itemRepository.getSavedPosts(userId)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun login(email: String, pass: String) {
        viewModelScope.launch(dispatchers.io) {
            _isAuthLoading.value = true
            _authError.value = null
            authRepository.login(email, pass)
                .onFailure { _authError.value = it.localizedMessage }
            _isAuthLoading.value = false
        }
    }

    fun register(email: String, pass: String, name: String, collegeEmail: String, studentId: String, role: UserRole) {
        viewModelScope.launch(dispatchers.io) {
            _isAuthLoading.value = true
            _authError.value = null
            authRepository.register(email, pass, name, collegeEmail, studentId, role)
                .onFailure { _authError.value = it.localizedMessage }
            _isAuthLoading.value = false
        }
    }

    fun updateContactEmail(newContactEmail: String, currentPass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(dispatchers.io) {
            _isAuthLoading.value = true
            val result = authRepository.updateContactEmail(newContactEmail, currentPass)
            _isAuthLoading.value = false
            result.onSuccess {
                onResult(true, "Verification email sent to $newContactEmail. Please verify your new contact email.")
            }.onFailure {
                onResult(false, it.localizedMessage ?: "Failed to update contact email.")
            }
        }
    }

    fun resendVerificationEmail(onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch(dispatchers.io) {
            _isAuthLoading.value = true
            val result = authRepository.sendEmailVerification()
            _isAuthLoading.value = false
            result.onSuccess {
                onComplete(true, null)
            }.onFailure {
                onComplete(false, it.localizedMessage)
            }
        }
    }

    fun checkVerificationStatus(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(dispatchers.io) {
            _isAuthLoading.value = true
            val result = authRepository.checkEmailVerificationStatus()
            _isAuthLoading.value = false
            val isVerified = result.getOrDefault(false)
            onComplete(isVerified)
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch(dispatchers.io) {
            val userId = currentUserState.value?.id ?: return@launch
            notificationRepository.markAsRead(userId, notificationId)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch(dispatchers.io) {
            val userId = currentUserState.value?.id ?: return@launch
            notificationRepository.markAllAsRead(userId)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch(dispatchers.io) {
            val userId = currentUserState.value?.id ?: return@launch
            notificationRepository.deleteNotification(userId, notificationId)
        }
    }

    fun logout() {
        viewModelScope.launch(dispatchers.io) {
            authRepository.logout()
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(dispatchers.io) {
            _isAuthLoading.value = true
            _authError.value = null
            val res = authRepository.resetPassword(email)
            _isAuthLoading.value = false
            res.onSuccess {
                onResult(true, "Password reset instructions sent to your email.")
            }.onFailure {
                _authError.value = it.localizedMessage
                onResult(false, it.localizedMessage)
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch(dispatchers.io) {
            authRepository.deleteAccount()
        }
    }

    fun submitClaim(
        itemId: String,
        itemTitle: String,
        verificationAnswer: String,
        proofDetails: String,
        distinctiveFeatures: String = "",
        lostDateAndLocation: String = ""
    ) {
        viewModelScope.launch(dispatchers.io) {
            val user = currentUserState.value ?: return@launch
            val item = itemRepository.getItemPostById(itemId) ?: return@launch

            val newClaim = Claim(
                id = "claim_${System.currentTimeMillis()}",
                organizationId = user.organizationId,
                itemId = itemId,
                itemTitle = itemTitle,
                claimantId = user.id,
                claimantName = user.displayName,
                claimantContact = user.contactEmail.ifBlank { user.email },
                itemOwnerOrFinderId = item.userId,
                verificationAnswer = verificationAnswer,
                ownershipProofDetails = proofDetails,
                claimDescription = proofDetails,
                distinctiveFeatures = distinctiveFeatures,
                lostDateAndLocation = lostDateAndLocation,
                status = ClaimStatus.PENDING
            )

            claimRepository.submitClaim(newClaim)
        }
    }

    fun updateClaimStatus(claimId: String, status: ClaimStatus, notes: String) {
        viewModelScope.launch(dispatchers.io) {
            val user = currentUserState.value ?: return@launch
            claimRepository.updateClaimStatus(claimId, status, user.id, notes)
        }
    }

    fun withdrawReport(postId: String) {
        viewModelScope.launch(dispatchers.io) {
            itemRepository.updatePostStatus(postId, com.example.claimit.domain.model.PostStatus.ARCHIVED)
        }
    }

    fun editReport(post: com.example.claimit.domain.model.ItemPost) {
        viewModelScope.launch(dispatchers.io) {
            itemRepository.updateItemPost(post)
        }
    }

    fun confirmItemHandover(claimId: String, isUploader: Boolean, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(dispatchers.io) {
            val user = currentUserState.value ?: return@launch
            val result = claimRepository.confirmItemHandover(claimId, user.id, isUploader)
            result.onSuccess {
                onResult(true, "Handover confirmed successfully!")
            }.onFailure {
                onResult(false, it.localizedMessage ?: "Failed to confirm handover.")
            }
        }
    }

    fun reportItem(itemId: String, title: String, reason: ReportReason, details: String) {
        viewModelScope.launch(dispatchers.io) {
            val user = currentUserState.value ?: return@launch
            moderationRepository.reportContent(
                reporterId = user.id,
                reporterEmail = user.email,
                targetType = TargetType.ITEM,
                targetId = itemId,
                targetTitleOrName = title,
                reason = reason,
                details = details,
                organizationId = user.organizationId
            )
        }
    }

    fun resolveReport(reportId: String, status: ReportStatus) {
        viewModelScope.launch(dispatchers.io) {
            val user = currentUserState.value ?: return@launch
            moderationRepository.resolveReport(reportId, status, user.id)
        }
    }
}
