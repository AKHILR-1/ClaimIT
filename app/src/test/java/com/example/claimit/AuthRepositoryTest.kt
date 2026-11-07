package com.example.claimit

import com.example.claimit.data.repository.AuthRepositoryImpl
import com.example.claimit.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var authRepository: AuthRepositoryImpl
    private val dispatchers = TestCoroutineDispatchers()

    @Before
    fun setup() {
        authRepository = AuthRepositoryImpl(dispatchers)
    }

    @Test
    fun `login with default student credentials succeeds`() = runBlocking {
        val result = authRepository.login("student@campus.edu", "password123")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("student@campus.edu", user?.email)
        assertEquals(UserRole.STUDENT, user?.role)
    }

    @Test
    fun `login with default admin credentials grants ADMIN role`() = runBlocking {
        val result = authRepository.login("admin@campus.edu", "admin123")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals(UserRole.ADMIN, user?.role)
    }

    @Test
    fun `registration with invalid email format fails`() = runBlocking {
        val result = authRepository.register(
            email = "invalidemailformat",
            pass = "password123",
            name = "Test User",
            collegeEmail = "invalidemailformat",
            studentId = "123456",
            role = UserRole.STUDENT
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `registration with valid college email succeeds`() = runBlocking {
        val result = authRepository.register(
            email = "newstudent@campus.edu",
            pass = "password123",
            name = "New Student",
            collegeEmail = "newstudent@campus.edu",
            studentId = "889911",
            role = UserRole.STUDENT
        )
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("newstudent@campus.edu", user?.collegeEmail)
    }

    @Test
    fun `logout clears current user state`() = runBlocking {
        authRepository.login("student@campus.edu", "password123")
        assertNotNull(authRepository.currentUserState.value)

        authRepository.logout()
        assertNull(authRepository.currentUserState.value)
    }

    @Test
    fun `updating contact email retains original college email`() = runBlocking {
        authRepository.register(
            email = "student.orig@college.edu",
            pass = "password123",
            name = "Student Orig",
            collegeEmail = "student.orig@college.edu",
            studentId = "998877",
            role = UserRole.STUDENT
        )

        val updateResult = authRepository.updateContactEmail("personal.email@gmail.com", "password123")
        assertTrue(updateResult.isSuccess)

        val updatedUser = authRepository.currentUserState.value
        assertNotNull(updatedUser)
        assertEquals("student.orig@college.edu", updatedUser?.collegeEmail)
        assertEquals("personal.email@gmail.com", updatedUser?.contactEmail)
    }

    @Test
    fun `registerSession for student returns session id`() = runBlocking {
        val sessionResult = authRepository.registerSession(
            userId = "user_student_1",
            deviceId = "dev_001",
            deviceName = "Android Test Device",
            isAdmin = false
        )
        assertTrue(sessionResult.isSuccess)
        assertNotNull(sessionResult.getOrNull())
    }
}
