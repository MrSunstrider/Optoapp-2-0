package com.example.optoapp.domain

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the single-writer invariant: the local dispensación save owns a sale's stock effect,
 * finanzas upload is transport only. A reflection + source assertion outlives the deleted
 * call site, unlike a mock expectation on an API that no longer exists.
 */
class InventoryStockSingleWriterTest {

    private fun coordinatorSource(): String {
        val relative = "src/main/java/com/example/optoapp/domain/UploadSyncCoordinator.kt"
        val candidates = listOf(File(relative), File("optoapp/$relative"), File("../$relative"))
        val found = candidates.firstOrNull { it.exists() }
        assertTrue("UploadSyncCoordinator.kt not found from ${File(".").absolutePath}", found != null)
        return found!!.readText()
    }

    @Test
    fun uploadCoordinatorExposesNoStockAdjustmentApi() {
        val offenders = (
            UploadSyncCoordinator::class.java.declaredMethods.toList() +
                UploadSyncCoordinator.Companion::class.java.declaredMethods.toList()
            )
            .map { it.name }
            .filter { it.contains("AdjustStock", ignoreCase = true) }

        assertTrue(
            "Finanzas upload must expose no stock-adjustment API, found: $offenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun finanzasUploadDoesNotCallStockRpc() {
        val source = coordinatorSource()

        assertTrue(
            "Finanzas upload must not invoke rpc_adjust_montura_stock",
            !source.contains("rpc_adjust_montura_stock"),
        )
        assertTrue(
            "Dead stock-adjustment helpers must be gone",
            !source.contains("AdjustStock"),
        )
    }
}
