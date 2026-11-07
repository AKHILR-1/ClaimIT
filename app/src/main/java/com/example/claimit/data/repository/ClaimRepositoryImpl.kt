package com.example.claimit.data.repository

import android.util.Log
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.Claim
import com.example.claimit.domain.model.ClaimStatus
import com.example.claimit.domain.model.NotificationType
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.repository.ClaimRepository
import com.example.claimit.domain.repository.ItemRepository
import com.example.claimit.domain.repository.NotificationRepository
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ClaimRepositoryImpl(
    private val itemRepository: ItemRepository,
    private val dispatchers: CoroutineDispatchers,
    private val notificationRepository: NotificationRepository? = null,
    private val realtimeDatabase: FirebaseDatabase? = try {
        FirebaseDatabase.getInstance("https://claimit-322b5-default-rtdb.asia-southeast1.firebasedatabase.app")
    } catch (_: Exception) {
        try { FirebaseDatabase.getInstance() } catch (_: Exception) { null }
    },
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }
) : ClaimRepository {

    private val TAG = "ClaimRepository"
    private val claimsStateFlow = MutableStateFlow<List<Claim>>(emptyList())

    override suspend fun submitClaim(claim: Claim): Result<String> = withContext(dispatchers.io) {
        try {
            val current = claimsStateFlow.value.toMutableList()
            val exists = current.any {
                it.itemId == claim.itemId &&
                it.claimantId == claim.claimantId &&
                (it.status == ClaimStatus.PENDING || it.status == ClaimStatus.APPROVED)
            }
            if (exists) {
                return@withContext Result.failure(Exception("You already have an active claim for this item."))
            }

            // Persist to Realtime Database
            realtimeDatabase?.let { rtdb ->
                try {
                    val claimMap = mapOf(
                        "id" to claim.id,
                        "claimId" to claim.id,
                        "organizationId" to claim.organizationId,
                        "itemId" to claim.itemId,
                        "itemTitle" to claim.itemTitle,
                        "claimantUid" to claim.claimantId,
                        "claimantId" to claim.claimantId,
                        "claimantName" to claim.claimantName,
                        "claimantContact" to claim.claimantContact,
                        "uploaderUid" to claim.itemOwnerOrFinderId,
                        "itemOwnerOrFinderId" to claim.itemOwnerOrFinderId,
                        "verificationAnswer" to claim.verificationAnswer,
                        "ownershipProofDetails" to claim.ownershipProofDetails,
                        "claimDescription" to claim.ownershipProofDetails,
                        "status" to claim.status.name,
                        "createdAt" to ServerValue.TIMESTAMP,
                        "updatedAt" to ServerValue.TIMESTAMP
                    )
                    rtdb.reference.child("claims").child(claim.id).setValue(claimMap).await()
                } catch (e: Exception) {
                    Log.w(TAG, "RTDB write for claim ${claim.id} error: ${e.message}")
                }
            }

            // Persist to Firestore
            firestore?.let { db ->
                try {
                    val firestoreMap = mapOf(
                        "id" to claim.id,
                        "organizationId" to claim.organizationId,
                        "itemId" to claim.itemId,
                        "itemTitle" to claim.itemTitle,
                        "claimantUid" to claim.claimantId,
                        "uploaderUid" to claim.itemOwnerOrFinderId,
                        "verificationAnswer" to claim.verificationAnswer,
                        "ownershipProofDetails" to claim.ownershipProofDetails,
                        "status" to claim.status.name,
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("claims").document(claim.id).set(firestoreMap).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore write for claim ${claim.id} error: ${e.message}")
                }
            }

            current.add(0, claim)
            claimsStateFlow.value = current

            // Update item status to CLAIM_PENDING
            itemRepository.updatePostStatus(claim.itemId, PostStatus.CLAIM_PENDING)

            // Notify Listing Owner
            notificationRepository?.sendNotification(
                userId = claim.itemOwnerOrFinderId,
                title = "New Claim Request",
                message = "Someone has submitted an ownership claim for your found item '${claim.itemTitle}'. Tap to review details.",
                type = NotificationType.NEW_CLAIM_REQUEST,
                itemId = claim.itemId,
                claimId = claim.id,
                senderUid = claim.claimantId
            )

            Result.success(claim.id)
        } catch (e: Exception) {
            Log.e(TAG, "Submit claim exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getClaimsForUser(userId: String): Flow<List<Claim>> {
        return claimsStateFlow.map { list ->
            list.filter { it.claimantId == userId }
        }
    }

    override fun getClaimsForListingOwner(ownerId: String): Flow<List<Claim>> {
        return claimsStateFlow.map { list ->
            list.filter { it.itemOwnerOrFinderId == ownerId }
        }
    }

    override fun getClaimsForItem(itemId: String): Flow<List<Claim>> {
        return claimsStateFlow.map { list ->
            list.filter { it.itemId == itemId }
        }
    }

    override suspend fun updateClaimStatus(
        claimId: String,
        status: ClaimStatus,
        reviewerId: String,
        reviewNotes: String
    ): Result<Unit> = withContext(dispatchers.io) {
        try {
            var targetClaim: Claim? = null
            val updated = claimsStateFlow.value.map { claim ->
                if (claim.id == claimId) {
                    val newClaim = claim.copy(
                        status = status,
                        reviewerId = reviewerId,
                        reviewNotes = reviewNotes,
                        updatedAtTimestamp = System.currentTimeMillis(),
                        reviewedAtTimestamp = System.currentTimeMillis()
                    )
                    targetClaim = newClaim
                    newClaim
                } else claim
            }
            claimsStateFlow.value = updated

            targetClaim?.let { claim ->
                // Update RTDB
                realtimeDatabase?.let { rtdb ->
                    try {
                        val updates = mapOf(
                            "status" to status.name,
                            "reviewerId" to reviewerId,
                            "reviewNotes" to reviewNotes,
                            "reviewedAt" to ServerValue.TIMESTAMP,
                            "updatedAt" to ServerValue.TIMESTAMP
                        )
                        rtdb.reference.child("claims").child(claimId).updateChildren(updates).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "RTDB status update error: ${e.message}")
                    }
                }

                // Update Firestore
                firestore?.let { db ->
                    try {
                        db.collection("claims").document(claimId).update(
                            mapOf(
                                "status" to status.name,
                                "reviewerId" to reviewerId,
                                "reviewNotes" to reviewNotes,
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Firestore claim status update error: ${e.message}")
                    }
                }

                when (status) {
                    ClaimStatus.APPROVED -> {
                        itemRepository.updatePostStatus(claim.itemId, PostStatus.CLAIM_APPROVED)
                        // Send notification to claimant
                        notificationRepository?.sendNotification(
                            userId = claim.claimantId,
                            title = "Your Claim Has Been Approved",
                            message = "The uploader approved your claim for '${claim.itemTitle}'. Arrange a safe handover with them.",
                            type = NotificationType.CLAIM_APPROVED,
                            itemId = claim.itemId,
                            claimId = claim.id,
                            senderUid = reviewerId
                        )
                    }
                    ClaimStatus.REJECTED -> {
                        itemRepository.updatePostStatus(claim.itemId, PostStatus.ACTIVE)
                        // Send notification to claimant
                        notificationRepository?.sendNotification(
                            userId = claim.claimantId,
                            title = "Claim Decision Update",
                            message = "Your claim for '${claim.itemTitle}' was rejected. Reason: ${reviewNotes.ifBlank { "Unspecified" }}",
                            type = NotificationType.CLAIM_REJECTED,
                            itemId = claim.itemId,
                            claimId = claim.id,
                            senderUid = reviewerId
                        )
                    }
                    ClaimStatus.CANCELLED -> {
                        itemRepository.updatePostStatus(claim.itemId, PostStatus.ACTIVE)
                    }
                    else -> {}
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update claim status failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun confirmItemHandover(
        claimId: String,
        userId: String,
        isUploader: Boolean
    ): Result<Unit> = withContext(dispatchers.io) {
        try {
            var targetClaim: Claim? = null
            val updated = claimsStateFlow.value.map { claim ->
                if (claim.id == claimId) {
                    val newUploaderConf = if (isUploader) true else claim.uploaderConfirmedReturn
                    val newClaimantConf = if (!isUploader) true else claim.claimantConfirmedReturn
                    val newStatus = ClaimStatus.RETURNED

                    val newClaim = claim.copy(
                        status = newStatus,
                        uploaderConfirmedReturn = newUploaderConf,
                        claimantConfirmedReturn = newClaimantConf,
                        updatedAtTimestamp = System.currentTimeMillis(),
                        returnedAtTimestamp = System.currentTimeMillis()
                    )
                    targetClaim = newClaim
                    newClaim
                } else claim
            }
            claimsStateFlow.value = updated

            targetClaim?.let { claim ->
                itemRepository.updatePostStatus(claim.itemId, PostStatus.RETURNED)

                // RTDB update
                realtimeDatabase?.let { rtdb ->
                    try {
                        val updates = mapOf(
                            "status" to ClaimStatus.RETURNED.name,
                            "uploaderConfirmedReturn" to claim.uploaderConfirmedReturn,
                            "claimantConfirmedReturn" to claim.claimantConfirmedReturn,
                            "returnedAt" to ServerValue.TIMESTAMP,
                            "updatedAt" to ServerValue.TIMESTAMP
                        )
                        rtdb.reference.child("claims").child(claimId).updateChildren(updates).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "RTDB handover confirmation update error: ${e.message}")
                    }
                }

                // Notify claimant
                notificationRepository?.sendNotification(
                    userId = claim.claimantId,
                    title = "Item Successfully Returned",
                    message = "The handover for '${claim.itemTitle}' has been confirmed. Thank you for using ClaimIT!",
                    type = NotificationType.ITEM_RETURNED,
                    itemId = claim.itemId,
                    claimId = claim.id,
                    senderUid = userId
                )

                // Notify uploader
                notificationRepository?.sendNotification(
                    userId = claim.itemOwnerOrFinderId,
                    title = "Item Successfully Returned",
                    message = "The handover for '${claim.itemTitle}' has been confirmed returned.",
                    type = NotificationType.ITEM_RETURNED,
                    itemId = claim.itemId,
                    claimId = claim.id,
                    senderUid = userId
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Confirm item handover error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
