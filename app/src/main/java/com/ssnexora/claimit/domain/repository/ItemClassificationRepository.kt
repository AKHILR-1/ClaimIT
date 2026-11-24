package com.ssnexora.claimit.domain.repository

import android.graphics.Bitmap
import com.ssnexora.claimit.domain.model.AIVectorMetadata

interface ItemClassificationRepository {
    /**
     * Downsamples and compresses bitmap stream before running native multimodal classifier.
     */
    suspend fun classifyItemImage(bitmap: Bitmap?): Result<AIVectorMetadata>
}

