package com.example.claimit.domain.model

enum class ItemType {
    LOST,
    FOUND
}

enum class PostStatus {
    DRAFT,
    ACTIVE,
    MATCHED,
    RESOLVED,
    ARCHIVED
}

enum class ItemCategory(val displayName: String) {
    ELECTRONICS("Electronics & Tech"),
    KEYS("Keys & Access"),
    WALLETS_CARDS("Wallets & IDs"),
    APPAREL("Apparel & Wearables"),
    BAGS_BACKPACKS("Bags & Luggage"),
    DOCUMENTS_BOOKS("Documents & Books"),
    ACCESSORIES("Glasses & Accessories"),
    OTHER("Other Personal Items")
}

data class ItemPost(
    val id: String,
    val trackingCode: String,
    val title: String,
    val description: String,
    val type: ItemType,
    val category: ItemCategory,
    val location: CampusLocation,
    val physicalAttributes: PhysicalAttributes,
    val aiMetadata: AIVectorMetadata?,
    val acceptedTags: List<String>,
    val imageUrls: List<String>,
    val createdAtTimestamp: Long,
    val status: PostStatus = PostStatus.ACTIVE,
    val finderOrOwnerContact: String,
    val actionTakenRemarks: String = ""
)
