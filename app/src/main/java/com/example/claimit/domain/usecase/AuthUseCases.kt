package com.example.claimit.domain.usecase

import com.example.claimit.domain.model.User
import com.example.claimit.domain.model.UserRole
import com.example.claimit.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

class AuthUseCases(
    private val authRepository: AuthRepository
) {
    val currentUserState: StateFlow<User?> = authRepository.currentUserState

    suspend fun login(email: String, pass: String): Result<User> {
        return authRepository.login(email, pass)
    }

    suspend fun register(
        email: String,
        pass: String,
        name: String,
        collegeEmail: String,
        studentId: String,
        role: UserRole
    ): Result<User> {
        return authRepository.register(email, pass, name, collegeEmail, studentId, role)
    }

    suspend fun logout() {
        authRepository.logout()
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return authRepository.resetPassword(email)
    }

    suspend fun updateProfile(displayName: String, studentId: String): Result<User> {
        return authRepository.updateProfile(displayName, studentId)
    }

    suspend fun updateContactEmail(newContactEmail: String, currentPass: String): Result<User> {
        return authRepository.updateContactEmail(newContactEmail, currentPass)
    }

    suspend fun deleteAccount(): Result<Unit> {
        return authRepository.deleteAccount()
    }
}
