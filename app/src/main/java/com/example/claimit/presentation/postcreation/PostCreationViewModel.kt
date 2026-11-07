package com.example.claimit.presentation.postcreation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.data.repository.ItemRepositoryImpl
import com.example.claimit.domain.model.AttributeType
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.DetectedAttribute
import com.example.claimit.domain.model.DuplicateMatchResult
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.repository.AuthRepository
import com.example.claimit.domain.repository.CampusLocationRepository
import com.example.claimit.domain.repository.ItemRepository
import com.example.claimit.domain.usecase.AnalyzeItemImageUseCase
import com.example.claimit.domain.usecase.CreateItemPostUseCase
import com.example.claimit.domain.usecase.DetectDuplicateListingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class PostCreationViewModel(
    private val analyzeItemImageUseCase: AnalyzeItemImageUseCase,
    private val detectDuplicateListingsUseCase: DetectDuplicateListingsUseCase,
    private val createItemPostUseCase: CreateItemPostUseCase,
    private val locationRepository: CampusLocationRepository,
    private val dispatchers: CoroutineDispatchers,
    private val authRepository: AuthRepository? = null,
    private val itemRepository: ItemRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostCreationUiState())
    val uiState: StateFlow<PostCreationUiState> = _uiState.asStateFlow()

    private val _sideEffectChannel = Channel<PostCreationSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffectChannel.receiveAsFlow()

    private var duplicateCheckJob: Job? = null
    private var locationSearchJob: Job? = null

    init {
        loadInitialCampusBuildings()
    }

    private fun loadInitialCampusBuildings() {
        viewModelScope.launch(dispatchers.io) {
            val buildings = locationRepository.getBuildings()
            _uiState.update { state ->
                state.copy(
                    availableBuildings = buildings,
                    filteredBuildings = buildings
                )
            }
        }
    }

    fun onIntent(intent: PostCreationIntent) {
        when (intent) {
            is PostCreationIntent.SelectItemType -> {
                _uiState.update { it.copy(itemType = intent.type) }
                triggerDuplicateCheckDebounced()
            }
            is PostCreationIntent.UpdateTitle -> {
                _uiState.update { it.copy(title = intent.title) }
                triggerDuplicateCheckDebounced()
            }
            is PostCreationIntent.UpdateDescription -> {
                _uiState.update { it.copy(description = intent.description) }
                triggerDuplicateCheckDebounced()
            }
            is PostCreationIntent.SelectCategory -> {
                _uiState.update { it.copy(selectedCategory = intent.category) }
                triggerDuplicateCheckDebounced()
            }
            is PostCreationIntent.UpdateCustomCategoryTag -> {
                _uiState.update { it.copy(customCategoryTag = intent.tag) }
            }
            is PostCreationIntent.UpdateContactInfo -> {
                _uiState.update { it.copy(contactInfo = intent.contact) }
            }
            is PostCreationIntent.UpdateActionTakenRemarks -> {
                _uiState.update { it.copy(actionTakenRemarks = intent.remarks) }
            }
            is PostCreationIntent.UpdateDominantColor -> {
                _uiState.update { state ->
                    state.copy(physicalAttributes = state.physicalAttributes.copy(dominantColor = intent.color))
                }
            }
            is PostCreationIntent.UpdateMaterial -> {
                _uiState.update { state ->
                    state.copy(physicalAttributes = state.physicalAttributes.copy(material = intent.material))
                }
            }
            is PostCreationIntent.UpdateCondition -> {
                _uiState.update { state ->
                    state.copy(physicalAttributes = state.physicalAttributes.copy(condition = intent.condition))
                }
            }
            is PostCreationIntent.UpdateDistinguishingFeatures -> {
                _uiState.update { state ->
                    state.copy(physicalAttributes = state.physicalAttributes.copy(distinguishingFeatures = intent.features))
                }
            }
            is PostCreationIntent.AddCapturedBitmap -> {
                processAndAnalyzeImage(intent.bitmap)
            }
            is PostCreationIntent.RemoveCapturedBitmap -> {
                _uiState.update { state ->
                    val updated = state.capturedBitmaps.toMutableList()
                    if (intent.index in updated.indices) {
                        updated.removeAt(intent.index)
                    }
                    state.copy(capturedBitmaps = updated)
                }
            }
            is PostCreationIntent.AcceptSuggestedTag -> {
                acceptAttributeSuggestion(intent.attribute)
            }
            is PostCreationIntent.DismissSuggestedTag -> {
                _uiState.update { state ->
                    state.copy(suggestedTags = state.suggestedTags.filterNot { it.id == intent.attribute.id })
                }
            }
            is PostCreationIntent.AddCustomTag -> {
                if (intent.tag.isNotBlank() && !_uiState.value.acceptedTags.contains(intent.tag)) {
                    _uiState.update { state ->
                        state.copy(acceptedTags = state.acceptedTags + intent.tag.trim())
                    }
                }
            }
            is PostCreationIntent.RemoveAcceptedTag -> {
                _uiState.update { state ->
                    state.copy(acceptedTags = state.acceptedTags.filterNot { it == intent.tag })
                }
            }
            is PostCreationIntent.SearchLocation -> {
                _uiState.update { it.copy(locationSearchQuery = intent.query) }
                searchBuildingsFuzzyDebounced(intent.query)
            }
            is PostCreationIntent.SelectBuilding -> {
                _uiState.update { state ->
                    state.copy(
                        selectedBuilding = intent.building
                    )
                }
                triggerDuplicateCheckDebounced()
            }
            is PostCreationIntent.UpdateCustomBuildingName -> {
                _uiState.update { it.copy(customBuildingName = intent.customName) }
            }
            is PostCreationIntent.SelectFloorWing -> {
                _uiState.update { it.copy(selectedFloorWing = intent.floorWing) }
            }
            is PostCreationIntent.UpdateMicroLocation -> {
                _uiState.update { it.copy(microLocationDetails = intent.details) }
            }
            is PostCreationIntent.ToggleDuplicateBanner -> {
                _uiState.update { it.copy(isDuplicateBannerExpanded = !it.isDuplicateBannerExpanded) }
            }
            is PostCreationIntent.SelectDuplicateForComparison -> {
                _uiState.update { it.copy(selectedDuplicateForComparison = intent.match) }
            }
            is PostCreationIntent.VerifyAndConnect -> {
                viewModelScope.launch {
                    _sideEffectChannel.send(
                        PostCreationSideEffect.ConnectWithFinderOrOwner(
                            contactInfo = intent.match.matchedPost.finderOrOwnerContact,
                            postId = intent.match.matchedPost.id
                        )
                    )
                }
            }
            is PostCreationIntent.SubmitPost -> {
                submitItemPost()
            }
            is PostCreationIntent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun processAndAnalyzeImage(bitmap: Bitmap) {
        viewModelScope.launch(dispatchers.default) {
            _uiState.update { state ->
                state.copy(
                    capturedBitmaps = state.capturedBitmaps + bitmap,
                    isAnalyzingImage = true
                )
            }

            val result = analyzeItemImageUseCase(bitmap)
            result.onSuccess { metadata ->
                _uiState.update { state ->
                    val pendingSuggestions = metadata.detectedAttributes.filterNot { detected ->
                        state.acceptedTags.contains(detected.value)
                    }

                    var currentAttrs = state.physicalAttributes
                    metadata.detectedAttributes.forEach { attr ->
                        when (attr.type) {
                            AttributeType.COLOR -> if (currentAttrs.dominantColor.isBlank()) currentAttrs = currentAttrs.copy(dominantColor = attr.value)
                            AttributeType.MATERIAL -> if (currentAttrs.material.isBlank()) currentAttrs = currentAttrs.copy(material = attr.value)
                            AttributeType.CONDITION -> if (currentAttrs.condition.isBlank()) currentAttrs = currentAttrs.copy(condition = attr.value)
                            AttributeType.DISTINGUISHING_FEATURE -> if (currentAttrs.distinguishingFeatures.isBlank()) currentAttrs = currentAttrs.copy(distinguishingFeatures = attr.value)
                            else -> {}
                        }
                    }

                    state.copy(
                        isAnalyzingImage = false,
                        aiMetadata = metadata,
                        suggestedTags = pendingSuggestions,
                        physicalAttributes = currentAttrs
                    )
                }
                _sideEffectChannel.send(PostCreationSideEffect.TriggerHapticFeedback)
                _sideEffectChannel.send(PostCreationSideEffect.ShowToastOrSnackbar("AI scan complete: ${metadata.structuralTags.size} tags detected"))
                triggerDuplicateCheckDebounced()
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isAnalyzingImage = false,
                        errorMessage = "Image analysis failed: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun acceptAttributeSuggestion(attribute: DetectedAttribute) {
        _uiState.update { state ->
            val updatedSuggestions = state.suggestedTags.filterNot { it.id == attribute.id }
            val updatedAccepted = if (!state.acceptedTags.contains(attribute.value)) {
                state.acceptedTags + attribute.value
            } else state.acceptedTags

            val categoryMatch = if (attribute.type == AttributeType.CATEGORY) {
                ItemCategory.entries.find { it.displayName.equals(attribute.value, ignoreCase = true) } ?: state.selectedCategory
            } else state.selectedCategory

            state.copy(
                suggestedTags = updatedSuggestions,
                acceptedTags = updatedAccepted,
                selectedCategory = categoryMatch
            )
        }
    }

    private fun searchBuildingsFuzzyDebounced(query: String) {
        locationSearchJob?.cancel()
        locationSearchJob = viewModelScope.launch(dispatchers.io) {
            delay(150)
            val results = locationRepository.searchBuildingsFuzzy(query)
            _uiState.update { it.copy(filteredBuildings = results) }
        }
    }

    private fun triggerDuplicateCheckDebounced() {
        duplicateCheckJob?.cancel()
        duplicateCheckJob = viewModelScope.launch(dispatchers.default) {
            delay(300)
            val currentState = _uiState.value
            if (currentState.title.trim().length < 3) {
                _uiState.update { it.copy(duplicateMatches = emptyList()) }
                return@launch
            }

            _uiState.update { it.copy(isCheckingDuplicates = true) }
            val result = detectDuplicateListingsUseCase(
                type = currentState.itemType,
                category = currentState.selectedCategory,
                title = currentState.title,
                description = currentState.description,
                location = currentState.selectedLocation,
                aiMetadata = currentState.aiMetadata
            )

            result.onSuccess { matches ->
                _uiState.update { state ->
                    state.copy(
                        duplicateMatches = matches,
                        isCheckingDuplicates = false,
                        isDuplicateBannerExpanded = matches.isNotEmpty()
                    )
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isCheckingDuplicates = false) }
            }
        }
    }

    private fun submitItemPost() {
        val currentState = _uiState.value
        val location = currentState.selectedLocation

        android.util.Log.i("ClaimIT_Submission", "STAGE: VALIDATION_STARTED")

        if (currentState.capturedBitmaps.isEmpty()) {
            android.util.Log.w("ClaimIT_Submission", "STAGE: SUBMISSION_FAILED reason=NO_PHOTO")
            _uiState.update { state ->
                state.copy(errorMessage = "Photo Required: Please upload at least one photograph of the item before publishing your report.")
            }
            return
        }

        if (!currentState.isFormValid || location == null) {
            android.util.Log.w("ClaimIT_Submission", "STAGE: SUBMISSION_FAILED reason=INVALID_FORM")
            _uiState.update { state ->
                state.copy(errorMessage = "Please fill in all required fields (title, location building, photo, and contact info).")
            }
            return
        }

        android.util.Log.i("ClaimIT_Submission", "STAGE: VALIDATION_SUCCESS")

        val currentUser = authRepository?.currentUserState?.value
        val fbUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
        val userId = currentUser?.id ?: fbUser?.uid ?: "user_${System.currentTimeMillis()}"
        val userDisplayName = currentUser?.displayName ?: fbUser?.displayName ?: "Campus Member"
        val orgId = currentUser?.organizationId ?: "lpu_main_campus"

        viewModelScope.launch(dispatchers.default) {
            _uiState.update { it.copy(isSubmitting = true, uploadProgressMessage = "Preparing report...") }

            val uploadedUrls = mutableListOf<String>()
            val bitmaps = currentState.capturedBitmaps
            val tempId = "post_${UUID.randomUUID()}"

            android.util.Log.i("ClaimIT_Submission", "STAGE: IMAGE_UPLOAD_STARTED count=${bitmaps.size}")

            if (bitmaps.isNotEmpty() && itemRepository is ItemRepositoryImpl) {
                bitmaps.forEachIndexed { index, bitmap ->
                    _uiState.update { it.copy(uploadProgressMessage = "Uploading photo ${index + 1} of ${bitmaps.size}...") }
                    val uploadResult = itemRepository.uploadBitmapToStorage(
                        bitmap = bitmap,
                        organizationId = orgId,
                        itemId = tempId,
                        index = index
                    )
                    uploadResult.onSuccess { url ->
                        uploadedUrls.add(url)
                    }
                }
            }

            android.util.Log.i("ClaimIT_Submission", "STAGE: IMAGE_UPLOAD_SUCCESS urls=${uploadedUrls.size}")

            if (uploadedUrls.isEmpty()) {
                android.util.Log.e("ClaimIT_Submission", "STAGE: SUBMISSION_FAILED reason=IMAGE_UPLOAD_EMPTY")
                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        uploadProgressMessage = null,
                        errorMessage = "Image upload failed. Please re-select the photograph and try again."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(uploadProgressMessage = "Publishing report to campus database...") }
            android.util.Log.i("ClaimIT_Submission", "STAGE: DATABASE_SAVE_STARTED itemId=$tempId")

            val result = createItemPostUseCase(
                title = currentState.title,
                description = currentState.description,
                type = currentState.itemType,
                category = currentState.selectedCategory,
                location = location,
                physicalAttributes = currentState.physicalAttributes,
                aiMetadata = currentState.aiMetadata,
                acceptedTags = currentState.acceptedTags,
                imageUrls = uploadedUrls,
                contactInfo = currentState.contactInfo,
                actionTakenRemarks = currentState.actionTakenRemarks,
                userId = userId,
                userDisplayName = userDisplayName,
                organizationId = orgId
            )

            result.onSuccess { postId ->
                android.util.Log.i("ClaimIT_Submission", "STAGE: DATABASE_SAVE_SUCCESS postId=$postId")
                android.util.Log.i("ClaimIT_Submission", "STAGE: REPORT_PUBLISHED postId=$postId")

                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        uploadProgressMessage = null,
                        createdPostId = postId,
                        title = "",
                        description = "",
                        capturedBitmaps = emptyList(),
                        acceptedTags = emptyList(),
                        suggestedTags = emptyList(),
                        selectedBuilding = null,
                        customBuildingName = "",
                        selectedFloorWing = "",
                        microLocationDetails = "",
                        contactInfo = "",
                        actionTakenRemarks = ""
                    )
                }
                _sideEffectChannel.send(PostCreationSideEffect.TriggerHapticFeedback)
                _sideEffectChannel.send(PostCreationSideEffect.PostCreatedSuccessfully(postId, "CLM-" + postId.take(6).uppercase()))
            }.onFailure { error ->
                android.util.Log.e("ClaimIT_Submission", "STAGE: SUBMISSION_FAILED err=${error.message}")
                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        uploadProgressMessage = null,
                        errorMessage = "Failed to submit post: ${error.localizedMessage}"
                    )
                }
            }
        }
    }
}
