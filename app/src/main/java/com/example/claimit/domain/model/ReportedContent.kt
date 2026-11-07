package com.example.claimit.domain.model

enum class TargetType {
    ITEM,
    USER
}

enum class ReportReason {
    INAPPROPRIATE_CONTENT,
    SPAM_OR_DUPLICATE,
    FRAUDULENT_CLAIM,
    ILLEGAL_CONTENT,
    MISLEADING,
    SUSPICIOUS,
    POLICY_VIOLATION,
    OFFENSIVE_LANGUAGE,
    OTHER
}

enum class ReportStatus {
    PENDING,
    UNDER_REVIEW,
    RESOLVED,
    RESOLVED_REMOVED,
    RESOLVED_DISMISSED,
    DISMISSED
}

data class ReportedContent(
    val id: String,
    val organizationId: String,
    val reporterId: String,
    val reporterEmail: String,
    val targetType: TargetType = TargetType.ITEM,
    val targetId: String,
    val targetTitleOrName: String,
    val reason: ReportReason,
    val details: String,
    val status: ReportStatus = ReportStatus.PENDING,
    val resolvedByAdminId: String? = null,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val reportReference: String = "REF-" + id.takeLast(6).uppercase(),
    val reporterName: String = reporterEmail.substringBefore("@"),
    val reporterUid: String = reporterId,
    val itemId: String = targetId,
    val itemName: String = targetTitleOrName,
    val itemType: String = "FOUND",
    val itemOwnerDisplayName: String = "Campus Member",
    val reportCategory: String = reason.name,
    val explanation: String = details,
    val evidenceDetails: String = "",
    val reviewStatus: ReportStatus = status,
    val adminDecision: String = "",
    val adminExplanation: String = "",
    val resolvedAtTimestamp: Long? = null
)
