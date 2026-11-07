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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.claimit.domain.model.UserRole
import com.example.claimit.presentation.admin.AdminDashboardScreen
import com.example.claimit.presentation.auth.AuthScreen
import com.example.claimit.presentation.auth.EmailVerificationScreen
import com.example.claimit.presentation.claims.ClaimsScreen
import com.example.claimit.presentation.feed.CampusFeedScreen
import com.example.claimit.presentation.matches.VectorMatchesScreen
import com.example.claimit.presentation.navigation.ClaimItNavDestination
import com.example.claimit.presentation.notifications.NotificationsSheet
import com.example.claimit.presentation.postcreation.PostCreationScreen
import com.example.claimit.presentation.postcreation.PostCreationViewModel
import com.example.claimit.presentation.postcreation.components.ClaimItHeader
import com.example.claimit.presentation.profile.ProfileScreen
import com.example.claimit.ui.theme.StatusPendingAmber
import com.example.claimit.ui.theme.TagAiIndigo

@Composable
fun ClaimItMainScreen(
    postCreationViewModel: PostCreationViewModel,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(ClaimItNavDestination.FEED) }
    var isNotificationsSheetOpen by remember { mutableStateOf(false) }

    val userState by mainViewModel.currentUserState.collectAsState()
    val isAuthLoading by mainViewModel.isAuthLoading.collectAsState()
    val authError by mainViewModel.authError.collectAsState()

    val userNotifications by mainViewModel.userNotifications.collectAsState()
    val unreadCount by mainViewModel.unreadNotificationCount.collectAsState()

    val postUiState by postCreationViewModel.uiState.collectAsState()
    val mySubmittedClaims by mainViewModel.mySubmittedClaims.collectAsState()
    val incomingClaimsToReview by mainViewModel.incomingClaimsToReview.collectAsState()
    val reportedQueue by mainViewModel.reportedQueue.collectAsState()
    val savedPosts by mainViewModel.savedPosts.collectAsState()

    val currentUser = userState

    if (currentUser == null) {
        AuthScreen(
            onLoginSubmitted = { email, pass -> mainViewModel.login(email, pass) },
            onRegisterSubmitted = { email, pass, name, collegeEmail, studentId, role ->
                mainViewModel.register(email, pass, name, collegeEmail, studentId, role)
            },
            onResetPasswordClicked = { email ->
                mainViewModel.resetPassword(email) { success, msg ->
                    if (success) {
                        Toast.makeText(context, "Password reset instructions sent to $email", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, msg ?: "Password reset failed.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            isLoading = isAuthLoading,
            errorMessage = authError,
            modifier = modifier
        )
    } else if (!currentUser.isEmailVerified) {
        EmailVerificationScreen(
            userEmail = currentUser.email,
            onResendEmailClicked = { callback ->
                mainViewModel.resendVerificationEmail { success, err ->
                    callback(success, err)
                }
            },
            onCheckStatusClicked = { callback ->
                mainViewModel.checkVerificationStatus { isVerified ->
                    callback(isVerified)
                }
            },
            onLogoutClicked = { mainViewModel.logout() },
            modifier = modifier
        )
    } else {
        Scaffold(
            topBar = {
                ClaimItHeader(
                    unreadCount = unreadCount,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onNotificationClick = { isNotificationsSheetOpen = true }
                )
            },
            bottomBar = {
                ClaimItBottomMenu(
                    currentDestination = currentDestination,
                    matchCount = postUiState.duplicateMatches.size,
                    userRole = currentUser.role,
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
                        ClaimItNavDestination.FEED -> {
                            CampusFeedScreen(
                                itemRepository = mainViewModel.itemRepository,
                                organizationId = currentUser.organizationId,
                                onConnectContact = { contact ->
                                    Toast.makeText(context, "Contacting: $contact", Toast.LENGTH_LONG).show()
                                },
                                onSubmitClaim = { itemId, title, answer, proof ->
                                    mainViewModel.submitClaim(itemId, title, answer, proof)
                                    Toast.makeText(context, "Ownership claim submitted successfully!", Toast.LENGTH_LONG).show()
                                },
                                onReportItem = { itemId, title, reason, details ->
                                    mainViewModel.reportItem(itemId, title, reason, details)
                                    Toast.makeText(context, "Report sent to campus administration.", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                        ClaimItNavDestination.REPORT -> {
                            PostCreationScreen(viewModel = postCreationViewModel)
                        }
                        ClaimItNavDestination.MY_REPORTS -> {
                            com.example.claimit.presentation.reports.MyReportsScreen(
                                currentUserId = currentUser.id,
                                itemRepository = mainViewModel.itemRepository,
                                onWithdrawReport = { postId ->
                                    mainViewModel.withdrawReport(postId)
                                },
                                onEditReport = { post ->
                                    mainViewModel.editReport(post)
                                }
                            )
                        }
                        ClaimItNavDestination.MATCHES -> {
                            VectorMatchesScreen(viewModel = postCreationViewModel)
                        }
                        ClaimItNavDestination.CLAIMS -> {
                            ClaimsScreen(
                                mySubmittedClaims = mySubmittedClaims,
                                claimsToReview = incomingClaimsToReview,
                                onUpdateClaimStatus = { claimId, status, notes ->
                                    mainViewModel.updateClaimStatus(claimId, status, notes)
                                    Toast.makeText(context, "Claim status updated to ${status.name}", Toast.LENGTH_SHORT).show()
                                },
                                onConfirmHandover = { claimId, isUploader ->
                                    mainViewModel.confirmItemHandover(claimId, isUploader) { success, msg ->
                                        Toast.makeText(context, msg ?: "Handover updated.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                        ClaimItNavDestination.ADMIN -> {
                            AdminDashboardScreen(
                                reportedContentList = reportedQueue,
                                onResolveReport = { reportId, status ->
                                    mainViewModel.resolveReport(reportId, status)
                                    Toast.makeText(context, "Report resolved: ${status.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        ClaimItNavDestination.PROFILE -> {
                            ProfileScreen(
                                currentUser = currentUser,
                                savedPosts = savedPosts,
                                onLogoutClicked = { mainViewModel.logout() },
                                onDeleteAccountClicked = {
                                    mainViewModel.deleteAccount()
                                    Toast.makeText(context, "Account deletion request processed.", Toast.LENGTH_LONG).show()
                                },
                                onUpdateContactEmailSubmitted = { newEmail, pass ->
                                    mainViewModel.updateContactEmail(newEmail, pass) { success, msg ->
                                        Toast.makeText(context, msg ?: "Update failed.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (isNotificationsSheetOpen) {
                NotificationsSheet(
                    notifications = userNotifications,
                    onDismiss = { isNotificationsSheetOpen = false },
                    onMarkAsRead = { notifId -> mainViewModel.markNotificationAsRead(notifId) },
                    onMarkAllAsRead = { mainViewModel.markAllNotificationsAsRead() },
                    onDeleteNotification = { notifId -> mainViewModel.deleteNotification(notifId) },
                    onNotificationClick = { notif ->
                        isNotificationsSheetOpen = false
                        if (notif.claimId.isNotBlank()) {
                            currentDestination = ClaimItNavDestination.CLAIMS
                        } else if (notif.itemId.isNotBlank()) {
                            currentDestination = ClaimItNavDestination.FEED
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ClaimItBottomMenu(
    currentDestination: ClaimItNavDestination,
    matchCount: Int,
    userRole: UserRole,
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
        val availableDestinations = ClaimItNavDestination.entries.filter {
            (it != ClaimItNavDestination.ADMIN) || (userRole == UserRole.ADMIN)
        }

        availableDestinations.forEach { destination ->
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
