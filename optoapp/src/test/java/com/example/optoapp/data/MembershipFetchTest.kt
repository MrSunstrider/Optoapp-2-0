package com.example.optoapp.data

import com.example.optoapp.data.membership.MembershipFetch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MembershipFetchTest {

    @Test
    fun fromCaught_ioException_isErrorNotEmpty() {
        val cause = IOException("timeout")
        val fetch = MembershipFetch.fromCaught(cause)

        assertTrue(fetch is MembershipFetch.Error)
        assertFalse(fetch is MembershipFetch.Empty)
        assertEquals(cause, (fetch as MembershipFetch.Error).cause)
    }

    @Test
    fun fromCaught_genericException_isError() {
        val cause = IllegalStateException("postgrest")
        val fetch = MembershipFetch.fromCaught(cause)

        assertTrue(fetch is MembershipFetch.Error)
        assertEquals(cause, (fetch as MembershipFetch.Error).cause)
    }

    @Test
    fun fromMapped_empty_isEmptyNotError() {
        val fetch = MembershipFetch.fromMapped(emptyList())

        assertTrue(fetch is MembershipFetch.Empty)
        assertFalse(fetch is MembershipFetch.Error)
    }

    @Test
    fun mapRow_blankRol_isSkipped() {
        val mapped = MembershipFetch.mapRow("opt-1", "Vista", "  ")

        assertEquals(null, mapped)
    }

    @Test
    fun mapRow_empleado_isKept() {
        val mapped = MembershipFetch.mapRow("opt-2", "Lentes", "empleado")

        assertEquals(OpticaMembership("opt-2", "Lentes", "empleado"), mapped)
    }

    @Test
    fun asList_error_isEmptyForSyncCallersOnly() {
        val fetch: MembershipFetch = MembershipFetch.Error(IOException("net"))

        assertTrue(fetch.asList().isEmpty())
        assertTrue(fetch is MembershipFetch.Error)
    }
}
