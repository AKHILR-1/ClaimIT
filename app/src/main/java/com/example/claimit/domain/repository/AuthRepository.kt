package com.example.claimit.domain.repository

import com.example.claimit.domain.model.User
import com.example.claimit.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserState: StateFlow<User?>
    suspend fun register(
        email: String,
        pass: String,
        name: String,
        collegeEmail: String,
        studentId: String,
        role: UserRole
    ): Result<User>
    suspend fun login(email: String, pass: String): Result<User>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun checkEmailVerificationStatus(): Result<Boolean>
    suspend fun logout()
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateProfile(displayName: String, studentId: String): Result<User>
    suspend fun updateContactEmail(newContactEmail: String, currentPass: String): Result<User>
    suspend fun saveFcmToken(deviceId: String, token: String): Result<Unit>
    suspend fun removeFcmToken(deviceId: String): Result<Unit>
    suspend fun registerSession(userId: String, deviceId: String, deviceName: String, isAdmin: Boolean): Result<String>
    fun observeSessionRevocation(userId: String, sessionId: String): Flow<Boolean>
    fun getActiveDeviceSessions(userId: String): Flow<List<com.example.claimit.domain.model.DeviceSession>>
    suspend fun revokeSession(userId: String, sessionId: String): Result<Unit>
    suspend fun suspendUserAccount(userId: String): Result<Unit>
    suspend fun unsuspendUserAccount(userId: String): Result<Unit>
    fun getAllRegisteredUsers(): Flow<List<User>>
    suspend fun deleteAccount(): Result<Unit>
}
