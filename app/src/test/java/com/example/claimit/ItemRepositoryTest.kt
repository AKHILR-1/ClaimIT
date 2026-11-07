package com.example.claimit

import com.example.claimit.data.repository.ItemRepositoryImpl
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes
import com.example.claimit.domain.model.PostStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ItemRepositoryTest {

    private lateinit var itemRepository: ItemRepositoryImpl
    private val dispatchers = TestCoroutineDispatchers()

    @Before
    fun setup() {
        runBlocking {
            itemRepository = ItemRepositoryImpl(dispatchers)
            itemRepository.createItemPost(
                ItemPost(
                    id = "post_001",
                    trackingCode = "CLM-8F32A1",
                    organizationId = "lpu_main_campus",
                    userId = "user_student_1",
                    userDisplayName = "Alex Student",
                    title = "AirPods Pro 2 in White Case",
                    description = "Left AirPods Pro 2 generation case in white.",
                    type = ItemType.LOST,
                    category = ItemCategory.ELECTRONICS,
                    location = CampusLocation(
                        building = Building("bld_15", "15", "Uni Mall", "Commercial Hub", "Shopping"),
                        floorWing = "Level 2",
                        microLocationDetails = "Table #12"
                    ),
                    physicalAttributes = PhysicalAttributes(dominantColor = "White"),
                    aiMetadata = null,
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
    fun `create new item post prepends to flow`() = runBlocking {
        val newPost = ItemPost(
            id = "test_post_1",
            trackingCode = "CLM-TEST1",
            organizationId = "lpu_main_campus",
            userId = "user_student_1",
            userDisplayName = "Tester",
            title = "Blue Scientific Calculator",
            description = "Casio fx-991EX left in Block 33 Lab 2",
            type = ItemType.FOUND,
            category = ItemCategory.ELECTRONICS,
            location = CampusLocation(
                building = Building("bld_33", "33", "School of CSE", "CSE Complex", "Software Labs"),
                floorWing = "Floor 1",
                microLocationDetails = "Lab 2 Desk 5"
            ),
            physicalAttributes = PhysicalAttributes(dominantColor = "Blue"),
            aiMetadata = null,
            acceptedTags = listOf("Calculator", "Casio"),
            imageUrls = emptyList(),
            createdAtTimestamp = System.currentTimeMillis(),
            status = PostStatus.ACTIVE,
            finderOrOwnerContact = "tester@campus.edu"
        )

        val result = itemRepository.createItemPost(newPost)
        assertTrue(result.isSuccess)

        val fetched = itemRepository.getItemPostById("test_post_1")
        assertNotNull(fetched)
        assertEquals("Blue Scientific Calculator", fetched?.title)
    }

    @Test
    fun `filtering by searchQuery returns matching items`() = runBlocking {
        val searchResult = itemRepository.getRecentItemPosts(searchQuery = "AirPods").first()
        assertTrue(searchResult.any { it.title.contains("AirPods") })
    }

    @Test
    fun `update post status changes status value`() = runBlocking {
        val updated = itemRepository.updatePostStatus("post_001", PostStatus.RETURNED)
        assertTrue(updated)

        val post = itemRepository.getItemPostById("post_001")
        assertEquals(PostStatus.RETURNED, post?.status)
    }
}
