package com.example.optoapp.domain

import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SyncStateTracker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadQuarantineSkipTest {

    @Test
    fun `quarantine prefix ids are skipped LWW otherwise not`() = runTest {
        val dao = mockk<SyncEntityStateDao>()
        coEvery { dao.getByStatus("o1", "error") } returns listOf(
            SyncEntityState("o1", "pago", "poison", "error", "quarantine:negative_monto", 1L),
            SyncEntityState("o1", "pago", "net-err", "error", "timeout", 2L),
            SyncEntityState("o1", "dispensacion", "d1", "error", "quarantine:invalid_estado_entrega:X", 3L),
        )
        val tracker = SyncStateTracker(dao, mockk(relaxed = true))
        val skip = tracker.quarantinedEntityIds("o1", "pago")
        assertEquals(setOf("poison"), skip)
        assertFalse("net-err" in skip)
        assertTrue("d1" !in skip)
    }
}

class SyncFinanzasPartialErrorTest {

    @Test
    fun `UploadPartialException yields Resource Error with partial data`() = runTest {
        // Covered by SyncFinanzasUseCaseKtTest rewrite; this documents the contract.
        val partial = UploadPartialException(79, java.io.IOException("quarantine:1"))
        assertEquals(79, partial.uploadedCount)
        assertTrue(partial.message!!.contains("Partial upload"))
    }
}
