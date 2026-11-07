package com.example.claimit.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.vector.ImageVector

enum class ClaimItNavDestination(
    val route: String,
    val title: String,
    val label: String,
    val icon: ImageVector
) {
    FEED("feed", "Campus Feed", "Feed", Icons.Default.Layers),
    REPORT("report", "Report Item", "Report", Icons.Default.AddCircle),
    MY_REPORTS("my_reports", "My Reports & History", "My Reports", Icons.Default.Folder),
    MATCHES("matches", "AI Vector Matches", "Matches", Icons.Default.AutoAwesome),
    CLAIMS("claims", "Claims & Recovery", "Claims", Icons.AutoMirrored.Filled.Assignment),
    ADMIN("admin", "Admin Dashboard", "Admin", Icons.Default.AdminPanelSettings),
    PROFILE("profile", "Profile & Settings", "Profile", Icons.Default.Person)
}
