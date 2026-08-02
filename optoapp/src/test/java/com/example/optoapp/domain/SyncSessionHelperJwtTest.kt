@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.optoapp.domain

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SyncSessionHelperJwtTest {

    @Before
    fun setUp() {
        mockkStatic("io.github.jan.supabase.auth.AuthKt")
    }

    private fun mockSupabase(authMock: Auth): SupabaseClient {
        val client = mockk<SupabaseClient>()
        every { client.auth } returns authMock
        return client
    }

    private fun mockAuth(
        sessionOrNull: UserSession?,
        refreshResult: () -> Unit = {},
        userOrNull: UserInfo? = mockk(),
    ): Auth {
        val auth = mockk<Auth>()
        every { auth.currentSessionOrNull() } returns sessionOrNull
        coEvery { auth.refreshCurrentSession() } answers { refreshResult() }
        every { auth.currentUserOrNull() } returns userOrNull
        return auth
    }

    private fun mockSession(accessToken: String, expiresAt: Instant): UserSession {
        val s = mockk<UserSession>()
        every { s.accessToken } returns accessToken
        every { s.expiresAt } returns expiresAt
        return s
    }

    private fun epochNow(): Long = java.lang.System.currentTimeMillis() / 1000

    @Test
    fun `forces refresh when token near expiry`() = runTest {
        val session = mockSession("valid", Instant.fromEpochSeconds(epochNow() + 120))
        val auth = mockAuth(sessionOrNull = session)
        val client = mockSupabase(auth)

        val result = SyncSessionHelper.refreshSessionBeforeSync(client)

        assertTrue(result)
        coVerify(exactly = 1) { auth.refreshCurrentSession() }
    }

    @Test
    fun `skips refresh when token fresh`() = runTest {
        val session = mockSession("valid", Instant.fromEpochSeconds(epochNow() + 600))
        val auth = mockAuth(sessionOrNull = session)
        val client = mockSupabase(auth)

        val result = SyncSessionHelper.refreshSessionBeforeSync(client)

        assertTrue(result)
        coVerify(exactly = 0) { auth.refreshCurrentSession() }
    }

    @Test
    fun `no session returns false`() = runTest {
        val auth = mockAuth(sessionOrNull = null)
        val client = mockSupabase(auth)

        val result = SyncSessionHelper.refreshSessionBeforeSync(client)

        assertFalse(result)
    }

    @Test
    fun `refresh failure returns false`() = runTest {
        val session = mockSession("valid", Instant.fromEpochSeconds(epochNow() + 120))
        val auth = mockAuth(sessionOrNull = session, refreshResult = { throw IOException("boom") })
        val client = mockSupabase(auth)

        val result = SyncSessionHelper.refreshSessionBeforeSync(client)

        assertFalse(result)
    }

    @Test
    fun `expired token forces refresh`() = runTest {
        val session = mockSession("valid", Instant.fromEpochSeconds(epochNow() - 60))
        val auth = mockAuth(sessionOrNull = session)
        val client = mockSupabase(auth)

        val result = SyncSessionHelper.refreshSessionBeforeSync(client)

        assertTrue(result)
        coVerify(exactly = 1) { auth.refreshCurrentSession() }
    }

    @Test
    fun `anonymous session returns false`() = runTest {
        val session = mockSession("valid", Instant.fromEpochSeconds(epochNow() + 120))
        val auth = mockAuth(sessionOrNull = session, userOrNull = null)
        val client = mockSupabase(auth)

        val result = SyncSessionHelper.refreshSessionBeforeSync(client)

        assertFalse(result)
    }
}
