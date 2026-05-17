package com.example.optoapp

import com.example.optoapp.data.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResourceTest {
    @Test
    fun successHasData() {
        val r = Resource.Success("hola")
        assertEquals("hola", r.data)
        assertNull(r.message)
    }

    @Test
    fun errorHasMessage() {
        val r = Resource.Error<String>("algo falló")
        assertEquals("algo falló", r.message)
        assertNull(r.data)
    }

    @Test
    fun loadingNoData() {
        val r = Resource.Loading<Int>()
        assertNull(r.data)
    }

    @Test
    fun errorCanCarryData() {
        val r = Resource.Error("error", 42)
        assertEquals(42, r.data)
        assertEquals("error", r.message)
    }
}
