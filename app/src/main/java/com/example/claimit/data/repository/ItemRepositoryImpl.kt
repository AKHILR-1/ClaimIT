package com.example.claimit.data.repository

import com.example.claimit.core.dispatchers.CoroutineDispatchers
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
import com.example.claimit.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ItemRepositoryImpl(
    private val dispatchers: CoroutineDispatchers
) : ItemRepository {

    private val mallBuilding = Building("bld_15", "15", "Uni Mall", "Commercial Hub", "LPU UniMall Shopping & Food Court")
    private val aiBuilding = Building("bld_25", "25", "School of AI and Emerging Technology", "Engineering & Tech", "AI & Emerging Tech Labs")
    private val healthBuilding = Building("bld_03", "03", "Uni Health Center", "Medical & Health", "University Health Center & Medical Clinic")
    private val libraryBuilding = Building("bld_37", "37", "Centre Library", "Central Academic", "Central University Library")

    // Pre-populated realistic campus listings referencing the exact LPU building index
    private val initialPosts = listOf(
        ItemPost(
            id = "post_001",
            trackingCode = "CLM-8F32A1",
            title = "AirPods Pro 2 in White Case",
            description = "Left AirPods Pro 2 generation case in white with a small scratch on the rear hinge. Found near Food Court area at Uni Mall.",
            type = ItemType.LOST,
            category = ItemCategory.ELECTRONICS,
            location = CampusLocation(
                building = mallBuilding,
                floorWing = "Level 2 Food Plaza",
                microLocationDetails = "Near Central Table #12"
            ),
            physicalAttributes = PhysicalAttributes(
                dominantColor = "White",
                material = "Glossy Plastic",
                condition = "Used - Minor scratch",
                distinguishingFeatures = "Hinge mark"
            ),
            aiMetadata = AIVectorMetadata(
                vectorEmbedding = List(128) { 0.8f },
                structuralTags = listOf("AirPods Pro 2", "White Case", "Scratch on Hinge"),
                detectedAttributes = listOf(
                    DetectedAttribute("1", "AirPods Pro 2", AttributeType.BRAND_MODEL, 0.95f),
                    DetectedAttribute("2", "White Case", AttributeType.COLOR, 0.92f)
                ),
                overallConfidence = 0.94f,
                analyzedAtTimestamp = System.currentTimeMillis() - 3600000
            ),
            acceptedTags = listOf("AirPods Pro 2", "White Case"),
            imageUrls = emptyList(),
            createdAtTimestamp = System.currentTimeMillis() - (2 * 3600000), // 2h ago
            status = PostStatus.ACTIVE,
            finderOrOwnerContact = "alex.student@campus.edu",
            actionTakenRemarks = "Submitted at Block 15 Security Desk"
        ),
        ItemPost(
            id = "post_002",
            trackingCode = "CLM-12B9C8",
            title = "Black Hydro Flask Water Bottle 32oz",
            description = "Matte black Hydro Flask with NASA & AI Society stickers. Left on bench outside Block 25.",
            type = ItemType.LOST,
            category = ItemCategory.OTHER,
            location = CampusLocation(
                building = aiBuilding,
                floorWing = "Floor 2",
                microLocationDetails = "AI Robotics Lab Bench"
            ),
            physicalAttributes = PhysicalAttributes(
                dominantColor = "Matte Black",
                material = "Stainless Steel",
                condition = "Good",
                distinguishingFeatures = "NASA & AI stickers"
            ),
            aiMetadata = null,
            acceptedTags = listOf("Hydro Flask", "Matte Black", "Stickers"),
            imageUrls = emptyList(),
            createdAtTimestamp = System.currentTimeMillis() - (5 * 3600000), // 5h ago
            status = PostStatus.ACTIVE,
            finderOrOwnerContact = "jordan.eng@campus.edu",
            actionTakenRemarks = "Left at Block 25 Security Counter"
        ),
        ItemPost(
            id = "post_003",
            trackingCode = "CLM-99D0F1",
            title = "Student ID Card - Marcus Vance",
            description = "Campus Student Access Card found on the reading table at Centre Library Block 37.",
            type = ItemType.FOUND,
            category = ItemCategory.WALLETS_CARDS,
            location = CampusLocation(
                building = libraryBuilding,
                floorWing = "1st Floor Quiet Reading Zone",
                microLocationDetails = "Table #4"
            ),
            physicalAttributes = PhysicalAttributes(
                dominantColor = "Blue/White",
                material = "PVC Card",
                condition = "Clean",
                distinguishingFeatures = "ID #8820319"
            ),
            aiMetadata = null,
            acceptedTags = listOf("Student ID", "Marcus Vance"),
            imageUrls = emptyList(),
            createdAtTimestamp = System.currentTimeMillis() - (12 * 3600000),
            status = PostStatus.ACTIVE,
            finderOrOwnerContact = "lib.desk@campus.edu",
            actionTakenRemarks = "Submitted at Central Library Circulation Desk"
        ),
        ItemPost(
            id = "post_004",
            trackingCode = "CLM-44A7E2",
            title = "Prescription Eyeglasses - Black Frame",
            description = "Ray-Ban prescription glasses in brown leather case. Found in OPD waiting room at Uni Health Center Block 03.",
            type = ItemType.FOUND,
            category = ItemCategory.ACCESSORIES,
            location = CampusLocation(
                building = healthBuilding,
                floorWing = "Ground Floor OPD",
                microLocationDetails = "OPD Reception Waiting Bench"
            ),
            physicalAttributes = PhysicalAttributes(
                dominantColor = "Black",
                material = "Acetate",
                condition = "Excellent",
                distinguishingFeatures = "Ray-Ban logo"
            ),
            aiMetadata = null,
            acceptedTags = listOf("Prescription Glasses", "Ray-Ban"),
            imageUrls = emptyList(),
            createdAtTimestamp = System.currentTimeMillis() - (18 * 3600000),
            status = PostStatus.ACTIVE,
            finderOrOwnerContact = "health.clinic@campus.edu",
            actionTakenRemarks = "Submitted at Uni Health Center Reception"
        )
    )

    private val postsStateFlow = MutableStateFlow(initialPosts)

    override suspend fun createItemPost(post: ItemPost): Result<String> = withContext(dispatchers.io) {
        try {
            val updated = postsStateFlow.value.toMutableList()
            updated.add(0, post)
            postsStateFlow.value = updated
            Result.success(post.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getItemPostById(id: String): ItemPost? = withContext(dispatchers.io) {
        postsStateFlow.value.firstOrNull { it.id == id }
    }

    override fun getRecentItemPosts(type: ItemType?): Flow<List<ItemPost>> {
        return postsStateFlow.map { list ->
            if (type == null) list else list.filter { it.type == type }
        }
    }

    override suspend fun updatePostStatus(id: String, status: PostStatus): Boolean = withContext(dispatchers.io) {
        val updated = postsStateFlow.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
        postsStateFlow.value = updated
        true
    }
}
