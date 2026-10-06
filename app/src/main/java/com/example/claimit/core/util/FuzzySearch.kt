package com.example.claimit.core.util

import kotlin.math.min

object FuzzySearch {

    /**
     * Calculates the normalized similarity score between [query] and [target] (0.0 to 1.0).
     * Combines prefix matching and Levenshtein distance for optimal campus building search.
     */
    fun calculateSimilarity(query: String, target: String): Float {
        val q = query.trim().lowercase()
        val t = target.trim().lowercase()

        if (q.isEmpty()) return 1.0f
        if (t.isEmpty()) return 0.0f
        if (t == q) return 1.0f
        if (t.startsWith(q)) return 0.95f
        if (t.contains(q)) return 0.85f

        val distance = levenshteinDistance(q, t)
        val maxLength = maxOf(q.length, t.length)
        if (maxLength == 0) return 1.0f

        return (1.0f - (distance.toFloat() / maxLength.toFloat())).coerceIn(0.0f, 1.0f)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = IntArray(s2.length + 1) { it }

        for (i in 1..s1.length) {
            var previousCorner = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val currentCorner = dp[j]
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[j] = min(
                    min(dp[j] + 1, dp[j - 1] + 1),
                    previousCorner + cost
                )
                previousCorner = currentCorner
            }
        }
        return dp[s2.length]
    }
}
