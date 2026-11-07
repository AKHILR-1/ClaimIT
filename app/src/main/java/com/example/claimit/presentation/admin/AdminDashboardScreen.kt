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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
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
import com.example.claimit.domain.model.DeviceSession
import com.example.claimit.domain.model.ModerationAudit
import com.example.claimit.domain.model.ReportStatus
import com.example.claimit.domain.model.ReportedContent
import com.example.claimit.domain.model.User
import com.example.claimit.ui.theme.StatusFoundEmerald
import com.example.claimit.ui.theme.StatusLostCrimson
import com.example.claimit.ui.theme.StatusPendingAmber
import com.example.claimit.ui.theme.TagAiIndigo

@Composable
fun AdminDashboardScreen(
    reportedContentList: List<ReportedContent>,
    auditLogs: List<ModerationAudit> = emptyList(),
    registeredUsers: List<User> = emptyList(),
    activeDeviceSessions: List<DeviceSession> = emptyList(),
    onResolveReportWithDecision: (reportId: String, status: ReportStatus, decision: String, explanation: String) -> Unit = { _, _, _, _ -> },
    onAdminRemoveListing: (itemId: String, reason: String, explanation: String) -> Unit = { _, _, _ -> },
    onSuspendUser: (userId: String) -> Unit = {},
    onUnsuspendUser: (userId: String) -> Unit = {},
    onRevokeSession: (userId: String, sessionId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Dashboard Stats, 1 = Moderation Inbox, 2 = Audit Logs, 3 = User & Sessions

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Admin Portal Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = TagAiIndigo,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "CLAIMIT ADMIN PORTAL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Central campus moderation, session security & content oversight",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Navigation Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                0 to "DASHBOARD",
                1 to "MODERATION INBOX (${reportedContentList.count { it.status == ReportStatus.PENDING }})",
                2 to "AUDIT HISTORY",
                3 to "USERS & SESSIONS"
            ).forEach { (index, label) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> AdminStatsDashboardTab(
                pendingReportsCount = reportedContentList.count { it.status == ReportStatus.PENDING },
                totalUsersCount = registeredUsers.size,
                activeSessionsCount = activeDeviceSessions.size,
                auditLogsCount = auditLogs.size
            )
            1 -> ModerationInboxTab(
                reportedContentList = reportedContentList,
                onResolveReport = onResolveReportWithDecision
            )
            2 -> ModerationAuditHistoryTab(auditLogs = auditLogs)
            3 -> UserAndSessionManagementTab(
                users = registeredUsers,
                deviceSessions = activeDeviceSessions,
                onSuspendUser = {
                    onSuspendUser(it)
                    Toast.makeText(context, "User account suspended.", Toast.LENGTH_SHORT).show()
                },
                onUnsuspendUser = {
                    onUnsuspendUser(it)
                    Toast.makeText(context, "User account reactivated.", Toast.LENGTH_SHORT).show()
                },
                onRevokeSession = { uid, sId ->
                    onRevokeSession(uid, sId)
                    Toast.makeText(context, "Device session revoked.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun AdminStatsDashboardTab(
    pendingReportsCount: Int,
    totalUsersCount: Int,
    activeSessionsCount: Int,
    auditLogsCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    .padding(14.dp)
            ) {
                Column {
                    Text("FLAGGED INBOX", style = MaterialTheme.typography.labelSmall, color = StatusPendingAmber, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = pendingReportsCount.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("REGISTERED USERS", style = MaterialTheme.typography.labelSmall, color = TagAiIndigo, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = totalUsersCount.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

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
                    .padding(14.dp)
            ) {
                Column {
                    Text("ACTIVE SESSIONS", style = MaterialTheme.typography.labelSmall, color = StatusFoundEmerald, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = activeSessionsCount.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("AUDITED ACTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = auditLogsCount.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Phase 3 — Standard CLAIMIT Listing Moderation Report View
 */
@Composable
private fun ModerationInboxTab(
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
            Text(
                text = "Moderation queue is empty. No reported content requiring action.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(reportedContentList, key = { it.id }) { report ->
                StandardizedModerationReportCard(
                    report = report,
                    onResolveReport = onResolveReport
                )
            }
        }
    }
}

@Composable
private fun StandardizedModerationReportCard(
    report: ReportedContent,
    onResolveReport: (reportId: String, status: ReportStatus, decision: String, explanation: String) -> Unit
) {
    var isReviewDialogOpen by remember { mutableStateOf(false) }
    var adminExplanationInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // CLAIMIT — LISTING MODERATION REPORT Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CLAIMIT — LISTING MODERATION REPORT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatusLostCrimson,
                    fontSize = 10.sp
                )

                Text(
                    text = "Ref: ${report.reportReference}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp
                )
            }

            Text(
                text = "Reported Listing: ${report.itemName} (ID: ${report.itemId})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Submitted By: ${report.reporterName} (UID: ${report.reporterUid.take(8)}) • Org: ${report.organizationId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Text(
                text = "Category: ${report.reportCategory} • Submission Date: ${TimeUtils.formatRelativeTime(report.createdAtTimestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Explanation: ${report.explanation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Review Status: ${report.reviewStatus.name}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (report.reviewStatus == ReportStatus.PENDING) StatusPendingAmber else StatusFoundEmerald,
                    fontSize = 10.sp
                )
            }

            if (report.reviewStatus == ReportStatus.PENDING || report.reviewStatus == ReportStatus.UNDER_REVIEW) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isReviewDialogOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusLostCrimson),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("REMOVE LISTING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onResolveReport(report.id, ReportStatus.DISMISSED, "DISMISSED", "Report dismissed after investigation")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DISMISS REPORT", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    }
                }
            }
        }
    }

    if (isReviewDialogOpen) {
        AlertDialog(
            onDismissRequest = { isReviewDialogOpen = false },
            title = { Text("Confirm Moderation Action", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide administrator explanation for removing '${report.itemName}':", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = adminExplanationInput,
                        onValueChange = { adminExplanationInput = it },
                        placeholder = { Text("Admin explanation / reason...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResolveReport(report.id, ReportStatus.RESOLVED_REMOVED, "REMOVED", adminExplanationInput)
                        isReviewDialogOpen = false
                    },
                    enabled = adminExplanationInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusLostCrimson)
                ) {
                    Text("CONFIRM REMOVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isReviewDialogOpen = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
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
            Text(
                text = "No moderation audit logs recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                            Text(
                                text = "Action: ${audit.action}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusLostCrimson
                            )
                            Text(
                                text = TimeUtils.formatRelativeTime(audit.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            text = "Item ID: ${audit.itemId} • Admin UID: ${audit.adminUid.take(8)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "Reason: ${audit.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAndSessionManagementTab(
    users: List<User>,
    deviceSessions: List<DeviceSession>,
    onSuspendUser: (String) -> Unit,
    onUnsuspendUser: (String) -> Unit,
    onRevokeSession: (userId: String, sessionId: String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "REGISTERED USERS (${users.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TagAiIndigo
            )
        }

        items(users, key = { it.id }) { user ->
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
                            text = "${user.displayName} (${user.role.name})",
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
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = "College Email: ${user.collegeEmail} • Student ID: ${user.studentId.ifEmpty { "N/A" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    if (user.role != com.example.claimit.domain.model.UserRole.ADMIN) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (user.status == AccountStatus.SUSPENDED) {
                                Button(
                                    onClick = { onUnsuspendUser(user.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusFoundEmerald),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("REACTIVATE USER", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onSuspendUser(user.id) },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("SUSPEND ACCOUNT", style = MaterialTheme.typography.labelSmall, color = StatusLostCrimson, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ACTIVE DEVICE SESSIONS (${deviceSessions.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TagAiIndigo
            )
        }

        if (deviceSessions.isEmpty()) {
            item {
                Text("No active sessions recorded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
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
                            Text(
                                text = "Device: ${session.deviceName} (ID: ${session.deviceId})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "User UID: ${session.userId.take(8)} • Active: ${session.isActive}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
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
