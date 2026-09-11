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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Unit tests for recovery password PUT: timeouts + pendingRecoveryToken taxonomy.
 */
class AuthDelegateRecoveryTest {

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

    private fun recoveryIntent(token: String = "recovery-jwt-token"): Intent {
        val uri = mockk<Uri>()
        every { uri.fragment } returns "access_token=$token&type=recovery"
        val intent = mockk<Intent>()
        every { intent.data } returns uri
        return intent
    }

    private fun mockConnection(responseCode: Int): HttpURLConnection {
        val conn = mockk<HttpURLConnection>(relaxed = true)
        every { conn.responseCode } returns responseCode
        every { conn.outputStream } returns ByteArrayOutputStream()
        every { conn.errorStream } returns null
        return conn
    }

    private fun buildDelegate(
        connectionFactory: (URL) -> HttpURLConnection = {
            URL("https://example.invalid").openConnection() as HttpURLConnection
        },
    ): AuthDelegate {
        return object : AuthDelegate(
            securityManager = mockk(relaxed = true),
            sessionManager = mockk(relaxed = true),
            repository = mockk(relaxed = true),
            membershipRepository = mockk(relaxed = true),
            supabase = mockk(relaxed = true),
            fiscalStore = mockk(relaxed = true),
            appContext = mockk(relaxed = true),
        ) {
            override fun recoveryUserUrl(): URL = URL("https://example.invalid/auth/v1/user")

            override fun openRecoveryPasswordConnection(url: URL): HttpURLConnection =
                connectionFactory(url)
        }
    }

    @Test
    fun openRecoveryPasswordConnection_setsConnectAndReadTimeouts() {
        val delegate = AuthDelegate(
            securityManager = mockk(relaxed = true),
            sessionManager = mockk(relaxed = true),
            repository = mockk(relaxed = true),
            membershipRepository = mockk(relaxed = true),
            supabase = mockk(relaxed = true),
            fiscalStore = mockk(relaxed = true),
            appContext = mockk(relaxed = true),
        )
        val conn = delegate.openRecoveryPasswordConnection(URL("https://example.invalid/auth/v1/user"))
        try {
            assertEquals(AuthDelegate.RECOVERY_CONNECT_TIMEOUT_MS, conn.connectTimeout)
            assertEquals(AuthDelegate.RECOVERY_READ_TIMEOUT_MS, conn.readTimeout)
        } finally {
            conn.disconnect()
        }
    }

    @Test
    fun updatePassword_http2xx_clearsToken() = runTest {
        val conn = mockConnection(200)
        val delegate = buildDelegate { conn }
        assertNull(delegate.handleRecoveryDeepLink(recoveryIntent()))
        assertTrue(delegate.hasPendingRecoveryToken())

        val error = delegate.updatePassword("Valid1!pass")
        assertNull(error)
        assertFalse(delegate.hasPendingRecoveryToken())
    }

    @Test
    fun updatePassword_http401_clearsToken() = runTest {
        val conn = mockConnection(401)
        val delegate = buildDelegate { conn }
        assertNull(delegate.handleRecoveryDeepLink(recoveryIntent()))

        val error = delegate.updatePassword("Valid1!pass")
        assertNotNull(error)
        assertFalse(delegate.hasPendingRecoveryToken())
    }

    @Test
    fun updatePassword_http403_clearsToken() = runTest {
        val conn = mockConnection(403)
        val delegate = buildDelegate { conn }
        assertNull(delegate.handleRecoveryDeepLink(recoveryIntent()))

        val error = delegate.updatePassword("Valid1!pass")
        assertNotNull(error)
        assertFalse(delegate.hasPendingRecoveryToken())
    }

    @Test
    fun updatePassword_http500_keepsTokenForRetry() = runTest {
        val first = mockConnection(500)
        val second = mockConnection(200)
        var calls = 0
        val delegate = buildDelegate {
            if (calls++ == 0) first else second
        }
        assertNull(delegate.handleRecoveryDeepLink(recoveryIntent("retry-token")))

        val error = delegate.updatePassword("Valid1!pass")
        assertNotNull(error)
        assertTrue(delegate.hasPendingRecoveryToken())

        val retry = delegate.updatePassword("Valid1!pass")
        assertNull(retry)
        assertFalse(delegate.hasPendingRecoveryToken())
        verify { second.setRequestProperty("Authorization", "Bearer retry-token") }
    }

    @Test
    fun updatePassword_http422_keepsToken() = runTest {
        val conn = mockConnection(422)
        val delegate = buildDelegate { conn }
        assertNull(delegate.handleRecoveryDeepLink(recoveryIntent()))

        assertNotNull(delegate.updatePassword("Valid1!pass"))
        assertTrue(delegate.hasPendingRecoveryToken())
    }

    @Test
    fun updatePassword_http400_keepsToken() = runTest {
        val conn = mockConnection(400)
        val delegate = buildDelegate { conn }
        assertNull(delegate.handleRecoveryDeepLink(recoveryIntent()))

        assertNotNull(delegate.updatePassword("Valid1!pass"))
        assertTrue(delegate.hasPendingRecoveryToken())
    }

    @Test
    fun updatePassword_http429_keepsToken() = runTest {
        val conn = mockConnection(429)
        val delegate = buildDelegate { conn }
        assertNull(delegate.handleRecoveryDeepLink(recoveryIntent()))

        assertNotNull(delegate.updatePassword("Valid1!pass"))
        assertTrue(delegate.hasPendingRecoveryToken())
    }

    @Test
    fun updatePassword_ioException_keepsToken() = runTest {
        val conn = mockk<HttpURLConnection>(relaxed = true)
        every { conn.outputStream } returns ByteArrayOutputStream()
        every { conn.responseCode } throws IOException("connection reset")
        val delegate = buildDelegate { conn }
        assertNull(delegate.handleRecoveryDeepLink(recoveryIntent()))

        assertNotNull(delegate.updatePassword("Valid1!pass"))
        assertTrue(delegate.hasPendingRecoveryToken())
    }

    @Test
    fun updatePassword_socketTimeout_keepsToken() = runTest {
        val conn = mockk<HttpURLConnection>(relaxed = true)
        every { conn.outputStream } returns ByteArrayOutputStream()
        every { conn.responseCode } throws SocketTimeoutException("read timed out")
        val delegate = buildDelegate { conn }
        assertNull(delegate.handleRecoveryDeepLink(recoveryIntent()))

        assertNotNull(delegate.updatePassword("Valid1!pass"))
        assertTrue(delegate.hasPendingRecoveryToken())
    }

    @Test
    fun clearPendingRecoveryToken_clearsStoredToken() = runTest {
        val delegate = buildDelegate { mockConnection(200) }
        assertNull(delegate.handleRecoveryDeepLink(recoveryIntent()))
        assertTrue(delegate.hasPendingRecoveryToken())

        delegate.clearPendingRecoveryToken()
        assertFalse(delegate.hasPendingRecoveryToken())
    }
}
