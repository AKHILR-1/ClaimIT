package com.example.claimit.presentation.postcreation

sealed interface PostCreationSideEffect {
    data class ShowToastOrSnackbar(val message: String) : PostCreationSideEffect
    object TriggerHapticFeedback : PostCreationSideEffect
    data class PostCreatedSuccessfully(val postId: String, val trackingCode: String) : PostCreationSideEffect
    data class ConnectWithFinderOrOwner(val contactInfo: String, val postId: String) : PostCreationSideEffect
}
