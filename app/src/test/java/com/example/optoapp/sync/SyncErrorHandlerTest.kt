package com.example.optoapp.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class SyncErrorHandlerTest {

    @Test
    fun `errorLabelForException returns network label for IOException`() {
        assertEquals("Error en red", errorLabelForException(IOException("timeout")))
    }

    @Test
    fun `errorLabelForException returns inesperado label for generic Exception`() {
        assertEquals("Error inesperado", errorLabelForException(RuntimeException("crash")))
    }

    @Test
    fun `errorLabelForException returns inesperado label for IllegalStateException`() {
        assertEquals("Error inesperado", errorLabelForException(IllegalStateException("bad state")))
    }

    @Test
    fun `errorLabelForException returns inesperado label for IllegalArgumentException`() {
        assertEquals("Error inesperado", errorLabelForException(IllegalArgumentException("invalid")))
    }
}
