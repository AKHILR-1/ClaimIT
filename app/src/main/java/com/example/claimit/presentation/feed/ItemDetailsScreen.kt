package com.example.claimit.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.claimit.core.util.TimeUtils
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.model.ReportReason
import com.example.claimit.domain.model.UserRole
import com.example.claimit.ui.theme.StatusFoundEmerald
import com.example.claimit.ui.theme.StatusLostCrimson
import com.example.claimit.ui.theme.TagAiIndigo

@Composable
fun ItemDetailsScreen(
    post: ItemPost,
    currentUserId: String,
    currentUserRole: UserRole,
    onBackClicked: () -> Unit,
    onSubmitClaim: (itemId: String, itemTitle: String, verificationAnswer: String, proofDetails: String) -> Unit,
    onReportToAdmin: (itemId: String, title: String, reason: ReportReason, details: String) -> Unit,
    onConnectContact: (contactInfo: String) -> Unit,
    modifier: Modifier = Modifier,
    onWithdrawReport: (itemId: String) -> Unit = {},
    onAdminRemoveListing: (itemId: String, reason: String, explanation: String) -> Unit = { _, _, _ -> }
) {
    val isOwner = post.userId == currentUserId
    val isAdmin = currentUserRole == UserRole.ADMIN

    var isClaimFormOpen by remember { mutableStateOf(false) }
    var isReportFormOpen by remember { mutableStateOf(false) }
    var isAdminRemoveDialogOpen by remember { mutableStateOf(false) }

    var verificationAnswer by remember { mutableStateOf("") }
    var proofDetails by remember { mutableStateOf("") }

    var reportReason by remember { mutableStateOf(ReportReason.INAPPROPRIATE_CONTENT) }
    var reportDetails by remember { mutableStateOf("") }

    var removeReasonSelect by remember { mutableStateOf("Policy Violation") }
    var removeExplanationText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ITEM DETAILS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // SECTION A — ITEM PHOTOGRAPH
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TagAiIndigo.copy(alpha = 0.1f))
                    .border(1.dp, TagAiIndigo.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = TagAiIndigo,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (post.imageUrls.isNotEmpty()) "Image attached" else "No photograph uploaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = TagAiIndigo
                    )
                }
            }
        }

        // SECTION B — ITEM INFORMATION
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isFound = post.type == ItemType.FOUND
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isFound) StatusFoundEmerald.copy(alpha = 0.15f) else StatusLostCrimson.copy(alpha = 0.15f))
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
                            text = post.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = post.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Status: ${post.status.name}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // SECTION C — LOCATION INFORMATION
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LOCATION INFORMATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = post.location.fullFormattedAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (post.location.microLocationDetails.isNotBlank()) {
                        Text(
                            text = "Micro-location: ${post.location.microLocationDetails}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // SECTION D — REPORT INFORMATION
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "REPORT INFORMATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Report Reference ID: ${post.trackingCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Reported By: ${post.userDisplayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Organization: ${post.organizationId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Published: ${TimeUtils.formatRelativeTime(post.createdAtTimestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // SECTION E — ACTIONS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isClaimFormOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("SUBMIT OWNERSHIP CLAIM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TagAiIndigo)

                        OutlinedTextField(
                            value = verificationAnswer,
                            onValueChange = { verificationAnswer = it },
                            label = { Text("Answer to verification question") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = proofDetails,
                            onValueChange = { proofDetails = it },
                            label = { Text("Ownership proof details (receipt, serial #, distinctive marks)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onSubmitClaim(post.id, post.title, verificationAnswer, proofDetails)
                                    isClaimFormOpen = false
                                },
                                enabled = verificationAnswer.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = TagAiIndigo),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SUBMIT CLAIM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { isClaimFormOpen = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else if (isReportFormOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("REPORT LISTING TO CAMPUS ADMIN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)

                        OutlinedTextField(
                            value = reportDetails,
                            onValueChange = { reportDetails = it },
                            label = { Text("Describe reason for reporting") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onReportToAdmin(post.id, post.title, reportReason, reportDetails)
                                    isReportFormOpen = false
                                },
                                enabled = reportDetails.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusLostCrimson),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SUBMIT REPORT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { isReportFormOpen = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isOwner && post.type == ItemType.FOUND && post.status == PostStatus.ACTIVE) {
                            Button(
                                onClick = { isClaimFormOpen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TagAiIndigo),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CLAIM ITEM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Button(
                            onClick = { onConnectContact(post.finderOrOwnerContact) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CONTACT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!isOwner) {
                            OutlinedButton(
                                onClick = { isReportFormOpen = true }
                            ) {
                                Icon(Icons.Default.Report, contentDescription = "Report to Admin", tint = StatusLostCrimson, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (isOwner) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { onWithdrawReport(post.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusLostCrimson),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WITHDRAW REPORT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { isAdminRemoveDialogOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusLostCrimson),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ADMIN: REMOVE LISTING GLOBALLY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Admin Remove Listing Dialog
    if (isAdminRemoveDialogOpen) {
        AlertDialog(
            onDismissRequest = { isAdminRemoveDialogOpen = false },
            title = { Text("Remove Prohibited Listing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Item: ${post.title} (ID: ${post.id})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = removeExplanationText,
                        onValueChange = { removeExplanationText = it },
                        label = { Text("Reason for removal (required)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdminRemoveListing(post.id, removeReasonSelect, removeExplanationText)
                        isAdminRemoveDialogOpen = false
                        onBackClicked()
                    },
                    enabled = removeExplanationText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusLostCrimson)
                ) {
                    Text("CONFIRM REMOVAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAdminRemoveDialogOpen = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
    }
}
