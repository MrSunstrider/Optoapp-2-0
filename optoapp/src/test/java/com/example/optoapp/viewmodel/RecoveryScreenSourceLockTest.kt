package com.example.optoapp.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source locks for Part 2a screen wiring (allowed under viewmodel test root; no Robolectric).
 */
class RecoveryScreenSourceLockTest {

    @Test
    fun loginScreen_entryEffect_doesNotCallResetRecoveryState() {
        val source = readMainSource("LoginScreen.kt")
        val entryStart = source.indexOf("LaunchedEffect(Unit)")
        assertTrue("LoginScreen must have entry LaunchedEffect(Unit)", entryStart >= 0)
        val afterEntry = source.substring(entryStart)
        val nextLaunched = afterEntry.indexOf("LaunchedEffect(", startIndex = "LaunchedEffect(Unit)".length)
        val entryBlock = if (nextLaunched >= 0) afterEntry.substring(0, nextLaunched) else afterEntry
        assertFalse(
            "Login entry LaunchedEffect must not call resetRecoveryState()",
            entryBlock.contains("resetRecoveryState"),
        )
        assertTrue(
            "Login entry LaunchedEffect must still load remembered email",
            entryBlock.contains("getRememberedEmail"),
        )
    }

    @Test
    fun newPasswordScreen_retryableErrorKeepsFormBranch() {
        val source = readMainSource("NewPasswordScreen.kt")
        assertTrue(
            "Retryable Error must stay on form via guarded when (!isRetryable → terminal)",
            source.contains("is RecoveryState.Error if !state.isRetryable"),
        )
        assertTrue(
            "Retryable Error banner must render from Error message on form branch",
            source.contains("state is RecoveryState.Error && state.isRetryable"),
        )
        assertTrue(
            "Guardar must call updatePassword without reset on form path",
            source.contains("viewModel.updatePassword(newPassword)"),
        )
    }

    @Test
    fun newPasswordScreen_terminalErrorCtaResetsAndNavigatesRecovery() {
        val source = readMainSource("NewPasswordScreen.kt")
        assertTrue(source.contains("Solicitar uno nuevo"))
        assertTrue(source.contains("Route.Recovery.route"))
        val ctaIdx = source.indexOf("Solicitar uno nuevo")
        assertTrue(ctaIdx > 0)
        val beforeCta = source.substring(0, ctaIdx)
        val resetIdx = beforeCta.lastIndexOf("resetRecoveryState")
        assertTrue("Terminal CTA must call resetRecoveryState before navigate", resetIdx > 0)
    }

    private fun readMainSource(fileName: String): String {
        val relative = "src/main/java/com/example/optoapp/ui/screens/$fileName"
        val candidates = listOf(
            java.io.File("optoapp/$relative"),
            java.io.File(relative),
            java.io.File("../optoapp/$relative"),
        )
        val found = candidates.firstOrNull { it.exists() }
        if (found != null) return found.readText()
        val root = java.io.File(System.getProperty("user.dir") ?: ".")
        val walked = root.walkTopDown()
            .firstOrNull { it.name == fileName && it.path.contains("main") }
        requireNotNull(walked) { "$fileName not found from ${root.absolutePath}" }
        return walked.readText()
    }
}
