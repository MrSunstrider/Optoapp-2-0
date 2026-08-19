package com.example.optoapp.viewmodel

import com.example.optoapp.data.SecurityManager
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.viewmodel.auth.AuthDelegate
import com.example.optoapp.viewmodel.auth.BackupDelegate
import com.example.optoapp.viewmodel.auth.ColdStartNavigation
import com.example.optoapp.viewmodel.auth.PinDelegate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for AuthViewModel — baseline + static contracts.
 *
 * AuthViewModel requires Hilt for construction (AuthDelegate, BackupDelegate depend
 * on Android/Supabase deps). The delegates' pure logic is tested in their own
 * test classes (AuthDelegateTest, BackupDelegateTest, PinDelegateTest).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(authDelegate: AuthDelegate): AuthViewModel {
        val pinDelegate = mockk<PinDelegate>(relaxed = true)
        val backupDelegate = mockk<BackupDelegate>(relaxed = true)
        every { authDelegate.isLoggedIn } returns flowOf(false)
        every { authDelegate.opticaId } returns flowOf("")
        every { authDelegate.opticaRol } returns flowOf("")
        every { authDelegate.userEmail } returns flowOf("")
        every { authDelegate.userName } returns flowOf("")
        every { authDelegate.userTimeZone } returns flowOf(null)
        every { pinDelegate.pinInput } returns MutableStateFlow("")
        every { pinDelegate.pinHasBeenSet } returns flowOf(false)
        every { pinDelegate.isPinRequired } returns flowOf(false)
        return AuthViewModel(authDelegate, pinDelegate, backupDelegate)
    }

    @Test
    fun isValidPin_validLengthAndDigits_returnsTrue() {
        assertTrue(SecurityManager.isValidPin("123789"))
    }

    @Test
    fun isValidPin_tooShort_returnsFalse() {
        assertFalse(SecurityManager.isValidPin("123"))
    }

    @Test
    fun isValidPin_tooLong_returnsFalse() {
        assertFalse(SecurityManager.isValidPin("1234567"))
    }

    @Test
    fun isValidPin_empty_returnsFalse() {
        assertFalse(SecurityManager.isValidPin(""))
    }

    @Test
    fun isValidPin_nonDigits_returnsFalse() {
        assertFalse(SecurityManager.isValidPin("12345a"))
    }

    @Test
    fun isValidPin_weakPattern_returnsFalse() {
        assertFalse(SecurityManager.isValidPin("123456"))
    }

    @Test
    fun isValidPin_repeatedPattern_returnsFalse() {
        assertFalse(SecurityManager.isValidPin("111111"))
    }

    @Test
    fun pinLength_isSix() {
        assertEquals(6, SecurityManager.PIN_LENGTH)
    }

    @Test
    fun authStateLoading_isNotIdle() {
        val loading = AuthState.Loading
        val idle = AuthState.Idle
        assertNotEquals(idle, loading)
    }

    @Test
    fun authStateError_holdsMessage() {
        val error = AuthState.Error("Test error")
        assertEquals("Test error", (error as AuthState.Error).message)
    }

    @Test
    fun authStateSuccess_isNotError() {
        val success: AuthState = AuthState.Success
        assertFalse(success is AuthState.Error)
    }

    @Test
    fun checkExistingSession_isDeclared() {
        val methods = AuthViewModel::class.java.methods.map { it.name }
        assertTrue("checkExistingSession debe existir", "checkExistingSession" in methods)
    }

    @Test
    fun isAuthChecked_isDeclared() {
        val members = AuthViewModel::class.java.declaredFields.map { it.name } +
            AuthViewModel::class.java.methods.map { it.name }
        assertTrue("isAuthChecked debe existir", "isAuthChecked" in members)
    }

    @Test
    fun onGoogleAuthAbandoned_isDeclared() {
        val methods = AuthViewModel::class.java.methods.map { it.name }
        assertTrue("onGoogleAuthAbandoned debe existir", "onGoogleAuthAbandoned" in methods)
    }

    // ── checkExistingSession behavioral test ──────────────────────────────────

    @Test
    fun checkExistingSession_whenInvalid_doesNotCallDelegateLogout() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.checkExistingSession() } returns false

        val vm = buildViewModel(authDelegate)
        vm.checkExistingSession().join()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { authDelegate.logout() }
        assertTrue(vm.isAuthChecked.value)
    }

    @Test
    fun checkExistingSession_whenValid_doesNotCallDelegateLogout() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.checkExistingSession() } returns true

        val vm = buildViewModel(authDelegate)
        vm.checkExistingSession().join()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { authDelegate.logout() }
        assertTrue(vm.isAuthChecked.value)
    }

    @Test
    fun refreshMembershipsForWaitScreen_onError_returnsMinusOne() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.prepareOpticaSelection() } returns
            com.example.optoapp.data.membership.MembershipFetch.Error(java.io.IOException("net"))

        val vm = buildViewModel(authDelegate)
        val result = vm.refreshMembershipsForWaitScreen()

        assertEquals(-1, result)
    }

    @Test
    fun refreshMembershipsForWaitScreen_onEmpty_returnsZero() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.prepareOpticaSelection() } returns
            com.example.optoapp.data.membership.MembershipFetch.Empty

        val vm = buildViewModel(authDelegate)
        val result = vm.refreshMembershipsForWaitScreen()

        assertEquals(0, result)
    }
}
