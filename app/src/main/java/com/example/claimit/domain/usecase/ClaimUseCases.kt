package com.example.claimit.domain.usecase

import com.example.claimit.domain.model.Claim
import com.example.claimit.domain.model.ClaimStatus
import com.example.claimit.domain.repository.ClaimRepository
import kotlinx.coroutines.flow.Flow

class ClaimUseCases(
    private val claimRepository: ClaimRepository
) {
    suspend fun submitClaim(claim: Claim): Result<String> {
        return claimRepository.submitClaim(claim)
    }

    fun getClaimsForUser(userId: String): Flow<List<Claim>> {
        return claimRepository.getClaimsForUser(userId)
    }

    fun getClaimsForListingOwner(ownerId: String): Flow<List<Claim>> {
        return claimRepository.getClaimsForListingOwner(ownerId)
    }

    suspend fun updateClaimStatus(
        claimId: String,
        status: ClaimStatus,
        reviewerId: String,
        reviewNotes: String = ""
    ): Result<Unit> {
        return claimRepository.updateClaimStatus(claimId, status, reviewerId, reviewNotes)
    }

    suspend fun confirmItemHandover(
        claimId: String,
        userId: String,
        isUploader: Boolean
    ): Result<Unit> {
        return claimRepository.confirmItemHandover(claimId, userId, isUploader)
    }
}
