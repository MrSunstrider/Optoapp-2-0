package com.example.optoapp.data

import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.servicio.ServicioExtraDao
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
 * Approval + behavior tests for [DispensacionRepository] catch-block refactoring.
 *
 * Verifies:
 * - IOException in DAO calls is caught → [Resource.Error]
 * - CancellationException propagates
 * - Generic Exception returns [Resource.Error]
 */
@RunWith(RobolectricTestRunner::class)
class DispensacionRepositoryErrorTest {

    private lateinit var dispensacionDao: DispensacionDao
    private lateinit var dispensacionItemDao: DispensacionItemDao
    private lateinit var pagoDao: PagoDao
    private lateinit var servicioExtraDao: ServicioExtraDao
    private lateinit var repo: DispensacionRepository

    @Before
    fun setUp() {
        dispensacionDao = mockk()
        dispensacionItemDao = mockk()
        pagoDao = mockk()
        servicioExtraDao = mockk()
        repo = DispensacionRepository(dispensacionDao, dispensacionItemDao, pagoDao, servicioExtraDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── DispensacionDao.getDispensacionById ───────────────────────────

    @Test
    fun `getDispensacionById dao throws IOException returns Error`() = runTest {
        coEvery { dispensacionDao.getDispensacionById("d1") } throws IOException("Network error")

        val result = repo.getDispensacionById("d1")

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        assertNotNull((result as Resource.Error).message)
    }

    @Test
    fun `getDispensacionById dao throws CancellationException rethrows`() = runTest {
        coEvery { dispensacionDao.getDispensacionById("d1") } throws CancellationException("Cancelled")

        var caught = false
        try {
            repo.getDispensacionById("d1")
        } catch (e: CancellationException) {
            caught = true
        }
        assertTrue("CancellationException must be rethrown, not caught", caught)
    }

    @Test
    fun `getDispensacionById dao throws generic Exception returns Error`() = runTest {
        coEvery { dispensacionDao.getDispensacionById("d1") } throws RuntimeException("DB corrupt")

        val result = repo.getDispensacionById("d1")

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        assertEquals("DB corrupt", (result as Resource.Error).message)
    }

    // ── ServicioExtraDao.getServicioById ───────────────────────────────

    @Test
    fun `getServicioById dao throws IOException returns Error`() = runTest {
        coEvery { servicioExtraDao.getServicioById("s1") } throws IOException("Network error")

        val result = repo.getServicioById("s1")

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        assertNotNull((result as Resource.Error).message)
    }

    @Test
    fun `getServicioById dao throws CancellationException rethrows`() = runTest {
        coEvery { servicioExtraDao.getServicioById("s1") } throws CancellationException("Cancelled")

        var caught = false
        try {
            repo.getServicioById("s1")
        } catch (e: CancellationException) {
            caught = true
        }
        assertTrue("CancellationException must be rethrown, not caught", caught)
    }

    @Test
    fun `getServicioById dao throws generic Exception returns Error`() = runTest {
        coEvery { servicioExtraDao.getServicioById("s1") } throws RuntimeException("DB corrupt")

        val result = repo.getServicioById("s1")

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        assertEquals("DB corrupt", (result as Resource.Error).message)
    }
}
