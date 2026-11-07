package com.example.claimit

import com.example.claimit.data.repository.DuplicateDetectionRepositoryImpl
import com.example.claimit.data.repository.ItemRepositoryImpl
import com.example.claimit.domain.model.AIVectorMetadata
import com.example.claimit.domain.model.AttributeType
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.DetectedAttribute
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes
import com.example.claimit.domain.model.PostStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DuplicateDetectionRepositoryTest {

    private lateinit var itemRepository: ItemRepositoryImpl
    private lateinit var duplicateRepository: DuplicateDetectionRepositoryImpl
    private val dispatchers = TestCoroutineDispatchers()

    @Before
    fun setup() {
        runBlocking {
            itemRepository = ItemRepositoryImpl(dispatchers)
            duplicateRepository = DuplicateDetectionRepositoryImpl(itemRepository, dispatchers)

            val mallBuilding = Building("bld_15", "15", "Uni Mall", "Commercial Hub", "Shopping")
            itemRepository.createItemPost(
                ItemPost(
                    id = "post_001",
                    trackingCode = "CLM-8F32A1",
                    organizationId = "lpu_main_campus",
                    userId = "user_student_1",
                    userDisplayName = "Alex Student",
                    title = "AirPods Pro 2 in White Case",
                    description = "Left AirPods Pro 2 case",
                    type = ItemType.LOST,
                    category = ItemCategory.ELECTRONICS,
                    location = CampusLocation(mallBuilding, "Level 2 Food Plaza", "Table #12"),
                    physicalAttributes = PhysicalAttributes(dominantColor = "White"),
                    aiMetadata = AIVectorMetadata(
                        vectorEmbedding = List(128) { 0.8f },
                        structuralTags = listOf("AirPods Pro 2"),
                        detectedAttributes = listOf(
                            DetectedAttribute("1", "White", AttributeType.COLOR, 0.9f)
                        ),
                        overallConfidence = 0.9f,
                        analyzedAtTimestamp = System.currentTimeMillis()
                    ),
                    acceptedTags = listOf("AirPods Pro 2"),
                    imageUrls = emptyList(),
                    createdAtTimestamp = System.currentTimeMillis(),
                    status = PostStatus.ACTIVE,
                    finderOrOwnerContact = "alex@campus.edu"
                )
            )
        }
    }

    @Test
    fun `detecting duplicates against opposite post type returns matches when title overlaps`() = runBlocking {
        val mallBuilding = Building("bld_15", "15", "Uni Mall", "Commercial Hub", "Shopping")
        val testLocation = CampusLocation(mallBuilding, "Level 2 Food Plaza", "Table #12")

        val result = duplicateRepository.detectDuplicates(
            type = ItemType.FOUND,
            category = ItemCategory.ELECTRONICS,
            title = "AirPods Pro 2 White Case",
            description = "Found white AirPods Pro 2 case near food court",
            location = testLocation,
            aiMetadata = AIVectorMetadata(
                vectorEmbedding = List(128) { 0.8f },
                structuralTags = listOf("AirPods Pro 2"),
                detectedAttributes = listOf(
                    DetectedAttribute("1", "White", AttributeType.COLOR, 0.9f)
                ),
                overallConfidence = 0.9f,
                analyzedAtTimestamp = System.currentTimeMillis()
            ),
            threshold = 0.50f
        )

        assertTrue(result.isSuccess)
        val matches = result.getOrNull()
        assertNotNull(matches)
        assertTrue(matches!!.isNotEmpty())
        assertTrue(matches.first().matchedPost.title.contains("AirPods"))
    }
}
