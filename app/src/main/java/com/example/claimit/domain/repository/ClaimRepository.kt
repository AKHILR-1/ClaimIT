package com.example.claimit.domain.repository

import com.example.claimit.domain.model.Claim
import com.example.claimit.domain.model.ClaimStatus
import kotlinx.coroutines.flow.Flow

interface ClaimRepository {
    suspend fun submitClaim(claim: Claim): Result<String>
    fun getClaimsForUser(userId: String): Flow<List<Claim>>
    fun getClaimsForListingOwner(ownerId: String): Flow<List<Claim>>
    fun getClaimsForItem(itemId: String): Flow<List<Claim>>
    suspend fun updateClaimStatus(claimId: String, status: ClaimStatus, reviewerId: String, reviewNotes: String = ""): Result<Unit>
    suspend fun confirmItemHandover(claimId: String, userId: String, isUploader: Boolean): Result<Unit>
}
