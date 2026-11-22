package com.example.claimit.presentation.feed.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.claimit.data.repository.DriveImageCache
import com.example.claimit.data.repository.ImageStorageRepositoryImpl
import com.example.claimit.domain.repository.ImageStorageRepository
import com.example.claimit.ui.theme.TagAiIndigo

private sealed class DriveImageRenderState {
    object Loading : DriveImageRenderState()
    data class Success(val bitmap: Bitmap) : DriveImageRenderState()
    data class Error(val message: String) : DriveImageRenderState()
    object Empty : DriveImageRenderState()
}

@Composable
fun ReportImageThumbnail(
    imageUrl: String?,
    imageFileId: String? = null,
    modifier: Modifier = Modifier,
    imageStorageRepository: ImageStorageRepository = remember { ImageStorageRepositoryImpl() }
) {
    // 1. Determine Google Drive File ID if present
    val driveFileId = remember(imageUrl, imageFileId) {
        if (!imageFileId.isNullOrBlank()) {
            imageFileId.trim()
        } else if (!imageUrl.isNullOrBlank() && imageUrl.startsWith("drive:")) {
            imageUrl.substringAfter("drive:").trim()
        } else if (!imageUrl.isNullOrBlank() && !imageUrl.contains("/") && !imageUrl.startsWith("http") && !imageUrl.startsWith("data:")) {
            imageUrl.trim()
        } else null
    }

    // 2. Drive Image State Lifecycle Management (ProduceState avoids network calls on recomposition)
    val driveState = if (driveFileId != null) {
        produceState<DriveImageRenderState>(
            initialValue = DriveImageCache.get(driveFileId)?.let { DriveImageRenderState.Success(it) } ?: DriveImageRenderState.Loading,
            key1 = driveFileId
        ) {
            val cached = DriveImageCache.get(driveFileId)
            if (cached != null) {
                value = DriveImageRenderState.Success(cached)
            } else {
                value = DriveImageRenderState.Loading
                val result = DriveImageCache.fetchOrGet(driveFileId, imageStorageRepository)
                value = result.fold(
                    onSuccess = { DriveImageRenderState.Success(it) },
                    onFailure = { DriveImageRenderState.Error(it.message ?: "Failed to fetch image") }
                )
            }
        }.value
    } else null

    // 3. Render Image based on source
    if (driveFileId != null && driveState != null) {
        Crossfade(targetState = driveState, label = "DriveImageCrossfade", modifier = modifier) { state ->
            when (state) {
                is DriveImageRenderState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TagAiIndigo.copy(alpha = 0.12f))
                            .border(1.dp, TagAiIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TagAiIndigo,
                            strokeWidth = 2.dp
                        )
                    }
                }
                is DriveImageRenderState.Success -> {
                    Image(
                        bitmap = state.bitmap.asImageBitmap(),
                        contentDescription = "Item Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is DriveImageRenderState.Error, is DriveImageRenderState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TagAiIndigo.copy(alpha = 0.08f))
                            .border(1.dp, TagAiIndigo.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "Failed to load Drive photo",
                            tint = TagAiIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Legacy Base64 or HTTP / Cloudinary URL rendering
        val base64Bitmap = remember(imageUrl) {
            if (!imageUrl.isNullOrBlank() && imageUrl.startsWith("data:image")) {
                try {
                    val base64Data = imageUrl.substringAfter("base64,")
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (_: Exception) {
                    null
                }
            } else null
        }

        if (base64Bitmap != null) {
            Image(
                bitmap = base64Bitmap.asImageBitmap(),
                contentDescription = "Item Photo",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else if (!imageUrl.isNullOrBlank() && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Item Photo",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier
                    .background(TagAiIndigo.copy(alpha = 0.12f))
                    .border(1.dp, TagAiIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Item Photo Placeholder",
                    tint = TagAiIndigo,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
