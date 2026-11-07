package com.example.claimit.domain.model

enum class ClaimStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED
}

data class Claim(
    val id: String,
    val organizationId: String,
    val itemId: String,
    val itemTitle: String,
    val claimantId: String,
    val claimantName: String,
    val claimantContact: String,
    val itemOwnerOrFinderId: String,
    val verificationAnswer: String = "",
    val ownershipProofDetails: String = "",
    val claimDescription: String = ownershipProofDetails,
    val distinctiveFeatures: String = "",
    val lostDateAndLocation: String = "",
    val status: ClaimStatus = ClaimStatus.PENDING,
    val reviewerId: String? = null,
    val reviewNotes: String = "",
    val uploaderConfirmedReturn: Boolean = false,
    val claimantConfirmedReturn: Boolean = false,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val updatedAtTimestamp: Long = System.currentTimeMillis(),
    val reviewedAtTimestamp: Long? = null,
    val returnedAtTimestamp: Long? = null
)
