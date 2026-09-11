package com.example.optoapp.viewmodel.auth

import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.viewmodel.RecoveryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColdStartNavigationTest {

    @Test
    fun recoveryBlocksColdStart_linkReceived() {
        assertTrue(ColdStartNavigation.recoveryBlocksColdStartRestore(RecoveryState.LinkReceived))
    }

    @Test
    fun recoveryBlocksColdStart_passwordUpdated() {
        assertTrue(ColdStartNavigation.recoveryBlocksColdStartRestore(RecoveryState.PasswordUpdated))
    }

    @Test
    fun recoveryBlocksColdStart_idleAndEmailSent_doNotBlock() {
        assertFalse(ColdStartNavigation.recoveryBlocksColdStartRestore(RecoveryState.Idle))
        assertFalse(ColdStartNavigation.recoveryBlocksColdStartRestore(RecoveryState.EmailSent))
    }

    @Test
    fun recoveryBlocksColdStart_loadingAndError_doNotBlock() {
        assertFalse(ColdStartNavigation.recoveryBlocksColdStartRestore(RecoveryState.Loading))
        assertFalse(
            ColdStartNavigation.recoveryBlocksColdStartRestore(
                RecoveryState.Error("fail", isRetryable = true),
            ),
        )
    }

    @Test
    fun incompleteCheck_doesNotRestore_evenIfSessionLooksValid() {
        val dest = ColdStartNavigation.dest(
            isAuthChecked = false,
            sessionValid = true,
            isLoggedIn = true,
            postLoginDest = Route.Main.route,
        )

        assertEquals(Route.Login.route, dest)
    }

    @Test
    fun checkedAndLoggedIn_usesPostLoginDest_pin() {
        val dest = ColdStartNavigation.dest(
            isAuthChecked = true,
            sessionValid = true,
            isLoggedIn = true,
            postLoginDest = Route.Pin.route,
        )

        assertEquals(Route.Pin.route, dest)
    }

    @Test
    fun checkedAndNoSession_staysLogin() {
        val dest = ColdStartNavigation.dest(
            isAuthChecked = true,
            sessionValid = false,
            isLoggedIn = false,
            postLoginDest = Route.Main.route,
        )

        assertEquals(Route.Login.route, dest)
    }

    @Test
    fun checkedAndLoggedIn_onboarding_goesSinOptica() {
        val dest = ColdStartNavigation.dest(
            isAuthChecked = true,
            sessionValid = true,
            isLoggedIn = true,
            postLoginDest = Route.SinOptica.route,
        )

        assertEquals(Route.SinOptica.route, dest)
    }

    @Test
    fun checkedButNotLoggedIn_staysLogin_evenIfSessionFlagTrue() {
        val dest = ColdStartNavigation.dest(
            isAuthChecked = true,
            sessionValid = true,
            isLoggedIn = false,
            postLoginDest = Route.Main.route,
        )

        assertEquals(Route.Login.route, dest)
    }

    @Test
    fun pinStateReady_waitsUntilRequiredFlagKnown() {
        assertEquals(false, ColdStartNavigation.pinStateReady(null, false))
    }

    @Test
    fun pinStateReady_waitsUntilSetFlagKnownWhenRequired() {
        assertEquals(false, ColdStartNavigation.pinStateReady(true, null))
    }

    @Test
    fun pinStateReady_optionalPinDoesNotWaitForSetFlag() {
        assertEquals(true, ColdStartNavigation.pinStateReady(false, null))
    }
}
