package com.example.claimit.domain.model

enum class TargetType {
    ITEM,
    USER
}

enum class ReportReason {
    INAPPROPRIATE_CONTENT,
    SPAM_OR_DUPLICATE,
    FRAUDULENT_CLAIM,
    OFFENSIVE_LANGUAGE,
    OTHER
}

enum class ReportStatus {
    PENDING,
    RESOLVED_REMOVED,
    RESOLVED_DISMISSED
}

data class ReportedContent(
    val id: String,
    val organizationId: String,
    val reporterId: String,
    val reporterEmail: String,
    val targetType: TargetType,
    val targetId: String,
    val targetTitleOrName: String,
    val reason: ReportReason,
    val details: String,
    val status: ReportStatus = ReportStatus.PENDING,
    val resolvedByAdminId: String? = null,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
