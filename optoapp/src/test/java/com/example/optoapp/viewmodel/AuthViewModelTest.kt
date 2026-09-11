package com.example.optoapp.viewmodel

import android.util.Log
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.viewmodel.auth.AuthDelegate
import com.example.optoapp.viewmodel.auth.BackupDelegate
import com.example.optoapp.viewmodel.auth.ColdStartNavigation
import com.example.optoapp.viewmodel.auth.PinDelegate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
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
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.d(any(), any(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        authDelegate: AuthDelegate,
        pinDelegate: PinDelegate = mockk(relaxed = true),
    ): AuthViewModel {
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

    @Test
    fun prepareOpticaSelection_onFetchError_returnsErrorNotOkFalse() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.prepareOpticaSelection() } returns
            com.example.optoapp.data.membership.MembershipFetch.Error(java.io.IOException("net down"))

        val vm = buildViewModel(authDelegate)
        val prep = vm.prepareOpticaSelection()

        assertTrue(prep is OpticaSelectionPrep.Error)
        assertEquals("net down", (prep as OpticaSelectionPrep.Error).message)
    }

    @Test
    fun prepareOpticaSelection_onFetchErrorWithoutMessage_usesFallback() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.prepareOpticaSelection() } returns
            com.example.optoapp.data.membership.MembershipFetch.Error(RuntimeException())

        val vm = buildViewModel(authDelegate)
        val prep = vm.prepareOpticaSelection()

        assertTrue(prep is OpticaSelectionPrep.Error)
        assertEquals("Error al cargar ópticas", (prep as OpticaSelectionPrep.Error).message)
    }

    @Test
    fun prepareOpticaSelection_onEmpty_returnsOkFalse() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.prepareOpticaSelection() } returns
            com.example.optoapp.data.membership.MembershipFetch.Empty

        val vm = buildViewModel(authDelegate)
        val prep = vm.prepareOpticaSelection()

        assertTrue(prep is OpticaSelectionPrep.Ok)
        assertFalse((prep as OpticaSelectionPrep.Ok).hasMultiple)
        assertTrue(vm.pendingMemberships.value.isEmpty())
    }

    @Test
    fun prepareOpticaSelection_onSingleMembership_returnsOkFalse() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.prepareOpticaSelection() } returns
            com.example.optoapp.data.membership.MembershipFetch.Ok(
                listOf(com.example.optoapp.data.OpticaMembership("o1", "Una", "admin")),
            )

        val vm = buildViewModel(authDelegate)
        val prep = vm.prepareOpticaSelection()

        assertTrue(prep is OpticaSelectionPrep.Ok)
        assertFalse((prep as OpticaSelectionPrep.Ok).hasMultiple)
        assertEquals(1, vm.pendingMemberships.value.size)
    }

    @Test
    fun prepareOpticaSelection_onMultipleMemberships_returnsOkTrue() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.prepareOpticaSelection() } returns
            com.example.optoapp.data.membership.MembershipFetch.Ok(
                listOf(
                    com.example.optoapp.data.OpticaMembership("o1", "Una", "admin"),
                    com.example.optoapp.data.OpticaMembership("o2", "Dos", "empleado"),
                ),
            )

        val vm = buildViewModel(authDelegate)
        val prep = vm.prepareOpticaSelection()

        assertTrue(prep is OpticaSelectionPrep.Ok)
        assertTrue((prep as OpticaSelectionPrep.Ok).hasMultiple)
        assertEquals(2, vm.pendingMemberships.value.size)
    }

    // ── Recovery: reset + Error/retry contracts ───────────────────────────────

    @Test
    fun resetRecoveryState_setsIdleAndClearsDelegateToken() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        every { authDelegate.hasPendingRecoveryToken() } returns false
        coEvery { authDelegate.handleRecoveryDeepLink(any()) } returns null

        val vm = buildViewModel(authDelegate)
        vm.handleRecoveryDeepLink(mockk(relaxed = true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(RecoveryState.LinkReceived, vm.recoveryState.value)

        vm.resetRecoveryState()

        assertEquals(RecoveryState.Idle, vm.recoveryState.value)
        verify { authDelegate.clearPendingRecoveryToken() }
    }

    @Test
    fun updatePassword_retryableFailure_setsIsRetryableTrue() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        every { authDelegate.hasPendingRecoveryToken() } returns true
        coEvery { authDelegate.handleRecoveryDeepLink(any()) } returns null
        coEvery { authDelegate.updatePassword(any()) } returns
            "No se pudo actualizar la contraseña. (código 500)"

        val vm = buildViewModel(authDelegate)
        vm.handleRecoveryDeepLink(mockk(relaxed = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updatePassword("Valid1!pass")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.recoveryState.value
        assertTrue(state is RecoveryState.Error)
        assertEquals(
            "No se pudo actualizar la contraseña. (código 500)",
            (state as RecoveryState.Error).message,
        )
        assertTrue(state.isRetryable)
        verify(exactly = 0) { authDelegate.clearPendingRecoveryToken() }
    }

    @Test
    fun updatePassword_blankTokenFailure_setsIsRetryableFalse() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        every { authDelegate.hasPendingRecoveryToken() } returns false
        coEvery { authDelegate.handleRecoveryDeepLink(any()) } returns null
        coEvery { authDelegate.updatePassword(any()) } returns
            "No se pudo actualizar la contraseña. (código 401)"

        val vm = buildViewModel(authDelegate)
        vm.handleRecoveryDeepLink(mockk(relaxed = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updatePassword("Valid1!pass")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.recoveryState.value
        assertTrue(state is RecoveryState.Error)
        assertTrue((state as RecoveryState.Error).message.contains("401"))
        assertFalse(state.isRetryable)
    }

    @Test
    fun updatePassword_terminal403_setsIsRetryableFalse() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        every { authDelegate.hasPendingRecoveryToken() } returns false
        coEvery { authDelegate.handleRecoveryDeepLink(any()) } returns null
        coEvery { authDelegate.updatePassword(any()) } returns
            "No se pudo actualizar la contraseña. (código 403)"

        val vm = buildViewModel(authDelegate)
        vm.handleRecoveryDeepLink(mockk(relaxed = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updatePassword("Valid1!pass")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.recoveryState.value
        assertTrue(state is RecoveryState.Error)
        assertTrue((state as RecoveryState.Error).message.contains("403"))
        assertFalse(state.isRetryable)
    }

    @Test
    fun updatePassword_retryablePath_secondAttemptWithoutReset() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        every { authDelegate.hasPendingRecoveryToken() } returns true
        coEvery { authDelegate.handleRecoveryDeepLink(any()) } returns null
        coEvery { authDelegate.updatePassword(any()) } returns
            "No se pudo actualizar la contraseña. (código 500)"

        val vm = buildViewModel(authDelegate)
        vm.handleRecoveryDeepLink(mockk(relaxed = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updatePassword("Valid1!pass")
        testDispatcher.scheduler.advanceUntilIdle()

        val first = vm.recoveryState.value as RecoveryState.Error
        assertTrue(first.isRetryable)

        vm.updatePassword("Valid1!pass")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { authDelegate.updatePassword("Valid1!pass") }
        verify(exactly = 0) { authDelegate.clearPendingRecoveryToken() }
        val second = vm.recoveryState.value as RecoveryState.Error
        assertTrue(second.isRetryable)
    }

    // ── Part 2b: OAuth null-data Error + awaitable deep-link APIs ─────────────

    @Test
    fun awaitHandleAuthDeepLinkIntent_nullDataError_setsAuthErrorWithoutResolvePostLogin() =
        runTest {
            val authDelegate = mockk<AuthDelegate>(relaxed = true)
            coEvery { authDelegate.handleAuthDeepLinkIntent(any()) } returns "Enlace inválido"

            val vm = buildViewModel(authDelegate)
            vm.awaitHandleAuthDeepLinkIntent(mockk(relaxed = true))

            val state = vm.authState.value
            assertTrue(state is AuthState.Error)
            assertEquals("Enlace inválido", (state as AuthState.Error).message)
            coVerify(exactly = 0) { authDelegate.resolvePostLogin(any(), any()) }
            coVerify(exactly = 0) { authDelegate.resolvePostLogin() }
            coVerify(exactly = 0) { authDelegate.resetLocalStoreForNewAuthSession() }
        }

    @Test
    fun awaitHandleAuthDeepLinkIntent_success_stillResolvesPostLogin() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.handleAuthDeepLinkIntent(any()) } returns null
        coEvery { authDelegate.resolvePostLogin(any(), any()) } returns AuthDelegate.PostLoginResult(
            email = "u@example.com",
            name = "U",
            memberships = emptyList(),
            requiresSelection = false,
            requiresOnboarding = true,
        )
        coEvery { authDelegate.resolvePostLogin() } returns AuthDelegate.PostLoginResult(
            email = "u@example.com",
            name = "U",
            memberships = emptyList(),
            requiresSelection = false,
            requiresOnboarding = true,
        )

        val vm = buildViewModel(authDelegate)
        vm.awaitHandleAuthDeepLinkIntent(mockk(relaxed = true))

        assertEquals(AuthState.Success, vm.authState.value)
        coVerify(atLeast = 1) { authDelegate.resolvePostLogin() }
    }

    @Test
    fun awaitHandleRecoveryDeepLink_success_setsLinkReceived() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.handleRecoveryDeepLink(any()) } returns null

        val vm = buildViewModel(authDelegate)
        vm.awaitHandleRecoveryDeepLink(mockk(relaxed = true))

        assertEquals(RecoveryState.LinkReceived, vm.recoveryState.value)
    }

    @Test
    fun awaitHandleRecoveryDeepLink_error_setsRecoveryError() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.handleRecoveryDeepLink(any()) } returns "Enlace inválido"

        val vm = buildViewModel(authDelegate)
        vm.awaitHandleRecoveryDeepLink(mockk(relaxed = true))

        val state = vm.recoveryState.value
        assertTrue(state is RecoveryState.Error)
        assertEquals("Enlace inválido", (state as RecoveryState.Error).message)
    }

    @Test
    fun awaitAuthDeepLink_thenCheckExistingSession_delegateOrderPreserved() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.handleAuthDeepLinkIntent(any()) } returns "Enlace inválido"
        coEvery { authDelegate.checkExistingSession() } returns false

        val vm = buildViewModel(authDelegate)
        vm.awaitHandleAuthDeepLinkIntent(mockk(relaxed = true))
        val sessionJob = vm.checkExistingSession()
        testDispatcher.scheduler.advanceUntilIdle()
        sessionJob.join()

        coVerifyOrder {
            authDelegate.handleAuthDeepLinkIntent(any())
            authDelegate.checkExistingSession()
        }
    }

    @Test
    fun handleAuthDeepLinkIntent_jobWrapper_stillSurfacesError() = runTest {
        val authDelegate = mockk<AuthDelegate>(relaxed = true)
        coEvery { authDelegate.handleAuthDeepLinkIntent(any()) } returns "Enlace inválido"

        val vm = buildViewModel(authDelegate)
        vm.handleAuthDeepLinkIntent(mockk(relaxed = true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.authState.value is AuthState.Error)
        coVerify(exactly = 0) { authDelegate.resolvePostLogin() }
    }

    @Test
    fun createPinAwaitingSuccess_validPin_returnsTrue() = runTest {
        val pinDelegate = mockk<PinDelegate>(relaxed = true)
        coEvery { pinDelegate.createPin("183729") } returns true
        val vm = buildViewModel(mockk(relaxed = true), pinDelegate)

        assertTrue(vm.createPinAwaitingSuccess("183729"))
        coVerify { pinDelegate.createPin("183729") }
    }

    @Test
    fun createPinAwaitingSuccess_invalidPin_returnsFalse() = runTest {
        val pinDelegate = mockk<PinDelegate>(relaxed = true)
        coEvery { pinDelegate.createPin("12") } returns false
        val vm = buildViewModel(mockk(relaxed = true), pinDelegate)

        assertFalse(vm.createPinAwaitingSuccess("12"))
        coVerify { pinDelegate.createPin("12") }
    }

    @Test
    fun createPinScreen_awaitsSuccessBeforeMainNavigation() {
        val relative = "src/main/java/com/example/optoapp/ui/screens/CreatePinScreen.kt"
        val found = listOf(
            java.io.File(relative),
            java.io.File("optoapp/$relative"),
        ).first { it.exists() }
        val text = found.readText()
        assertTrue(text.contains("createPinAwaitingSuccess"))
        assertFalse(
            "Must not fire-and-forget createPin before navigate",
            text.contains("viewModel.createPin(firstPin)"),
        )
    }
}
