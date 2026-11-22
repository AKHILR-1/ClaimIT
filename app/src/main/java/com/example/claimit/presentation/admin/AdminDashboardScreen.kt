package com.example.claimit.presentation.admin

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.claimit.core.util.TimeUtils
import com.example.claimit.domain.model.AccountStatus
import com.example.claimit.domain.model.Claim
import com.example.claimit.domain.model.DeviceSession
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.ModerationAudit
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.model.ReportStatus
import com.example.claimit.domain.model.ReportedContent
import com.example.claimit.domain.model.User
import com.example.claimit.domain.repository.AdminOverviewStats
import com.example.claimit.presentation.feed.components.ReportImageThumbnail
import com.example.claimit.ui.theme.StatusFoundEmerald
import com.example.claimit.ui.theme.StatusLostCrimson
import com.example.claimit.ui.theme.StatusPendingAmber
import com.example.claimit.ui.theme.TagAiIndigo

@Composable
fun AdminDashboardScreen(
    overviewStats: AdminOverviewStats = AdminOverviewStats(),
    allItemPosts: List<ItemPost> = emptyList(),
    reportedContentList: List<ReportedContent> = emptyList(),
    registeredUsers: List<User> = emptyList(),
    claimsList: List<Claim> = emptyList(),
    activeDeviceSessions: List<DeviceSession> = emptyList(),
    auditLogs: List<ModerationAudit> = emptyList(),
    onHideReport: (itemId: String, reason: String) -> Unit = { _, _ -> },
    onRestoreReport: (itemId: String) -> Unit = {},
    onMarkReportNonGenuine: (itemId: String, reason: String) -> Unit = { _, _ -> },
    onPermanentlyDeleteReport: (itemId: String, reason: String, callback: (Boolean, String?) -> Unit) -> Unit = { _, _, _ -> },
    onResolveReportWithDecision: (reportId: String, status: ReportStatus, decision: String, explanation: String) -> Unit = { _, _, _, _ -> },
    onSuspendStudent: (userId: String, reason: String) -> Unit = { _, _ -> },
    onReactivateStudent: (userId: String) -> Unit = {},
    onRevokeSession: (userId: String, sessionId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    // 0 = Overview, 1 = All Reports, 2 = Student Directory, 3 = Flagged Queue, 4 = Claims, 5 = Sessions, 6 = Audit Logs

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = TagAiIndigo,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "CLAIMIT SUPER ADMIN PORTAL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Realtime Database Data • Campus Moderation & Student Directory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Navigation Tabs
        val tabs = listOf(
            0 to "OVERVIEW",
            1 to "ALL REPORTS (${allItemPosts.size})",
            2 to "STUDENT DIRECTORY (${registeredUsers.size})",
            3 to "FLAGGED INBOX (${reportedContentList.count { it.status == ReportStatus.PENDING }})",
            4 to "CLAIMS (${claimsList.size})",
            5 to "SESSIONS (${activeDeviceSessions.size})",
            6 to "AUDIT LOGS (${auditLogs.size})"
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tabs) { (index, label) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) TagAiIndigo else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isSelected) TagAiIndigo else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> AdminOverviewTab(stats = overviewStats)
            1 -> AllReportsTab(
                reports = allItemPosts,
                onHideReport = { itemId, reason ->
                    onHideReport(itemId, reason)
                    Toast.makeText(context, "Report hidden from campus feed.", Toast.LENGTH_SHORT).show()
                },
                onRestoreReport = { itemId ->
                    onRestoreReport(itemId)
                    Toast.makeText(context, "Report restored to campus feed.", Toast.LENGTH_SHORT).show()
                },
                onMarkNonGenuine = { itemId, reason ->
                    onMarkReportNonGenuine(itemId, reason)
                    Toast.makeText(context, "Report marked non-genuine and removed.", Toast.LENGTH_SHORT).show()
                },
                onPermanentlyDelete = { itemId, reason ->
                    onPermanentlyDeleteReport(itemId, reason) { success, msg ->
                        Toast.makeText(context, msg ?: "Deletion completed.", Toast.LENGTH_LONG).show()
                    }
                }
            )
            2 -> StudentDirectoryTab(
                users = registeredUsers,
                allItemPosts = allItemPosts,
                onSuspendStudent = { uid, reason ->
                    onSuspendStudent(uid, reason)
                    Toast.makeText(context, "Student account suspended.", Toast.LENGTH_SHORT).show()
                },
                onReactivateStudent = { uid ->
                    onReactivateStudent(uid)
                    Toast.makeText(context, "Student account reactivated.", Toast.LENGTH_SHORT).show()
                }
            )
            3 -> FlaggedInboxTab(
                reportedContentList = reportedContentList,
                onResolveReport = onResolveReportWithDecision
            )
            4 -> ClaimsQueueTab(claims = claimsList)
            5 -> DeviceSessionsTab(
                deviceSessions = activeDeviceSessions,
                onRevokeSession = { uid, sId ->
                    onRevokeSession(uid, sId)
                    Toast.makeText(context, "Device session revoked.", Toast.LENGTH_SHORT).show()
                }
            )
            6 -> ModerationAuditHistoryTab(auditLogs = auditLogs)
        }
    }
}

