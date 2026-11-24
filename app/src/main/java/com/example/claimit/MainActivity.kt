package com.example.claimit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.claimit.core.dispatchers.DefaultDispatchers
import com.example.claimit.data.repository.AdminRepositoryImpl
import com.example.claimit.data.repository.AuthRepositoryImpl
import com.example.claimit.data.repository.CampusLocationRepositoryImpl
import com.example.claimit.data.repository.ClaimRepositoryImpl
import com.example.claimit.data.repository.DuplicateDetectionRepositoryImpl
import com.example.claimit.data.repository.ImageStorageRepositoryImpl
import com.example.claimit.data.repository.ItemClassificationRepositoryImpl
import com.example.claimit.data.repository.ItemRepositoryImpl
import com.example.claimit.data.repository.ModerationRepositoryImpl
import com.example.claimit.data.repository.NotificationRepositoryImpl
import com.example.claimit.domain.usecase.AnalyzeItemImageUseCase
import com.example.claimit.domain.usecase.CreateItemPostUseCase
import com.example.claimit.domain.usecase.DetectDuplicateListingsUseCase
import com.example.claimit.presentation.admin.AdminViewModel
import com.example.claimit.presentation.main.ClaimItMainScreen
import com.example.claimit.presentation.main.MainViewModel
import com.example.claimit.presentation.postcreation.PostCreationViewModel
import com.example.claimit.presentation.splash.SplashScreen
import com.example.claimit.ui.theme.ClaimITTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val dispatchers by lazy { DefaultDispatchers() }
    private val imageStorageRepository by lazy { ImageStorageRepositoryImpl() }
    private val authRepository by lazy { AuthRepositoryImpl(dispatchers) }
    private val itemRepository by lazy { ItemRepositoryImpl(dispatchers, imageStorageRepository) }
    private val locationRepository by lazy { CampusLocationRepositoryImpl(dispatchers) }
    private val classificationRepository by lazy { ItemClassificationRepositoryImpl(dispatchers) }
    private val duplicateRepository by lazy { DuplicateDetectionRepositoryImpl(itemRepository, dispatchers) }
    private val notificationRepository by lazy { NotificationRepositoryImpl(dispatchers) }
    private val claimRepository by lazy { ClaimRepositoryImpl(itemRepository, dispatchers, notificationRepository) }
    private val moderationRepository by lazy { ModerationRepositoryImpl(itemRepository, dispatchers) }
    private val adminRepository by lazy {
        AdminRepositoryImpl(
            dispatchers = dispatchers,
            authRepository = authRepository,
            itemRepository = itemRepository,
            moderationRepository = moderationRepository,
            claimRepository = claimRepository
        )
    }

    private val postCreationViewModel: PostCreationViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val analyzeUseCase = AnalyzeItemImageUseCase(classificationRepository)
                val detectDuplicateUseCase = DetectDuplicateListingsUseCase(duplicateRepository)
                val createPostUseCase = CreateItemPostUseCase(itemRepository, notificationRepository)

                return PostCreationViewModel(
                    analyzeItemImageUseCase = analyzeUseCase,
                    detectDuplicateListingsUseCase = detectDuplicateUseCase,
                    createItemPostUseCase = createPostUseCase,
                    locationRepository = locationRepository,
                    dispatchers = dispatchers,
                    authRepository = authRepository,
                    itemRepository = itemRepository
                ) as T
            }
        }
    }

    private val mainViewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    authRepository = authRepository,
                    itemRepository = itemRepository,
                    claimRepository = claimRepository,
                    moderationRepository = moderationRepository,
                    notificationRepository = notificationRepository,
                    dispatchers = dispatchers
                ) as T
            }
        }
    }

    private val adminViewModel: AdminViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AdminViewModel(
                    adminRepository = adminRepository,
                    authRepository = authRepository,
                    itemRepository = itemRepository,
                    moderationRepository = moderationRepository,
                    imageStorageRepository = imageStorageRepository,
                    dispatchers = dispatchers
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(false) } // Default Light Theme
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(2000L) // Show splash screen with full logo for 2 seconds
                showSplash = false
            }

            ClaimITTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        SplashScreen()
                    } else {
                        ClaimItMainScreen(
                            postCreationViewModel = postCreationViewModel,
                            mainViewModel = mainViewModel,
                            adminViewModel = adminViewModel,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = !isDarkTheme }
                        )
                    }
                }
            }
        }
    }
}
