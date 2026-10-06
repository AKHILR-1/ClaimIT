package com.example.claimit.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.claimit.core.util.TimeUtils
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes
import com.example.claimit.ui.theme.StatusFoundEmerald
import com.example.claimit.ui.theme.StatusFoundEmeraldBg
import com.example.claimit.ui.theme.StatusLostCrimson
import com.example.claimit.ui.theme.StatusLostCrimsonBg

@Composable
fun CampusFeedScreen(
    onConnectContact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<ItemType?>(null) } // null = ALL

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Filter Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CAMPUS LIVE FEED",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Filter Chips (ALL, FOUND, LOST)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    null to "ALL",
                    ItemType.FOUND to "FOUND",
                    ItemType.LOST to "LOST"
                ).forEach { (type, label) ->
                    val isSelected = selectedFilter == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedFilter = type }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pre-loaded campus listings sample feed
        val sampleFeed = rememberFeedListings().filter {
            selectedFilter == null || it.type == selectedFilter
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sampleFeed) { post ->
                CampusItemFeedCard(
                    post = post,
                    onConnectContact = onConnectContact
                )
            }
        }
    }
}

@Composable
private fun CampusItemFeedCard(
    post: ItemPost,
    onConnectContact: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                val isFound = post.type == ItemType.FOUND
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isFound) StatusFoundEmeraldBg else StatusLostCrimsonBg)
                        .border(1.dp, if (isFound) StatusFoundEmerald else StatusLostCrimson, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = post.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFound) StatusFoundEmerald else StatusLostCrimson,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = post.trackingCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = post.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.location.fullFormattedAddress,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = TimeUtils.formatRelativeTime(post.createdAtTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            if (post.actionTakenRemarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Remarks: ${post.actionTakenRemarks}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onConnectContact(post.finderOrOwnerContact) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (post.type == ItemType.FOUND) "CONNECT WITH FINDER" else "CONNECT WITH OWNER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberFeedListings(): List<ItemPost> {
    val mallBuilding = Building("bld_15", "15", "Uni Mall", "Commercial Hub", "LPU UniMall Shopping & Food Court")
    val aiBuilding = Building("bld_25", "25", "School of AI and Emerging Technology", "Engineering & Tech", "AI & Emerging Tech Labs")
    val libraryBuilding = Building("bld_37", "37", "Centre Library", "Central Academic", "Central University Library")

    return remember {
        listOf(
            ItemPost(
                id = "p1",
                trackingCode = "CLM-8F32A1",
                title = "AirPods Pro 2 in White Case",
                description = "Left in white charging case with minor hinge scratch. Found near Food Court area at Uni Mall.",
                type = ItemType.FOUND,
                category = ItemCategory.ELECTRONICS,
                location = CampusLocation(mallBuilding, "Level 2 Food Plaza", "Table #12"),
                physicalAttributes = PhysicalAttributes("White", "Plastic", "Good"),
                aiMetadata = null,
                acceptedTags = listOf("AirPods Pro 2", "White Case"),
                imageUrls = emptyList(),
                createdAtTimestamp = System.currentTimeMillis() - (1 * 3600000),
                finderOrOwnerContact = "alex.finder@campus.edu",
                actionTakenRemarks = "Submitted at Block 15 Security Desk"
            ),
            ItemPost(
                id = "p2",
                trackingCode = "CLM-12B9C8",
                title = "Black Hydro Flask Water Bottle 32oz",
                description = "Matte black Hydro Flask with NASA & AI Society stickers. Left on bench outside Block 25.",
                type = ItemType.LOST,
                category = ItemCategory.OTHER,
                location = CampusLocation(aiBuilding, "Floor 2", "AI Robotics Lab Bench"),
                physicalAttributes = PhysicalAttributes("Black", "Steel", "Used"),
                aiMetadata = null,
                acceptedTags = listOf("Hydro Flask", "Stickers"),
                imageUrls = emptyList(),
                createdAtTimestamp = System.currentTimeMillis() - (4 * 3600000),
                finderOrOwnerContact = "jordan.owner@campus.edu",
                actionTakenRemarks = "Left at Block 25 Security Counter"
            ),
            ItemPost(
                id = "p3",
                trackingCode = "CLM-99D0F1",
                title = "Student ID Card - Marcus Vance",
                description = "Campus Access Card found on reading table at Centre Library Block 37.",
                type = ItemType.FOUND,
                category = ItemCategory.WALLETS_CARDS,
                location = CampusLocation(libraryBuilding, "1st Floor", "Table #4"),
                physicalAttributes = PhysicalAttributes("Blue", "PVC", "Clean"),
                aiMetadata = null,
                acceptedTags = listOf("Student ID"),
                imageUrls = emptyList(),
                createdAtTimestamp = System.currentTimeMillis() - (10 * 3600000),
                finderOrOwnerContact = "lib.desk@campus.edu",
                actionTakenRemarks = "Submitted at Central Library Circulation Desk"
            )
        )
    }
}