@Composable
private fun AdminOverviewTab(stats: AdminOverviewStats) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "REALTIME SYSTEM METRICS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TagAiIndigo
            )
        }

        // Reports Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("TOTAL REPORTS", style = MaterialTheme.typography.labelSmall, color = TagAiIndigo, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = stats.totalReportsCount.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "LOST: ${stats.lostReportsCount} • FOUND: ${stats.foundReportsCount}", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("ACTIVE / REMOVED", style = MaterialTheme.typography.labelSmall, color = StatusFoundEmerald, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = "${stats.activeReportsCount} / ${stats.removedReportsCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Flagged Inbox: ${stats.pendingFlaggedReportsCount}", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = StatusPendingAmber)
                    }
                }
            }
        }

        // Students & Claims Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("STUDENT DIRECTORY", style = MaterialTheme.typography.labelSmall, color = TagAiIndigo, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = stats.totalUsersCount.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Verified: ${stats.verifiedUsersCount} • Suspended: ${stats.suspendedUsersCount}", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("CLAIMS METRICS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = stats.totalClaimsCount.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Pending: ${stats.pendingClaimsCount} • Approved: ${stats.approvedClaimsCount}", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TagAiIndigo.copy(alpha = 0.08f))
                    .border(1.dp, TagAiIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ACTIVE REGISTERED SESSIONS: ${stats.activeSessionsCount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TagAiIndigo
                    )
                    Text(
                        text = "Live Realtime Database synchronization from canonical organization path 'organizations/lpu_main_campus/items'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AllReportsTab(
    reports: List<ItemPost>,
    onHideReport: (itemId: String, reason: String) -> Unit,
    onRestoreReport: (itemId: String) -> Unit,
    onMarkNonGenuine: (itemId: String, reason: String) -> Unit,
    onPermanentlyDelete: (itemId: String, reason: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<ItemType?>(null) }
    var selectedPostToModerate by remember { mutableStateOf<ItemPost?>(null) }

    val filteredReports = reports.filter { post ->
        val typeMatch = selectedTypeFilter == null || post.type == selectedTypeFilter
        val searchMatch = searchQuery.isBlank() ||
                post.title.contains(searchQuery, ignoreCase = true) ||
                post.description.contains(searchQuery, ignoreCase = true) ||
                post.userDisplayName.contains(searchQuery, ignoreCase = true) ||
                post.location.building.name.contains(searchQuery, ignoreCase = true)
        typeMatch && searchMatch
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search & Filters
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search report title, building, or uploader...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "ALL", ItemType.LOST to "LOST", ItemType.FOUND to "FOUND").forEach { (type, label) ->
                val isSelected = selectedTypeFilter == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TagAiIndigo else MaterialTheme.colorScheme.surface)
                        .border(1.dp, TagAiIndigo, RoundedCornerShape(6.dp))
                        .clickable { selectedTypeFilter = type }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.surface else TagAiIndigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        if (filteredReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No reports found matching criteria.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredReports, key = { it.id }) { post ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[${post.type.name}] ${post.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (post.type == ItemType.LOST) StatusLostCrimson else StatusFoundEmerald
                                )

                                val isRemoved = post.status == PostStatus.REMOVED || post.status == PostStatus.ARCHIVED
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isRemoved) StatusLostCrimson.copy(alpha = 0.15f) else StatusFoundEmerald.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = post.status.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isRemoved) StatusLostCrimson else StatusFoundEmerald,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Text(
                                text = "Uploader: ${post.userDisplayName} • Location: ${post.location.building.name} • Posted: ${TimeUtils.formatRelativeTime(post.createdAtTimestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )

                            // Moderation Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (post.status == PostStatus.REMOVED) {
                                    Button(
                                        onClick = { onRestoreReport(post.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusFoundEmerald),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("RESTORE", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onHideReport(post.id, "Hidden by Super Admin moderation") },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("HIDE", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onMarkNonGenuine(post.id, "Marked as non-genuine/fake report") },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("NON-GENUINE", style = MaterialTheme.typography.labelSmall, color = StatusPendingAmber, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { selectedPostToModerate = post },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusLostCrimson),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PERMANENT DELETE", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Permanent Deletion Confirmation Dialog with Drive Cleanup
    selectedPostToModerate?.let { targetPost ->
        var deleteReasonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedPostToModerate = null },
            title = { Text("Confirm Permanent Deletion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Permanently deleting '${targetPost.title}' will remove the database record from 'organizations/lpu_main_campus/items' and trigger Google Drive cleanup for associated photo files (${targetPost.imageFileIds.size} files).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = deleteReasonInput,
                        onValueChange = { deleteReasonInput = it },
                        placeholder = { Text("Enter moderation reason...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPermanentlyDelete(targetPost.id, deleteReasonInput.ifBlank { "Permanently deleted by Super Admin" })
                        selectedPostToModerate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusLostCrimson)
                ) {
                    Text("PERMANENTLY DELETE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPostToModerate = null }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }
}

@Composable
private fun StudentDirectoryTab(
    users: List<User>,
    allItemPosts: List<ItemPost>,
    onSuspendStudent: (userId: String, reason: String) -> Unit,
    onReactivateStudent: (userId: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStudentForReports by remember { mutableStateOf<User?>(null) }

    val filteredUsers = users.filter { user ->
        searchQuery.isBlank() ||
                user.displayName.contains(searchQuery, ignoreCase = true) ||
                user.studentId.contains(searchQuery, ignoreCase = true)
    }.sortedBy { it.displayName }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search student name or registration number...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredUsers, key = { it.id }) { user ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val isSuspended = user.status == AccountStatus.SUSPENDED
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSuspended) StatusLostCrimson.copy(alpha = 0.15f) else StatusFoundEmerald.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isSuspended) "SUSPENDED" else "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSuspended) StatusLostCrimson else StatusFoundEmerald,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Text(
                            text = "Registration #: ${user.studentId.ifEmpty { "N/A" }} • Verified College Identity: ${if (user.isEmailVerified) "VERIFIED" else "PENDING"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { selectedStudentForReports = user },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("VIEW REPORT HISTORY", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }

                            if (user.role != com.example.claimit.domain.model.UserRole.ADMIN) {
                                if (user.status == AccountStatus.SUSPENDED) {
                                    Button(
                                        onClick = { onReactivateStudent(user.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusFoundEmerald),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("REACTIVATE", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onSuspendStudent(user.id, "Admin suspension") },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("SUSPEND", style = MaterialTheme.typography.labelSmall, color = StatusLostCrimson, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Student Report History Dialog
    selectedStudentForReports?.let { student ->
        val studentReports = allItemPosts.filter { it.userId == student.id }
        AlertDialog(
            onDismissRequest = { selectedStudentForReports = null },
            title = { Text("Report History: ${student.displayName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                if (studentReports.isEmpty()) {
                    Text("No reports posted by this student.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(240.dp)) {
                        items(studentReports) { post ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("[${post.type.name}] ${post.title}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("Status: ${post.status.name} • ${post.location.building.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStudentForReports = null }) {
                    Text("CLOSE", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }
}

@Composable
private fun FlaggedInboxTab(
    reportedContentList: List<ReportedContent>,
    onResolveReport: (reportId: String, status: ReportStatus, decision: String, explanation: String) -> Unit
) {
    if (reportedContentList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Flagged inbox is empty.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(reportedContentList, key = { it.id }) { report ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Flagged Item: ${report.itemName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Reason: ${report.reportCategory} • Reporter: ${report.reporterName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("Details: ${report.explanation}", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)

                        if (report.reviewStatus == ReportStatus.PENDING) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onResolveReport(report.id, ReportStatus.RESOLVED_REMOVED, "REMOVED", "Removed by Admin") },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusLostCrimson),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("REMOVE LISTING", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                }

                                OutlinedButton(
                                    onClick = { onResolveReport(report.id, ReportStatus.DISMISSED, "DISMISSED", "Dismissed by Admin") },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("DISMISS", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimsQueueTab(claims: List<Claim>) {
    if (claims.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No active claims recorded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(claims, key = { it.id }) { claim ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Claim: ${claim.itemTitle}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Claimant: ${claim.claimantName} • Status: ${claim.status.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("Proof: ${claim.ownershipProofDetails}", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceSessionsTab(
    deviceSessions: List<DeviceSession>,
    onRevokeSession: (userId: String, sessionId: String) -> Unit
) {
    if (deviceSessions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No active device sessions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(deviceSessions, key = { it.sessionId }) { session ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Device: ${session.deviceName} (${session.deviceId})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("User ID: ${session.userId.take(8)} • Active: ${session.isActive}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onRevokeSession(session.userId, session.sessionId) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusLostCrimson),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("REVOKE", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModerationAuditHistoryTab(auditLogs: List<ModerationAudit>) {
    if (auditLogs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No moderation audit logs recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(auditLogs, key = { it.auditId }) { audit ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Action: ${audit.action}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = StatusLostCrimson)
                            Text(TimeUtils.formatRelativeTime(audit.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Text("Item ID: ${audit.itemId} • Admin: ${audit.adminUid.take(8)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("Reason: ${audit.reason}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
