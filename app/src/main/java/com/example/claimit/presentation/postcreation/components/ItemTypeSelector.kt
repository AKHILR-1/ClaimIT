package com.example.claimit.presentation.postcreation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WhereToVote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.claimit.domain.model.ItemType
import com.example.claimit.ui.theme.StatusFoundEmerald
import com.example.claimit.ui.theme.StatusFoundEmeraldBg
import com.example.claimit.ui.theme.StatusLostCrimson
import com.example.claimit.ui.theme.StatusLostCrimsonBg

@Composable
fun ItemTypeSelector(
    selectedType: ItemType,
    onTypeSelected: (ItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // FOUND option
            val isFoundSelected = selectedType == ItemType.FOUND
            val foundBgColor by animateColorAsState(
                targetValue = if (isFoundSelected) StatusFoundEmeraldBg else MaterialTheme.colorScheme.surface,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "foundBg"
            )
            val foundBorderColor by animateColorAsState(
                targetValue = if (isFoundSelected) StatusFoundEmerald else MaterialTheme.colorScheme.outline,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "foundBorder"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(foundBgColor)
                    .border(1.dp, foundBorderColor, RoundedCornerShape(6.dp))
                    .clickable { onTypeSelected(ItemType.FOUND) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WhereToVote,
                        contentDescription = "Found Item",
                        tint = if (isFoundSelected) StatusFoundEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I FOUND AN ITEM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isFoundSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isFoundSelected) StatusFoundEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // LOST option
            val isLostSelected = selectedType == ItemType.LOST
            val lostBgColor by animateColorAsState(
                targetValue = if (isLostSelected) StatusLostCrimsonBg else MaterialTheme.colorScheme.surface,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "lostBg"
            )
            val lostBorderColor by animateColorAsState(
                targetValue = if (isLostSelected) StatusLostCrimson else MaterialTheme.colorScheme.outline,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "lostBorder"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(lostBgColor)
                    .border(1.dp, lostBorderColor, RoundedCornerShape(6.dp))
                    .clickable { onTypeSelected(ItemType.LOST) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Lost Item",
                        tint = if (isLostSelected) StatusLostCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I LOST AN ITEM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isLostSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLostSelected) StatusLostCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
