package com.example.claimit.domain.repository

import android.graphics.Bitmap
import com.example.claimit.domain.model.AIVectorMetadata

interface ItemClassificationRepository {
    /**
     * Downsamples and compresses bitmap stream before running native multimodal classifier.
     */
    suspend fun classifyItemImage(bitmap: Bitmap): Result<AIVectorMetadata>
}
