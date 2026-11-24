package com.ssnexora.claimit.presentation.postcreation

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Title
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssnexora.claimit.domain.model.ItemCategory
import com.ssnexora.claimit.domain.model.ItemType
import com.ssnexora.claimit.presentation.postcreation.components.DuplicateMatchBanner
import com.ssnexora.claimit.presentation.postcreation.components.ItemTypeSelector
import com.ssnexora.claimit.presentation.postcreation.components.MediaDrawerComponent
import com.ssnexora.claimit.presentation.postcreation.components.MicroLocationPicker
import com.ssnexora.claimit.presentation.postcreation.components.SplitCardComparisonSheet
import com.ssnexora.claimit.presentation.postcreation.components.SuggestedTagsTray
import com.ssnexora.claimit.ui.theme.StatusFoundEmerald
import com.ssnexora.claimit.ui.theme.StatusLostCrimson
import com.ssnexora.claimit.ui.theme.TagAiIndigo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostCreationScreen(
    viewModel: PostCreationViewModel,
    modifier: Modifier = Modifier,
    onNavigateToFeed: () -> Unit = {},
    onNavigateToMyReports: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var currentStep by remember { mutableIntStateOf(1) } // Step 1, 2, or 3

    BackHandler(enabled = currentStep > 1) {
        currentStep--
    }

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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

    if (uiState.submissionStatus == ReportSubmissionStatus.SUCCESS) {
        PostCreationSuccessPage(
            uiState = uiState,
            onNavigateToFeed = onNavigateToFeed,
            onNavigateToMyReports = onNavigateToMyReports,
            onResetForm = {
                viewModel.onIntent(PostCreationIntent.ResetForm)
                currentStep = 1
            }
        )
    } else {
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

            if (!uiState.errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!uiState.uploadProgressMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TagAiIndigo.copy(alpha = 0.12f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = TagAiIndigo
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = uiState.uploadProgressMessage ?: "Processing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TagAiIndigo,
                        fontWeight = FontWeight.SemiBold
                    )
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

            Spacer(modifier = Modifier.height(64.dp))
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
                placeholder = { Text("Item title (e.g. White AirPods Pro)") },
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
                placeholder = { Text("Describe condition or specific markings") },
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
                    placeholder = { Text("Specify custom item tag") },
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
                label = { Text("Distinguishing features") },
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
                placeholder = { Text("e.g. Submitted at Block 31 Security Office") },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Contact Phone Number Box (Mandatory with Country Code)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = if (uiState.isContactPhoneValid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONTACT PHONE NUMBER (MANDATORY)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.isContactPhoneValid) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Valid Phone",
                        tint = com.ssnexora.claimit.ui.theme.StatusFoundEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            var isCountryPickerOpen by remember { mutableStateOf(false) }
            var countrySearchQuery by remember { mutableStateOf("") }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .clickable { isCountryPickerOpen = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = uiState.countryCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Country Code",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.contactInfo,
                    onValueChange = { input ->
                        val cleanDigits = input.filter { it.isDigit() || it == ' ' }
                        viewModel.onIntent(PostCreationIntent.UpdateContactInfo(cleanDigits))
                    },
                    placeholder = { Text("10-digit phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (uiState.isContactPhoneValid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (isCountryPickerOpen) {
                AlertDialog(
                    onDismissRequest = {
                        isCountryPickerOpen = false
                        countrySearchQuery = ""
                    },
                    title = {
                        Text(
                            text = "Select Country Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = countrySearchQuery,
                                onValueChange = { countrySearchQuery = it },
                                placeholder = { Text("Search country or code...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val filteredCountries = remember(countrySearchQuery) {
                                if (countrySearchQuery.isBlank()) {
                                    com.ssnexora.claimit.core.util.CountryCodeData.allCountryCodes
                                } else {
                                    com.ssnexora.claimit.core.util.CountryCodeData.allCountryCodes.filter {
                                        it.countryName.contains(countrySearchQuery, ignoreCase = true) ||
                                        it.code.contains(countrySearchQuery)
                                    }
                                }
                            }

                            Box(modifier = Modifier.height(260.dp)) {
                                LazyColumn {
                                    items(filteredCountries) { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.onIntent(PostCreationIntent.SelectCountryCode(item.formattedCode))
                                                    isCountryPickerOpen = false
                                                    countrySearchQuery = ""
                                                }
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.countryName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = item.formattedCode,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            isCountryPickerOpen = false
                            countrySearchQuery = ""
                        }) {
                            Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (uiState.isContactPhoneValid) "✓ Valid 10-digit mobile number" else "Must enter a valid 10-digit mobile number with country code",
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.isContactPhoneValid) StatusFoundEmerald else MaterialTheme.colorScheme.error,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun PostCreationSuccessPage(
    uiState: PostCreationUiState,
    onNavigateToFeed: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onResetForm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(StatusFoundEmerald.copy(alpha = 0.15f))
                .border(2.dp, StatusFoundEmerald, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = StatusFoundEmerald,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Successfully Submitted!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "For more updates check My Reports and active notifications.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card with summary details
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.itemType.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.itemType == ItemType.LOST) StatusLostCrimson else StatusFoundEmerald
                    )

                    Text(
                        text = "Status: Live",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusFoundEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = uiState.title.ifBlank { "Reported Item" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Location: ${uiState.selectedLocation?.building?.name ?: "Campus Location"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                if (!uiState.createdPostId.isNullOrBlank()) {
                    Text(
                        text = "Tracking ID: ${uiState.createdPostId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TagAiIndigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNavigateToMyReports,
                colors = ButtonDefaults.buttonColors(containerColor = TagAiIndigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "CHECK MY REPORTS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onNavigateToFeed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "VIEW CAMPUS LIVE FEED",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onResetForm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Submit Another Report",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

