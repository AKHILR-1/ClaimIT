package com.ssnexora.claimit

import com.ssnexora.claimit.data.repository.AdminRepositoryImpl
import com.ssnexora.claimit.data.repository.AuthRepositoryImpl
import com.ssnexora.claimit.data.repository.ClaimRepositoryImpl
import com.ssnexora.claimit.data.repository.ImageStorageRepositoryImpl
import com.ssnexora.claimit.data.repository.ItemRepositoryImpl
import com.ssnexora.claimit.data.repository.ModerationRepositoryImpl
import com.ssnexora.claimit.data.repository.NotificationRepositoryImpl
import com.ssnexora.claimit.domain.repository.AdminAuthState
import com.ssnexora.claimit.presentation.admin.AdminViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminRepositoryTest {

    private val dispatchers = TestCoroutineDispatchers()
    private lateinit var imageStorageRepository: ImageStorageRepositoryImpl
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var itemRepository: ItemRepositoryImpl
    private lateinit var notificationRepository: NotificationRepositoryImpl
    private lateinit var claimRepository: ClaimRepositoryImpl
    private lateinit var moderationRepository: ModerationRepositoryImpl
    private lateinit var adminRepository: AdminRepositoryImpl

    @Before
    fun setup() {
        imageStorageRepository = ImageStorageRepositoryImpl()
        authRepository = AuthRepositoryImpl(dispatchers)
        itemRepository = ItemRepositoryImpl(dispatchers, imageStorageRepository)
        notificationRepository = NotificationRepositoryImpl(dispatchers)
        claimRepository = ClaimRepositoryImpl(itemRepository, dispatchers, notificationRepository)
        moderationRepository = ModerationRepositoryImpl(itemRepository, dispatchers)

        adminRepository = AdminRepositoryImpl(
            dispatchers = dispatchers,
            authRepository = authRepository,
            itemRepository = itemRepository,
            moderationRepository = moderationRepository,
            claimRepository = claimRepository
        )
    }

    @Test
    fun `verifySuperAdminClaims with unauthenticated user returns unauthorized`() = runBlocking {
        authRepository.logout()
        val result = adminRepository.verifySuperAdminClaims(forceRefresh = true)
        assertTrue(result.isFailure)
        val state = adminRepository.adminAuthState.value
        assertTrue(state is AdminAuthState.Unauthorized)
    }

    @Test
    fun `verifySuperAdminClaims for logged in admin user returns authorized`() = runBlocking {
        authRepository.login("admin@campus.edu", "admin123")
        val result = adminRepository.verifySuperAdminClaims(forceRefresh = false)
        assertTrue(result.isSuccess)
        val state = adminRepository.adminAuthState.value
        assertTrue(state is AdminAuthState.Authorized)
        if (state is AdminAuthState.Authorized) {
            assertEquals("user_admin_1", state.adminUid)
            assertEquals("admin@campus.edu", state.email)
        }
    }

    @Test
    fun `AdminViewModel automatically verifies claims on user login emission`() = runBlocking {
        authRepository.logout()
        val viewModel = AdminViewModel(
            adminRepository = adminRepository,
            authRepository = authRepository,
            itemRepository = itemRepository,
            moderationRepository = moderationRepository,
            imageStorageRepository = imageStorageRepository,
            dispatchers = dispatchers
        )

        // Login as admin
        authRepository.login("admin@campus.edu", "admin123")

        val state = viewModel.adminAuthState.value
        assertTrue(state is AdminAuthState.Authorized)
    }

    @Test
    fun `resetAdminAuth resets state to Idle`() = runBlocking {
        authRepository.login("admin@campus.edu", "admin123")
        adminRepository.verifySuperAdminClaims(forceRefresh = false)
        assertTrue(adminRepository.adminAuthState.value is AdminAuthState.Authorized)

        adminRepository.resetAdminAuth()
        assertTrue(adminRepository.adminAuthState.value is AdminAuthState.Idle)
    }

    @Test
    fun `getOverviewStats emits real system metrics`() = runBlocking {
        authRepository.login("admin@campus.edu", "admin123")
        val stats = adminRepository.getOverviewStats().first()
        assertNotNull(stats)
        assertTrue(stats.totalUsersCount >= 0)
        assertTrue(stats.totalReportsCount >= 0)
    }
}

