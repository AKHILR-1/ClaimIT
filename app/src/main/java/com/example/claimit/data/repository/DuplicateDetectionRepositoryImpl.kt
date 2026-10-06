package com.example.claimit.data.repository

import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.core.util.TimeUtils
import com.example.claimit.domain.model.AIVectorMetadata
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.DuplicateMatchResult
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.repository.DuplicateDetectionRepository
import com.example.claimit.domain.repository.ItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class DuplicateDetectionRepositoryImpl(
    private val itemRepository: ItemRepository,
    private val dispatchers: CoroutineDispatchers
) : DuplicateDetectionRepository {

    override suspend fun detectDuplicates(
        type: ItemType,
        category: ItemCategory,
        title: String,
        description: String,
        location: CampusLocation?,
        aiMetadata: AIVectorMetadata?,
        threshold: Float,
        weightVector: Float,
        weightLocation: Float,
        weightTime: Float
    ): Result<List<DuplicateMatchResult>> = withContext(dispatchers.default) {
        try {
            // Opposite type query: if creating a FOUND post, cross-check against recent LOST reports, and vice-versa
            val targetType = if (type == ItemType.FOUND) ItemType.LOST else ItemType.FOUND
            val existingPosts = itemRepository.getRecentItemPosts(targetType).first()

            val matches = mutableListOf<DuplicateMatchResult>()

            for (post in existingPosts) {
                if (post.status != PostStatus.ACTIVE) continue

                // 1. Semantic Embedding Similarity
                val semanticSim = calculateSemanticSim(title, description, aiMetadata, post)

                // 2. Campus Zone Match Score
                val locationScore = calculateLocationScore(location, post.location)

                // 3. Time Decay Score
                val now = System.currentTimeMillis()
                val timeDecayScore = TimeUtils.calculateTimeDecay(now, post.createdAtTimestamp)

                // Compound Scoring Heuristic
                val compoundScore = (weightVector * semanticSim) +
                        (weightLocation * locationScore) +
                        (weightTime * timeDecayScore)

                if (compoundScore >= threshold) {
                    val matchingAttrs = mutableListOf<String>()
                    if (post.category == category) matchingAttrs.add("Category: ${category.displayName}")
                    if (location != null && location.building.id == post.location.building.id) {
                        matchingAttrs.add("Location: ${location.building.name}")
                    }
                    if (post.physicalAttributes.dominantColor.isNotBlank() &&
                        post.physicalAttributes.dominantColor.equals(aiMetadata?.detectedAttributes?.firstOrNull { it.type == com.example.claimit.domain.model.AttributeType.COLOR }?.value, ignoreCase = true)) {
                        matchingAttrs.add("Color match: ${post.physicalAttributes.dominantColor}")
                    }
                    if (matchingAttrs.isEmpty()) {
                        matchingAttrs.add("Title & description semantic overlap")
                    }

                    matches.add(
                        DuplicateMatchResult(
                            matchedPost = post,
                            compoundScore = compoundScore,
                            semanticEmbeddingSim = semanticSim,
                            campusZoneMatchScore = locationScore,
                            timeDecayScore = timeDecayScore,
                            matchingAttributesSummary = matchingAttrs
                        )
                    )
                }
            }

            Result.success(matches.sortedByDescending { it.compoundScore })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateSemanticSim(
        title: String,
        description: String,
        aiMetadata: AIVectorMetadata?,
        existingPost: ItemPost
    ): Float {
        val queryVector = aiMetadata?.vectorEmbedding
        val existingVector = existingPost.aiMetadata?.vectorEmbedding

        if (queryVector != null && existingVector != null && queryVector.size == existingVector.size) {
            // Cosine similarity
            var dot = 0.0f
            var normA = 0.0f
            var normB = 0.0f
            for (i in queryVector.indices) {
                dot += queryVector[i] * existingVector[i]
                normA += queryVector[i] * queryVector[i]
                normB += existingVector[i] * existingVector[i]
            }
            if (normA > 0f && normB > 0f) {
                return (dot / (sqrt(normA.toDouble()) * sqrt(normB.toDouble()))).toFloat().coerceIn(0.0f, 1.0f)
            }
        }

        // Textual keyword fallback similarity
        val queryWords = "$title $description".lowercase().split("\\s+".toRegex()).toSet()
        val targetWords = "${existingPost.title} ${existingPost.description}".lowercase().split("\\s+".toRegex()).toSet()
        if (queryWords.isEmpty() || targetWords.isEmpty()) return 0.0f

        val intersection = queryWords.intersect(targetWords).size
        val union = queryWords.union(targetWords).size
        return if (union > 0) intersection.toFloat() / union.toFloat() else 0.0f
    }

    private fun calculateLocationScore(loc1: CampusLocation?, loc2: CampusLocation): Float {
        if (loc1 == null) return 0.5f
        if (loc1.building.id == loc2.building.id) {
            if (loc1.floorWing.isNotBlank() && loc1.floorWing.equals(loc2.floorWing, ignoreCase = true)) {
                return 1.0f
            }
            return 0.85f
        }
        if (loc1.building.zoneName.equals(loc2.building.zoneName, ignoreCase = true)) {
            return 0.60f
        }
        return 0.20f
    }
}
