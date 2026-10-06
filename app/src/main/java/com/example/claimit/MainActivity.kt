package com.example.claimit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.claimit.core.dispatchers.DefaultDispatchers
import com.example.claimit.data.repository.CampusLocationRepositoryImpl
import com.example.claimit.data.repository.DuplicateDetectionRepositoryImpl
import com.example.claimit.data.repository.ItemClassificationRepositoryImpl
import com.example.claimit.data.repository.ItemRepositoryImpl
import com.example.claimit.domain.usecase.AnalyzeItemImageUseCase
import com.example.claimit.domain.usecase.CreateItemPostUseCase
import com.example.claimit.domain.usecase.DetectDuplicateListingsUseCase
import com.example.claimit.presentation.main.ClaimItMainScreen
import com.example.claimit.presentation.postcreation.PostCreationViewModel
import com.example.claimit.ui.theme.ClaimITTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PostCreationViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val dispatchers = DefaultDispatchers()
                val itemRepo = ItemRepositoryImpl(dispatchers)
                val locationRepo = CampusLocationRepositoryImpl(dispatchers)
                val classificationRepo = ItemClassificationRepositoryImpl(dispatchers)
                val duplicateRepo = DuplicateDetectionRepositoryImpl(itemRepo, dispatchers)

                val analyzeUseCase = AnalyzeItemImageUseCase(classificationRepo)
                val detectDuplicateUseCase = DetectDuplicateListingsUseCase(duplicateRepo)
                val createPostUseCase = CreateItemPostUseCase(itemRepo)

                return PostCreationViewModel(
                    analyzeItemImageUseCase = analyzeUseCase,
                    detectDuplicateListingsUseCase = detectDuplicateUseCase,
                    createItemPostUseCase = createPostUseCase,
                    locationRepository = locationRepo,
                    dispatchers = dispatchers
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClaimITTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ClaimItMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
