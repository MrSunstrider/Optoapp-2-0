package com.example.optoapp.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Source locks for Part 2b MainActivity cold-start serialize + onNewIntent gate.
 * No Robolectric — assert bootstrap contracts in source.
 */
class AuthDeepLinkBootstrapSourceLockTest {

    @Test
    fun onCreate_awaitsViewDeepLinkBeforeCheckExistingSession() {
        val source = readMainActivitySource()
        val onCreate = extractMethod(source, "override fun onCreate")
        assertTrue(
            "Cold start must use lifecycleScope.launch for serialize",
            onCreate.contains("lifecycleScope.launch"),
        )
        assertTrue(
            "VIEW recovery path must await recovery handler",
            onCreate.contains("awaitHandleRecoveryDeepLink"),
        )
        assertTrue(
            "VIEW OAuth path must await OAuth handler",
            onCreate.contains("awaitHandleAuthDeepLinkIntent"),
        )
        assertTrue(
            "Session check must still run after await path",
            onCreate.contains("checkExistingSession()"),
        )
        val awaitRecoveryIdx = onCreate.indexOf("awaitHandleRecoveryDeepLink")
        val awaitOauthIdx = onCreate.indexOf("awaitHandleAuthDeepLinkIntent")
        val sessionIdx = onCreate.indexOf("checkExistingSession()")
        assertTrue(awaitRecoveryIdx >= 0 && awaitOauthIdx >= 0 && sessionIdx >= 0)
        assertTrue(
            "checkExistingSession must appear after await handlers (serialized)",
            sessionIdx > awaitRecoveryIdx && sessionIdx > awaitOauthIdx,
        )
        assertFalse(
            "Must not fire-and-forget Job wrappers concurrently before session on cold start",
            Regex("""authViewModel\.handle(RecoveryDeepLink|AuthDeepLinkIntent)\(""").containsMatchIn(onCreate),
        )
    }

    @Test
    fun onNewIntent_requiresActionViewAndNonNullData() {
        val source = readMainActivitySource()
        val onNewIntent = extractMethod(source, "override fun onNewIntent")
        assertTrue(
            "onNewIntent must gate on ACTION_VIEW",
            onNewIntent.contains("ACTION_VIEW"),
        )
        assertTrue(
            "onNewIntent must require non-null data",
            onNewIntent.contains("data != null") || onNewIntent.contains("intent.data != null"),
        )
    }

    @Test
    fun coldStart_suppressesRestoreWhenRecoveryOwnsStack() {
        val source = readMainActivitySource()
        assertTrue(
            "Cold-start must consult recoveryBlocksColdStartRestore",
            source.contains("recoveryBlocksColdStartRestore"),
        )
        val coldStartIdx = source.indexOf("recoveryBlocksColdStartRestore")
        assertTrue(coldStartIdx >= 0)
        val window = source.substring(coldStartIdx, minOf(source.length, coldStartIdx + 400))
        assertTrue(
            "Suppress path must mark coldStartHandled without navigating post-login",
            window.contains("coldStartHandled = true"),
        )
        assertTrue(
            "Cold-start LaunchedEffect keys must include recoveryState",
            source.contains("recoveryState,"),
        )
    }

    @Test
    fun scopeLock_noSingleTaskOrLoginResetReopen() {
        val manifest = readManifestSource()
        assertFalse(
            "launchMode singleTask must remain deferred (unchanged)",
            manifest.contains("singleTask"),
        )
        val login = readLoginScreenSource()
        val entryStart = login.indexOf("LaunchedEffect(Unit)")
        assertTrue(entryStart >= 0)
        val afterEntry = login.substring(entryStart)
        val nextLaunched = afterEntry.indexOf("LaunchedEffect(", startIndex = "LaunchedEffect(Unit)".length)
        val entryBlock = if (nextLaunched >= 0) afterEntry.substring(0, nextLaunched) else afterEntry
        assertFalse(
            "Must not reopen Login entry resetRecoveryState",
            entryBlock.contains("resetRecoveryState"),
        )
    }

    private fun extractMethod(source: String, signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("$signature not found", start >= 0)
        val brace = source.indexOf('{', start)
        var depth = 0
        for (i in brace until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(start, i + 1)
                }
            }
        }
        error("Unbalanced braces for $signature")
    }

    private fun readMainActivitySource(): String = readSource(
        "src/main/java/com/example/optoapp/MainActivity.kt",
        "MainActivity.kt",
    )

    private fun readManifestSource(): String = readSource(
        "src/main/AndroidManifest.xml",
        "AndroidManifest.xml",
    )

    private fun readLoginScreenSource(): String = readSource(
        "src/main/java/com/example/optoapp/ui/screens/LoginScreen.kt",
        "LoginScreen.kt",
    )

    private fun readSource(relative: String, fileName: String): String {
        val candidates = listOf(
            File("optoapp/$relative"),
            File(relative),
            File("../optoapp/$relative"),
        )
        val found = candidates.firstOrNull { it.exists() }
        if (found != null) return found.readText()
        val root = File(System.getProperty("user.dir") ?: ".")
        val walked = root.walkTopDown()
            .firstOrNull { it.name == fileName && (it.path.contains("main") || fileName == "AndroidManifest.xml") }
        requireNotNull(walked) { "$fileName not found from ${root.absolutePath}" }
        return walked.readText()
    }
}
