package com.example.optoapp.viewmodel.auth

import android.content.Intent
import android.net.Uri
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Part 2b: OAuth null-data fail-closed + no secret-bearing deep-link Uri logs.
 */
class AuthDelegateDeepLinkTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.d(any(), any(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun buildDelegate(): AuthDelegate = AuthDelegate(
        securityManager = mockk(relaxed = true),
        sessionManager = mockk(relaxed = true),
        repository = mockk(relaxed = true),
        membershipRepository = mockk(relaxed = true),
        supabase = mockk(relaxed = true),
        fiscalStore = mockk(relaxed = true),
        appContext = mockk(relaxed = true),
    )

    @Test
    fun handleAuthDeepLinkIntent_nullData_returnsEnlaceInvalido() = runTest {
        val intent = mockk<Intent>()
        every { intent.data } returns null

        val error = buildDelegate().handleAuthDeepLinkIntent(intent)

        assertNotNull(error)
        assertEquals("Enlace inválido", error)
    }

    @Test
    fun handleAuthDeepLinkIntent_nullIntent_returnsEnlaceInvalido() = runTest {
        val error = buildDelegate().handleAuthDeepLinkIntent(null)

        assertNotNull(error)
        assertEquals("Enlace inválido", error)
    }

    @Test
    fun handleRecoveryDeepLink_withTokenUri_doesNotLogFullUriOrAccessToken() = runTest {
        val token = "secret-access-token-xyz"
        val uri = mockk<Uri>()
        every { uri.fragment } returns "access_token=$token&type=recovery"
        every { uri.toString() } returns "optoapp://auth#access_token=$token&type=recovery"
        val intent = mockk<Intent>()
        every { intent.data } returns uri

        assertEquals(null, buildDelegate().handleRecoveryDeepLink(intent))

        verify(exactly = 0) {
            Log.d(any(), match { msg ->
                msg.contains(token) ||
                    msg.contains("access_token=") ||
                    msg.contains("optoapp://auth#")
            })
        }
    }

    @Test
    fun authDelegateSource_doesNotLogFullDeepLinkUri() {
        val source = readAuthDelegateSource()
        assertFalse(
            "OAuth handler must not Log.d full deepLink Uri",
            source.contains("Recibido deeplink OAuth: \$deepLink"),
        )
        assertFalse(
            "Recovery handler must not Log.d full deepLink Uri",
            source.contains("Recibido deeplink recovery: \$deepLink"),
        )
    }

    @Test
    fun logout_doesNotClearPendingRecoveryToken() = runTest {
        val uri = mockk<Uri>()
        every { uri.fragment } returns "access_token=keep-me&type=recovery"
        val intent = mockk<Intent>()
        every { intent.data } returns uri
        val delegate = buildDelegate()
        assertEquals(null, delegate.handleRecoveryDeepLink(intent))
        assertTrue(delegate.hasPendingRecoveryToken())

        delegate.logout()

        assertTrue(
            "logout must not clear pendingRecoveryToken (cold-start serialize invariant)",
            delegate.hasPendingRecoveryToken(),
        )
    }

    private fun readAuthDelegateSource(): String {
        val relative = "src/main/java/com/example/optoapp/viewmodel/auth/AuthDelegate.kt"
        val candidates = listOf(
            File("optoapp/$relative"),
            File(relative),
            File("../optoapp/$relative"),
        )
        val found = candidates.firstOrNull { it.exists() }
        if (found != null) return found.readText()
        val root = File(System.getProperty("user.dir") ?: ".")
        val walked = root.walkTopDown()
            .firstOrNull { it.name == "AuthDelegate.kt" && it.path.contains("main") }
        requireNotNull(walked) { "AuthDelegate.kt not found from ${root.absolutePath}" }
        return walked.readText()
    }
}
