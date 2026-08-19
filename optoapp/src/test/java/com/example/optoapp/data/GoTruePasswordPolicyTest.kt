package com.example.optoapp.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoTruePasswordPolicyTest {

    @Test
    fun configToml_requiresMin6AndSymbolClasses() {
        val toml = repoFile("supabase/config.toml").readText()
        val min = Regex("""minimum_password_length\s*=\s*(\d+)""").find(toml)
            ?: error("minimum_password_length missing")
        val req = Regex("""password_requirements\s*=\s*"([^"]*)"""").find(toml)
            ?: error("password_requirements missing")

        assertEquals("6", min.groupValues[1])
        assertEquals("lower_upper_letters_digits_symbols", req.groupValues[1])
    }

    @Test
    fun hostedDashboardNote_mentionsSamePolicyAndDashboard() {
        val note = repoFile("docs/gotrue-hosted-password-policy.md").readText()
        assertTrue(note.contains("lower_upper_letters_digits_symbols"))
        assertTrue("hosted note must mention min length 6", Regex("""\b6\b""").containsMatchIn(note))
        assertTrue(
            "hosted note must mention dashboard or Management API",
            note.contains("dashboard", ignoreCase = true) ||
                note.contains("Management API", ignoreCase = true),
        )
        assertTrue("A10 unused invitaciones must be recorded", note.contains("invitaciones", ignoreCase = true))
    }

    private fun repoFile(relative: String): File {
        val found = listOf(File(relative), File("../$relative"), File("../../$relative"))
            .firstOrNull { it.exists() }
        assertTrue("$relative not found from ${File(".").absolutePath}", found != null)
        return found!!
    }
}
