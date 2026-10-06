package com.example.claimit.presentation.postcreation

import android.graphics.Bitmap
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.DetectedAttribute
import com.example.claimit.domain.model.DuplicateMatchResult
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemType

sealed interface PostCreationIntent {
    data class SelectItemType(val type: ItemType) : PostCreationIntent
    data class UpdateTitle(val title: String) : PostCreationIntent
    data class UpdateDescription(val description: String) : PostCreationIntent
    data class SelectCategory(val category: ItemCategory) : PostCreationIntent
    data class UpdateCustomCategoryTag(val tag: String) : PostCreationIntent
    data class UpdateContactInfo(val contact: String) : PostCreationIntent
    data class UpdateActionTakenRemarks(val remarks: String) : PostCreationIntent

    // Physical attributes updates
    data class UpdateDominantColor(val color: String) : PostCreationIntent
    data class UpdateMaterial(val material: String) : PostCreationIntent
    data class UpdateCondition(val condition: String) : PostCreationIntent
    data class UpdateDistinguishingFeatures(val features: String) : PostCreationIntent

    // Image capture & AI suggestions
    data class AddCapturedBitmap(val bitmap: Bitmap) : PostCreationIntent
    data class RemoveCapturedBitmap(val index: Int) : PostCreationIntent
    data class AcceptSuggestedTag(val attribute: DetectedAttribute) : PostCreationIntent
    data class DismissSuggestedTag(val attribute: DetectedAttribute) : PostCreationIntent
    data class AddCustomTag(val tag: String) : PostCreationIntent
    data class RemoveAcceptedTag(val tag: String) : PostCreationIntent

    // Location selection
    data class SearchLocation(val query: String) : PostCreationIntent
    data class SelectBuilding(val building: Building) : PostCreationIntent
    data class UpdateCustomBuildingName(val customName: String) : PostCreationIntent
    data class SelectFloorWing(val floorWing: String) : PostCreationIntent
    data class UpdateMicroLocation(val details: String) : PostCreationIntent

    // Duplicate detection controls
    object ToggleDuplicateBanner : PostCreationIntent
    data class SelectDuplicateForComparison(val match: DuplicateMatchResult?) : PostCreationIntent
    data class VerifyAndConnect(val match: DuplicateMatchResult) : PostCreationIntent

    // Action intents
    object SubmitPost : PostCreationIntent
    object DismissError : PostCreationIntent
}
