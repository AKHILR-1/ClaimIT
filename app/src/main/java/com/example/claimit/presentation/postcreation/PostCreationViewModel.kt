package com.example.claimit.presentation.postcreation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.AttributeType
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.DetectedAttribute
import com.example.claimit.domain.model.DuplicateMatchResult
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.repository.CampusLocationRepository
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

class PostCreationViewModel(
    private val analyzeItemImageUseCase: AnalyzeItemImageUseCase,
    private val detectDuplicateListingsUseCase: DetectDuplicateListingsUseCase,
    private val createItemPostUseCase: CreateItemPostUseCase,
    private val locationRepository: CampusLocationRepository,
    private val dispatchers: CoroutineDispatchers
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

                    // Auto-fill physical attributes if currently empty
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

            // Also accept into category or physical attributes if appropriate
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
            delay(150) // Fast search debounce
            val results = locationRepository.searchBuildingsFuzzy(query)
            _uiState.update { it.copy(filteredBuildings = results) }
        }
    }

    private fun triggerDuplicateCheckDebounced() {
        duplicateCheckJob?.cancel()
        duplicateCheckJob = viewModelScope.launch(dispatchers.default) {
            delay(300) // 300ms debounce
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

        if (!currentState.isFormValid || location == null) {
            _uiState.update { state ->
                state.copy(errorMessage = "Please fill in all required fields (title, location building, and contact info).")
            }
            return
        }

        viewModelScope.launch(dispatchers.default) {
            _uiState.update { it.copy(isSubmitting = true) }

            val result = createItemPostUseCase(
                title = currentState.title,
                description = currentState.description,
                type = currentState.itemType,
                category = currentState.selectedCategory,
                location = location,
                physicalAttributes = currentState.physicalAttributes,
                aiMetadata = currentState.aiMetadata,
                acceptedTags = currentState.acceptedTags,
                imageUrls = emptyList(), // Local bitmap pipeline simulated
                contactInfo = currentState.contactInfo,
                actionTakenRemarks = currentState.actionTakenRemarks
            )

            result.onSuccess { postId ->
                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        createdPostId = postId
                    )
                }
                _sideEffectChannel.send(PostCreationSideEffect.TriggerHapticFeedback)
                _sideEffectChannel.send(PostCreationSideEffect.PostCreatedSuccessfully(postId, "CLM-" + postId.take(6).uppercase()))
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to submit post: ${error.localizedMessage}"
                    )
                }
            }
        }
    }
}
