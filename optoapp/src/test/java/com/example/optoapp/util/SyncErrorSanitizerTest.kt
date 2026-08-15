package com.example.optoapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncErrorSanitizerTest {

    private val restException = """
        HTTP 400 Bad Request
        Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc-secret_sig
        apikey=sb_publishable_ABC123XYZ
        {"code":"23514","message":"new row for relation \"servicios_extra\" violates check constraint \"servicios_extra_estado_domain_chk\"","entity_id":"a1b2c3d4-0000-4000-8000-000000000001","rows":80}
    """.trimIndent()

    @Test
    fun `forDiagnostics removes bearer token value`() {
        val out = SyncErrorSanitizer.forDiagnostics(restException)
        assertFalse("token value must not survive", out.contains("eyJhbGciOiJIUzI1NiJ9"))
        assertFalse(out.contains("abc-secret_sig"))
        assertTrue("redaction marker expected", out.contains("[omitido]"))
    }

    @Test
    fun `forDiagnostics removes apikey value`() {
        val out = SyncErrorSanitizer.forDiagnostics(restException)
        assertFalse("apikey value must not survive", out.contains("sb_publishable_ABC123XYZ"))
    }

    @Test
    fun `forDiagnostics removes access and refresh token json fields`() {
        val raw = """{"access_token":"eyJsecret1","refresh_token":"rt_secret2","code":"23505"}"""
        val out = SyncErrorSanitizer.forDiagnostics(raw)
        assertFalse(out.contains("eyJsecret1"))
        assertFalse(out.contains("rt_secret2"))
        assertTrue("PG code must survive", out.contains("23505"))
    }

    @Test
    fun `forDiagnostics keeps http status pg code constraint and entity id`() {
        val out = SyncErrorSanitizer.forDiagnostics(restException)
        assertTrue("HTTP status must survive", out.contains("400"))
        assertTrue("SQLSTATE must survive", out.contains("23514"))
        assertTrue(
            "constraint name must survive",
            out.contains("servicios_extra_estado_domain_chk"),
        )
        assertTrue(
            "entity id must survive",
            out.contains("a1b2c3d4-0000-4000-8000-000000000001"),
        )
        assertTrue("row counts must survive", out.contains("80"))
    }

    @Test
    fun `forDiagnostics does not collapse network detail`() {
        val raw = "java.net.SocketTimeoutException: Connect timeout has expired [url=https://x.supabase.co]"
        val out = SyncErrorSanitizer.forDiagnostics(raw)
        assertTrue("technical class must survive", out.contains("SocketTimeoutException"))
        assertTrue(out.contains("Connect timeout"))
    }

    @Test
    fun `forDiagnostics returns empty string for blank input`() {
        assertEquals("", SyncErrorSanitizer.forDiagnostics(null))
        assertEquals("", SyncErrorSanitizer.forDiagnostics("   "))
    }

    @Test
    fun `forUserMessage still collapses network errors`() {
        val out = SyncErrorSanitizer.forUserMessage("Connect timeout has expired")
        assertTrue(out.contains("conexión inestable"))
    }

    @Test
    fun `forUserMessage still redacts bearer`() {
        val out = SyncErrorSanitizer.forUserMessage("Authorization: Bearer eyJsecret")
        assertFalse(out.contains("eyJsecret"))
    }
}
