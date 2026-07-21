package com.example.optoapp.data

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Approval + behavior tests for [PacienteRepository] catch-block refactoring.
 *
 * Verifies:
 * - IOException in DAO calls is caught and returned as [Resource.Error]
 * - CancellationException propagates (NOT caught by generic handler)
 * - Generic Exception still returns [Resource.Error]
 */
@RunWith(RobolectricTestRunner::class)
class PacienteRepositoryErrorTest {

    private lateinit var pacienteDao: PacienteDao
    private lateinit var evaluacionDao: EvaluacionDao
    private lateinit var repo: PacienteRepository

    @Before
    fun setUp() {
        pacienteDao = mockk()
        evaluacionDao = mockk()
        repo = PacienteRepository(pacienteDao, evaluacionDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── PacienteDao.getPacienteById ────────────────────────────────────

    @Test
    fun `getPacienteById dao throws IOException returns Error`() = runTest {
        coEvery { pacienteDao.getPacienteById("p1") } throws IOException("Network error")

        val result = repo.getPacienteById("p1")

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        assertNotNull((result as Resource.Error).message)
    }

    @Test
    fun `getPacienteById dao throws CancellationException rethrows`() = runTest {
        coEvery { pacienteDao.getPacienteById("p1") } throws CancellationException("Cancelled")

        var caught = false
        try {
            repo.getPacienteById("p1")
        } catch (e: CancellationException) {
            caught = true
        }
        assertTrue("CancellationException must be rethrown, not caught", caught)
    }

    @Test
    fun `getPacienteById dao throws generic Exception returns Error`() = runTest {
        coEvery { pacienteDao.getPacienteById("p1") } throws RuntimeException("DB corrupt")

        val result = repo.getPacienteById("p1")

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        assertEquals("DB corrupt", (result as Resource.Error).message)
    }

    // ── EvaluacionDao.getEvaluacionById ────────────────────────────────

    @Test
    fun `getEvaluacionById dao throws IOException returns Error`() = runTest {
        coEvery { evaluacionDao.getEvaluacionById("e1") } throws IOException("Network error")

        val result = repo.getEvaluacionById("e1")

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        assertNotNull((result as Resource.Error).message)
    }

    @Test
    fun `getEvaluacionById dao throws CancellationException rethrows`() = runTest {
        coEvery { evaluacionDao.getEvaluacionById("e1") } throws CancellationException("Cancelled")

        var caught = false
        try {
            repo.getEvaluacionById("e1")
        } catch (e: CancellationException) {
            caught = true
        }
        assertTrue("CancellationException must be rethrown, not caught", caught)
    }

    @Test
    fun `getEvaluacionById dao throws generic Exception returns Error`() = runTest {
        coEvery { evaluacionDao.getEvaluacionById("e1") } throws RuntimeException("DB corrupt")

        val result = repo.getEvaluacionById("e1")

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        assertEquals("DB corrupt", (result as Resource.Error).message)
    }
}
