package com.example.claimit.presentation.postcreation

import android.graphics.Bitmap
import com.example.claimit.domain.model.AIVectorMetadata
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.DetectedAttribute
import com.example.claimit.domain.model.DuplicateMatchResult
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes

enum class ReportSubmissionStatus {
    IDLE,
    VALIDATING,
    UPLOADING_IMAGES,
    SAVING_REPORT,
    SUCCESS,
    ERROR
}

data class PostCreationUiState(
    val itemType: ItemType = ItemType.FOUND,
    val title: String = "",
    val description: String = "",
    val selectedCategory: ItemCategory = ItemCategory.ELECTRONICS,
    val customCategoryTag: String = "",
    val countryCode: String = "+91",
    val contactInfo: String = "",
    val actionTakenRemarks: String = "",
    val physicalAttributes: PhysicalAttributes = PhysicalAttributes(),

    // Media & AI pipeline state
    val capturedBitmaps: List<Bitmap> = emptyList(),
    val isAnalyzingImage: Boolean = false,
    val aiMetadata: AIVectorMetadata? = null,
    val suggestedTags: List<DetectedAttribute> = emptyList(),
    val acceptedTags: List<String> = emptyList(),

    // Geospatial micro-location state
    val availableBuildings: List<Building> = emptyList(),
    val locationSearchQuery: String = "",
    val filteredBuildings: List<Building> = emptyList(),
    val selectedBuilding: Building? = null,
    val customBuildingName: String = "",
    val selectedFloorWing: String = "",
    val microLocationDetails: String = "",

    // Duplicate detection & cross-match engine state
    val duplicateMatches: List<DuplicateMatchResult> = emptyList(),
    val isCheckingDuplicates: Boolean = false,
    val isDuplicateBannerExpanded: Boolean = true,
    val selectedDuplicateForComparison: DuplicateMatchResult? = null,

    // Form lifecycle state
    val submissionStatus: ReportSubmissionStatus = ReportSubmissionStatus.IDLE,
    val isSubmitting: Boolean = false,
    val uploadProgressMessage: String? = null,
    val createdPostId: String? = null,
    val errorMessage: String? = null
) {
    val selectedLocation: CampusLocation?
        get() {
            val building = selectedBuilding ?: return null
            val finalBuilding = if (building.id == "bld_others" && customBuildingName.isNotBlank()) {
                building.copy(name = customBuildingName.trim(), code = "OTHER")
            } else building
            return CampusLocation(
                building = finalBuilding,
                floorWing = selectedFloorWing,
                microLocationDetails = microLocationDetails
            )
        }

    val isContactPhoneValid: Boolean
        get() = contactInfo.trim().filter { it.isDigit() }.length >= 10

    val isFormValid: Boolean
        get() = title.trim().length >= 3 &&
                selectedBuilding != null &&
                (selectedBuilding.id != "bld_others" || customBuildingName.trim().isNotBlank()) &&
                isContactPhoneValid &&
                capturedBitmaps.isNotEmpty() &&
                !isSubmitting
}
