package com.example.claimit.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Layers
import androidx.compose.ui.graphics.vector.ImageVector

enum class ClaimItNavDestination(
    val route: String,
    val title: String,
    val label: String,
    val icon: ImageVector
) {
    REPORT("report", "Report Item", "Report", Icons.Default.AddCircle),
    FEED("feed", "Campus Feed", "Feed", Icons.Default.Layers),
    MATCHES("matches", "AI Vector Matches", "Matches", Icons.Default.AutoAwesome),
    ZONES("zones", "Campus Zones", "Zones", Icons.Default.Business)
}
