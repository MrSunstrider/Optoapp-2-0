package com.example.optoapp.data.membership

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateOpticaReturnedIdTest {

    @Test
    fun persistableId_usesServerReturnedNotClientProposed() {
        val serverReturned = "opt_serveridabcdef0123456789"
        val clientProposed = "opt_clientidabcdef0123456789"

        val result = CreateOpticaReturnedId.persistableId(serverReturned)

        assertTrue(result.isSuccess)
        assertEquals(serverReturned, result.getOrThrow())
        assertNotEquals(clientProposed, result.getOrThrow())
    }

    @Test
    fun persistableId_blankFails() {
        val result = CreateOpticaReturnedId.persistableId("")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun persistableId_nullFails() {
        val result = CreateOpticaReturnedId.persistableId(null)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun persistableId_whitespaceFails() {
        val result = CreateOpticaReturnedId.persistableId("   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
