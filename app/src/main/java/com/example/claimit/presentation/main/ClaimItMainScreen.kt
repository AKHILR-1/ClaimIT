package com.example.claimit.presentation.main

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.claimit.presentation.feed.CampusFeedScreen
import com.example.claimit.presentation.matches.VectorMatchesScreen
import com.example.claimit.presentation.navigation.ClaimItNavDestination
import com.example.claimit.presentation.postcreation.PostCreationScreen
import com.example.claimit.presentation.postcreation.PostCreationViewModel
import com.example.claimit.presentation.postcreation.components.ClaimItHeader
import com.example.claimit.presentation.zones.CampusZonesScreen
import com.example.claimit.ui.theme.StatusPendingAmber
import com.example.claimit.ui.theme.TagAiIndigo

@Composable
fun ClaimItMainScreen(
    viewModel: PostCreationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(ClaimItNavDestination.REPORT) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            ClaimItHeader()
        },
        bottomBar = {
            ClaimItBottomMenu(
                currentDestination = currentDestination,
                matchCount = uiState.duplicateMatches.size,
                onDestinationSelected = { destination ->
                    currentDestination = destination
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "pageTransition"
            ) { destination ->
                when (destination) {
                    ClaimItNavDestination.REPORT -> {
                        PostCreationScreen(viewModel = viewModel)
                    }
                    ClaimItNavDestination.FEED -> {
                        CampusFeedScreen(
                            onConnectContact = { contact ->
                                Toast.makeText(context, "Contacting: $contact", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                    ClaimItNavDestination.MATCHES -> {
                        VectorMatchesScreen(viewModel = viewModel)
                    }
                    ClaimItNavDestination.ZONES -> {
                        CampusZonesScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimItBottomMenu(
    currentDestination: ClaimItNavDestination,
    matchCount: Int,
    onDestinationSelected: (ClaimItNavDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        ClaimItNavDestination.entries.forEach { destination ->
            val isSelected = currentDestination == destination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    if (destination == ClaimItNavDestination.MATCHES && matchCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = StatusPendingAmber,
                                    contentColor = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = matchCount.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title,
                                tint = if (isSelected) TagAiIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.title,
                            tint = if (isSelected) TagAiIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TagAiIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = TagAiIndigo.copy(alpha = 0.15f)
                )
            )
        }
    }
}
