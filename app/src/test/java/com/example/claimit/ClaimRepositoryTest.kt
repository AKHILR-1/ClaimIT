package com.example.claimit

import com.example.claimit.data.repository.ClaimRepositoryImpl
import com.example.claimit.data.repository.ItemRepositoryImpl
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.Claim
import com.example.claimit.domain.model.ClaimStatus
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes
import com.example.claimit.domain.model.PostStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClaimRepositoryTest {

    private lateinit var itemRepository: ItemRepositoryImpl
    private lateinit var claimRepository: ClaimRepositoryImpl
    private val dispatchers = TestCoroutineDispatchers()

    @Before
    fun setup() {
        runBlocking {
            itemRepository = ItemRepositoryImpl(dispatchers)
            claimRepository = ClaimRepositoryImpl(itemRepository, dispatchers)

            itemRepository.createItemPost(
                ItemPost(
                    id = "post_001",
                    trackingCode = "CLM-8F32A1",
                    organizationId = "lpu_main_campus",
                    userId = "user_jordan",
                    userDisplayName = "Jordan",
                    title = "AirPods Pro 2 in White Case",
                    description = "Left AirPods Pro 2 case",
                    type = ItemType.LOST,
                    category = ItemCategory.ELECTRONICS,
                    location = CampusLocation(
                        building = Building("bld_15", "15", "Uni Mall", "Commercial Hub", "Shopping"),
                        floorWing = "Level 2",
                        microLocationDetails = "Table #12"
                    ),
                    physicalAttributes = PhysicalAttributes(dominantColor = "White"),
                    aiMetadata = null,
                    acceptedTags = listOf("AirPods"),
                    imageUrls = emptyList(),
                    createdAtTimestamp = System.currentTimeMillis(),
                    status = PostStatus.ACTIVE,
                    finderOrOwnerContact = "jordan@campus.edu"
                )
            )

            itemRepository.createItemPost(
                ItemPost(
                    id = "post_002",
                    trackingCode = "CLM-12B9C8",
                    organizationId = "lpu_main_campus",
                    userId = "user_jordan",
                    userDisplayName = "Jordan",
                    title = "Black Hydro Flask Water Bottle 32oz",
                    description = "Matte black Hydro Flask",
                    type = ItemType.LOST,
                    category = ItemCategory.OTHER,
                    location = CampusLocation(
                        building = Building("bld_25", "25", "School of AI", "Tech", "Labs"),
                        floorWing = "Floor 2",
                        microLocationDetails = "Bench"
                    ),
                    physicalAttributes = PhysicalAttributes(dominantColor = "Black"),
                    aiMetadata = null,
                    acceptedTags = listOf("Hydro Flask"),
                    imageUrls = emptyList(),
                    createdAtTimestamp = System.currentTimeMillis(),
                    status = PostStatus.ACTIVE,
                    finderOrOwnerContact = "jordan@campus.edu"
                )
            )

            claimRepository.submitClaim(
                Claim(
                    id = "claim_101",
                    organizationId = "lpu_main_campus",
                    itemId = "post_001",
                    itemTitle = "AirPods Pro 2 in White Case",
                    claimantId = "user_student_1",
                    claimantName = "Alex Student",
                    claimantContact = "alex.student@campus.edu",
                    itemOwnerOrFinderId = "user_jordan",
                    verificationAnswer = "Scratch on back hinge",
                    ownershipProofDetails = "Invoice available",
                    status = ClaimStatus.PENDING
                )
            )
        }
    }

    @Test
    fun `submitting claim adds claim and sets item status to CLAIM_PENDING`() = runBlocking {
        val claim = Claim(
            id = "claim_test_1",
            organizationId = "lpu_main_campus",
            itemId = "post_002",
            itemTitle = "Black Hydro Flask Water Bottle 32oz",
            claimantId = "user_student_5",
            claimantName = "Sam Student",
            claimantContact = "sam@campus.edu",
            itemOwnerOrFinderId = "user_jordan",
            verificationAnswer = "GitHub sticker next to NASA",
            ownershipProofDetails = "Bought at Uni Mall"
        )

        val result = claimRepository.submitClaim(claim)
        assertTrue(result.isSuccess)

        val item = itemRepository.getItemPostById("post_002")
        assertEquals(PostStatus.CLAIM_PENDING, item?.status)

        val userClaims = claimRepository.getClaimsForUser("user_student_5").first()
        assertTrue(userClaims.any { it.id == "claim_test_1" })
    }

    @Test
    fun `approving claim sets claim status to APPROVED and item to CLAIM_APPROVED`() = runBlocking {
        val result = claimRepository.updateClaimStatus(
            claimId = "claim_101",
            status = ClaimStatus.APPROVED,
            reviewerId = "user_jordan",
            reviewNotes = "Correct verification answer"
        )
        assertTrue(result.isSuccess)

        val claims = claimRepository.getClaimsForUser("user_student_1").first()
        val claim = claims.find { it.id == "claim_101" }
        assertEquals(ClaimStatus.APPROVED, claim?.status)

        val item = itemRepository.getItemPostById("post_001")
        assertEquals(PostStatus.CLAIM_APPROVED, item?.status)
    }

    @Test
    fun `confirming item handover sets claim status to RETURNED and item to RETURNED`() = runBlocking {
        claimRepository.updateClaimStatus(
            claimId = "claim_101",
            status = ClaimStatus.APPROVED,
            reviewerId = "user_jordan",
            reviewNotes = "Approved"
        )

        val handoverResult = claimRepository.confirmItemHandover("claim_101", "user_jordan", true)
        assertTrue(handoverResult.isSuccess)

        val claims = claimRepository.getClaimsForUser("user_student_1").first()
        val claim = claims.find { it.id == "claim_101" }
        assertEquals(ClaimStatus.RETURNED, claim?.status)

        val item = itemRepository.getItemPostById("post_001")
        assertEquals(PostStatus.RETURNED, item?.status)
    }
}
