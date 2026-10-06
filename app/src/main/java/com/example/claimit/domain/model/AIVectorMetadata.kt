package com.example.claimit.domain.model

enum class AttributeType {
    CATEGORY,
    BRAND_MODEL,
    COLOR,
    MATERIAL,
    CONDITION,
    DISTINGUISHING_FEATURE
}

data class DetectedAttribute(
    val id: String,
    val value: String,
    val type: AttributeType,
    val confidenceScore: Float
) {
    val confidencePercentage: Int
        get() = (confidenceScore * 100).toInt().coerceIn(0, 100)
}

data class PhysicalAttributes(
    val dominantColor: String = "",
    val material: String = "",
    val condition: String = "",
    val distinguishingFeatures: String = ""
)

data class AIVectorMetadata(
    val vectorEmbedding: List<Float>,
    val structuralTags: List<String>,
    val detectedAttributes: List<DetectedAttribute>,
    val overallConfidence: Float,
    val analyzedAtTimestamp: Long
)
