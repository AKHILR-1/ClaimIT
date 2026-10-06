package com.example.claimit.presentation.postcreation

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.presentation.postcreation.components.DuplicateMatchBanner
import com.example.claimit.presentation.postcreation.components.ItemTypeSelector
import com.example.claimit.presentation.postcreation.components.MediaDrawerComponent
import com.example.claimit.presentation.postcreation.components.MicroLocationPicker
import com.example.claimit.presentation.postcreation.components.SplitCardComparisonSheet
import com.example.claimit.presentation.postcreation.components.SuggestedTagsTray
import com.example.claimit.ui.theme.TagAiIndigo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostCreationScreen(
    viewModel: PostCreationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var currentStep by remember { mutableIntStateOf(1) } // Step 1, 2, or 3

    // Collect Side Effects
    LaunchedEffect(key1 = viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is PostCreationSideEffect.ShowToastOrSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is PostCreationSideEffect.TriggerHapticFeedback -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is PostCreationSideEffect.PostCreatedSuccessfully -> {
                    Toast.makeText(
                        context,
                        "Report Published Successfully!\nTracking Code: ${effect.trackingCode}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is PostCreationSideEffect.ConnectWithFinderOrOwner -> {
                    Toast.makeText(
                        context,
                        "Connecting with contact: ${effect.contactInfo}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Step Progress Indicator Bar
            StepProgressHeader(
                currentStep = currentStep,
                onStepClicked = { step -> currentStep = step }
            )

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "stepAnimation"
            ) { step ->
                when (step) {
                    1 -> Step1Overview(uiState = uiState, viewModel = viewModel)
                    2 -> Step2CategoryAndTags(uiState = uiState, viewModel = viewModel)
                    3 -> Step3LocationAndContact(uiState = uiState, viewModel = viewModel)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Step Navigation Footer Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PREVIOUS",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                if (currentStep < 3) {
                    Button(
                        onClick = { currentStep++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NEXT STEP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.onIntent(PostCreationIntent.SubmitPost) },
                        enabled = uiState.isFormValid && !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TagAiIndigo,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Publish,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SUBMIT REPORT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Split-card comparison bottom sheet modal
        uiState.selectedDuplicateForComparison?.let { selectedMatch ->
            SplitCardComparisonSheet(
                matchResult = selectedMatch,
                currentFormState = uiState,
                onDismiss = { viewModel.onIntent(PostCreationIntent.SelectDuplicateForComparison(null)) },
                onVerifyAndConnect = { match ->
                    viewModel.onIntent(PostCreationIntent.VerifyAndConnect(match))
                    viewModel.onIntent(PostCreationIntent.SelectDuplicateForComparison(null))
                }
            )
        }
    }
}

@Composable
private fun StepProgressHeader(
    currentStep: Int,
    onStepClicked: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            1 to "1. OVERVIEW",
            2 to "2. TAGS",
            3 to "3. LOCATION"
        ).forEach { (step, label) ->
            val isSelected = currentStep == step
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onStepClicked(step) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Step1Overview(
    uiState: PostCreationUiState,
    viewModel: PostCreationViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ItemTypeSelector(
            selectedType = uiState.itemType,
            onTypeSelected = { viewModel.onIntent(PostCreationIntent.SelectItemType(it)) }
        )

        MediaDrawerComponent(
            capturedBitmaps = uiState.capturedBitmaps,
            isAnalyzing = uiState.isAnalyzingImage,
            onBitmapAdded = { viewModel.onIntent(PostCreationIntent.AddCapturedBitmap(it)) },
            onBitmapRemoved = { viewModel.onIntent(PostCreationIntent.RemoveCapturedBitmap(it)) }
        )

        DuplicateMatchBanner(
            duplicateMatches = uiState.duplicateMatches,
            isExpanded = uiState.isDuplicateBannerExpanded,
            onToggleExpanded = { viewModel.onIntent(PostCreationIntent.ToggleDuplicateBanner) },
            onSelectMatchForComparison = { viewModel.onIntent(PostCreationIntent.SelectDuplicateForComparison(it)) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Title,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TITLE & DESCRIPTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onIntent(PostCreationIntent.UpdateTitle(it)) },
                placeholder = { Text("Title (e.g. AirPods Pro 2 in White Charging Case)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onIntent(PostCreationIntent.UpdateDescription(it)) },
                placeholder = { Text("Describe specific markings, condition, where found or lost...") },
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step2CategoryAndTags(
    uiState: PostCreationUiState,
    viewModel: PostCreationViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SuggestedTagsTray(
            suggestedTags = uiState.suggestedTags,
            acceptedTags = uiState.acceptedTags,
            onTagAccepted = { viewModel.onIntent(PostCreationIntent.AcceptSuggestedTag(it)) },
            onTagDismissed = { viewModel.onIntent(PostCreationIntent.DismissSuggestedTag(it)) },
            onRemoveAcceptedTag = { viewModel.onIntent(PostCreationIntent.RemoveAcceptedTag(it)) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ITEM CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ItemCategory.entries.forEach { category ->
                    val isSelected = uiState.selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.onIntent(PostCreationIntent.SelectCategory(category)) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.selectedCategory == ItemCategory.OTHER) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Specify Custom Item Category / Tag",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = uiState.customCategoryTag,
                    onValueChange = { viewModel.onIntent(PostCreationIntent.UpdateCustomCategoryTag(it)) },
                    placeholder = { Text("Enter custom item tag (e.g. Helmet, Badminton Racket, Umbrella, Calculator...)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PHYSICAL ATTRIBUTES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.physicalAttributes.dominantColor,
                    onValueChange = { viewModel.onIntent(PostCreationIntent.UpdateDominantColor(it)) },
                    label = { Text("Color") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = uiState.physicalAttributes.material,
                    onValueChange = { viewModel.onIntent(PostCreationIntent.UpdateMaterial(it)) },
                    label = { Text("Material") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.physicalAttributes.distinguishingFeatures,
                onValueChange = { viewModel.onIntent(PostCreationIntent.UpdateDistinguishingFeatures(it)) },
                label = { Text("Distinguishing Features (Scratches, Stickers...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Step3LocationAndContact(
    uiState: PostCreationUiState,
    viewModel: PostCreationViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MicroLocationPicker(
            locationSearchQuery = uiState.locationSearchQuery,
            filteredBuildings = uiState.filteredBuildings,
            selectedBuilding = uiState.selectedBuilding,
            customBuildingName = uiState.customBuildingName,
            selectedFloorWing = uiState.selectedFloorWing,
            microLocationDetails = uiState.microLocationDetails,
            onSearchQueryChanged = { viewModel.onIntent(PostCreationIntent.SearchLocation(it)) },
            onBuildingSelected = { viewModel.onIntent(PostCreationIntent.SelectBuilding(it)) },
            onCustomBuildingNameChanged = { viewModel.onIntent(PostCreationIntent.UpdateCustomBuildingName(it)) },
            onFloorWingChanged = { viewModel.onIntent(PostCreationIntent.SelectFloorWing(it)) },
            onMicroLocationChanged = { viewModel.onIntent(PostCreationIntent.UpdateMicroLocation(it)) }
        )

        // Remarks / Action Taken Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContactPage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REMARKS / ACTION TAKEN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.actionTakenRemarks,
                onValueChange = { viewModel.onIntent(PostCreationIntent.UpdateActionTakenRemarks(it)) },
                placeholder = { Text("e.g. Submitted in Block 31 Security Office, AO Office, Main Gate Desk...") },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Contact Info Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContactPage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CONTACT INFO FOR VERIFICATION & RECOVERY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.contactInfo,
                onValueChange = { viewModel.onIntent(PostCreationIntent.UpdateContactInfo(it)) },
                placeholder = { Text("Campus email or phone number...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
