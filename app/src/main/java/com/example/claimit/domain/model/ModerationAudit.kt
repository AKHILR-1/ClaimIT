package com.example.claimit.domain.model

data class ModerationAudit(
    val auditId: String,
    val reportId: String = "",
    val itemId: String,
    val itemName: String = "",
    val adminUid: String,
    val action: String, // e.g. "REMOVED", "UNDER_REVIEW", "DISMISSED"
    val reason: String,
    val previousStatus: String,
    val newStatus: String,
    val timestamp: Long = System.currentTimeMillis()
)
