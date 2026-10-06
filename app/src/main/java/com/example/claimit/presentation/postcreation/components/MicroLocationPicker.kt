package com.example.claimit.presentation.postcreation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.claimit.domain.model.Building

@Composable
fun MicroLocationPicker(
    locationSearchQuery: String,
    filteredBuildings: List<Building>,
    selectedBuilding: Building?,
    customBuildingName: String,
    selectedFloorWing: String,
    microLocationDetails: String,
    onSearchQueryChanged: (String) -> Unit,
    onBuildingSelected: (Building) -> Unit,
    onCustomBuildingNameChanged: (String) -> Unit,
    onFloorWingChanged: (String) -> Unit,
    onMicroLocationChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isBuildingDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "GRANULAR CAMPUS GEOSPATIAL INDEX",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Step 1: Building Selection (Fuzzy search)
        Text(
            text = "1. Campus Building / Landmark",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = if (!isBuildingDropdownExpanded && selectedBuilding != null) selectedBuilding.displayTitle else locationSearchQuery,
            onValueChange = {
                onSearchQueryChanged(it)
                isBuildingDropdownExpanded = true
            },
            placeholder = {
                Text(
                    text = "Search building code or landmark (e.g. 15 Uni Mall, 25 CSE, Hospital, Others)...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (selectedBuilding != null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Building search results drop-down list
        if (isBuildingDropdownExpanded || (selectedBuilding == null && filteredBuildings.isNotEmpty())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                    .height(160.dp)
            ) {
                LazyColumn {
                    items(filteredBuildings) { building ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onBuildingSelected(building)
                                    isBuildingDropdownExpanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = building.displayTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${building.zoneName} • ${building.landmarkName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Building Input Box if "OTHERS" selected
        if (selectedBuilding?.id == "bld_others") {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Specify Custom Building / Landmark Name",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = customBuildingName,
                onValueChange = onCustomBuildingNameChanged,
                placeholder = {
                    Text(
                        text = "Enter custom building/location (e.g. Near Solar Plant, Playground Gate 3...)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Step 2: Custom Floor / Wing Level Text Input Box
        if (selectedBuilding != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "2. Mention Floor / Wing Level",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = selectedFloorWing,
                onValueChange = onFloorWingChanged,
                placeholder = {
                    Text(
                        text = "Enter floor level (e.g. Ground Floor, 2nd Floor, 3rd Floor East Wing...)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Step 3: Specific Micro-Location Text Input Box
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "3. Micro-Location Details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = microLocationDetails,
                onValueChange = onMicroLocationChanged,
                placeholder = {
                    Text(
                        text = "e.g., Quiet Study Pod #4, Bench next to vending machine, Rm 204...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
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
