package com.example.optoapp.viewmodel.auth

import com.example.optoapp.viewmodel.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthAbandonTest {

    @Test
    fun loadingBecomesIdle() {
        assertEquals(AuthState.Idle, GoogleAuthAbandon.nextState(AuthState.Loading))
    }

    @Test
    fun idleStaysIdle() {
        assertEquals(AuthState.Idle, GoogleAuthAbandon.nextState(AuthState.Idle))
    }

    @Test
    fun successStaysSuccess() {
        assertEquals(AuthState.Success, GoogleAuthAbandon.nextState(AuthState.Success))
    }

    @Test
    fun errorStaysError() {
        val error = AuthState.Error("x")
        val next = GoogleAuthAbandon.nextState(error)
        assertTrue(next is AuthState.Error)
        assertEquals("x", (next as AuthState.Error).message)
    }
}
