package com.example.claimit.domain.model

data class DeviceSession(
    val sessionId: String,
    val userId: String,
    val deviceId: String,
    val deviceName: String = "Android Device",
    val isActive: Boolean = true,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)
