package com.example.claimit

import com.example.claimit.data.repository.ItemRepositoryImpl
import com.example.claimit.data.repository.ModerationRepositoryImpl
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.model.ReportReason
import com.example.claimit.domain.model.ReportStatus
import com.example.claimit.domain.model.TargetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ModerationRepositoryTest {

    private lateinit var itemRepository: ItemRepositoryImpl
    private lateinit var moderationRepository: ModerationRepositoryImpl
    private val dispatchers = TestCoroutineDispatchers()

    @Before
    fun setup() {
        runBlocking {
            itemRepository = ItemRepositoryImpl(dispatchers)
            moderationRepository = ModerationRepositoryImpl(itemRepository, dispatchers)

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

            moderationRepository.reportContent(
                reporterId = "user_student_9",
                reporterEmail = "student9@campus.edu",
                targetType = TargetType.ITEM,
                targetId = "post_002",
                targetTitleOrName = "Black Hydro Flask Water Bottle 32oz",
                reason = ReportReason.SPAM_OR_DUPLICATE,
                details = "Listing appears to be duplicate report from earlier today.",
                organizationId = "lpu_main_campus"
            )
        }
    }

    @Test
    fun `reporting item adds report to admin queue`() = runBlocking {
        val result = moderationRepository.reportContent(
            reporterId = "user_student_1",
            reporterEmail = "student@campus.edu",
            targetType = TargetType.ITEM,
            targetId = "post_003",
            targetTitleOrName = "Student ID Card",
            reason = ReportReason.INAPPROPRIATE_CONTENT,
            details = "Testing reporting flow"
        )
        assertTrue(result.isSuccess)

        val queue = moderationRepository.getReportedContentQueue().first()
        assertTrue(queue.any { it.targetId == "post_003" })
    }

    @Test
    fun `resolving report with RESOLVED_REMOVED updates item status to REMOVED`() = runBlocking {
        val queue = moderationRepository.getReportedContentQueue().first()
        val report = queue.first { it.targetId == "post_002" }

        val result = moderationRepository.resolveReport(
            reportId = report.id,
            status = ReportStatus.RESOLVED_REMOVED,
            adminId = "user_admin_1"
        )
        assertTrue(result.isSuccess)

        val item = itemRepository.getItemPostById("post_002")
        assertEquals(PostStatus.REMOVED, item?.status)
    }
}
