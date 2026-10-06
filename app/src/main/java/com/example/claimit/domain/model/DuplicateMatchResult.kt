package com.example.claimit.domain.model

data class DuplicateMatchResult(
    val matchedPost: ItemPost,
    val compoundScore: Float,
    val semanticEmbeddingSim: Float,
    val campusZoneMatchScore: Float,
    val timeDecayScore: Float,
    val matchingAttributesSummary: List<String>
) {
    val scorePercentage: Int
        get() = (compoundScore * 100).toInt().coerceIn(0, 100)

    val isHighConfidenceMatch: Boolean
        get() = compoundScore >= 0.72f
}
