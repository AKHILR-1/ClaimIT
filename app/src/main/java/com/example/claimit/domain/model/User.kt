package com.example.claimit.domain.model

enum class UserRole {
    STUDENT,
    ADMIN
}

enum class AccountStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    SUSPENDED
}

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val collegeEmail: String,
    val collegeEmailVerified: Boolean = false,
    val contactEmail: String = email,
    val contactEmailVerified: Boolean = false,
    val studentId: String = "",
    val registrationNumber: String = studentId,
    val organizationId: String = "lpu_main_campus",
    val role: UserRole = UserRole.STUDENT,
    val status: AccountStatus = AccountStatus.VERIFIED,
    val isEmailVerified: Boolean = false,
    val photoUrl: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val updatedAtTimestamp: Long = System.currentTimeMillis()
)
