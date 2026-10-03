package com.example.optoapp.domain

import com.example.optoapp.data.Pago
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LedgerSnapshotTest {

    private val date = LocalDate.of(2026, 9, 1)

    private fun pago(
        id: String,
        tipo: String,
        monto: Double,
        metodo: String,
        reversaPagoId: String? = null,
    ) = Pago(
        id = id,
        dispensacionId = "d1",
        fecha = date,
        tipo = tipo,
        monto = monto,
        metodoPago = metodo,
        opticaId = "o1",
        reversaPagoId = reversaPagoId,
    )

    @Test
    fun normalLedgerNetsUnreversedCreditsPerMetodo() {
        val snapshot = ledgerSnapshot(
            listOf(pago("a1", "Abono", 120.0, "Efectivo"), pago("a2", "Pago completo", 80.0, "Tarjeta")),
        )

        assertEquals(listOf("a1", "a2"), snapshot.unreversedCredits.map { it.id })
        assertEquals(emptyList<String>(), snapshot.legacyDebits.map { it.id })
        assertEquals(mapOf("Efectivo" to 120.0, "Tarjeta" to 80.0), snapshot.netByMetodo)
        assertEquals(200.0, snapshot.netPaid, 0.001)
    }

    @Test
    fun reversedPairIsExcludedFromCreditsAndDebits() {
        val snapshot = ledgerSnapshot(
            listOf(
                pago("a1", "Abono", 120.0, "Efectivo"),
                pago("r1", "Reverso", 120.0, "Efectivo", reversaPagoId = "a1"),
                pago("a2", "Abono", 50.0, "Yape"),
            ),
        )

        assertEquals(listOf("a2"), snapshot.unreversedCredits.map { it.id })
        assertEquals(emptyList<String>(), snapshot.legacyDebits.map { it.id })
        assertEquals(mapOf("Yape" to 50.0), snapshot.netByMetodo)
        assertEquals(50.0, snapshot.netPaid, 0.001)
    }

    @Test
    fun legacyReembolsoReducesNetOfItsMetodo() {
        val snapshot = ledgerSnapshot(
            listOf(pago("a1", "Abono", 200.0, "Efectivo"), pago("rb1", "Reembolso", 50.0, "Efectivo")),
        )

        assertEquals(listOf("rb1"), snapshot.legacyDebits.map { it.id })
        assertEquals(mapOf("Efectivo" to 150.0), snapshot.netByMetodo)
        assertEquals(150.0, snapshot.netPaid, 0.001)
    }

    @Test
    fun crossMethodLegacyDebitLeavesNegativeNetOnItsOwnMetodo() {
        val snapshot = ledgerSnapshot(
            listOf(pago("a1", "Abono", 100.0, "Efectivo"), pago("rb1", "Reembolso", 30.0, "Yape")),
        )

        assertEquals(100.0, snapshot.netByMetodo.getValue("Efectivo"), 0.001)
        assertEquals(-30.0, snapshot.netByMetodo.getValue("Yape"), 0.001)
        assertEquals(70.0, snapshot.netPaid, 0.001)
    }

    @Test
    fun orphanReversoCountsAsLegacyDebit() {
        val snapshot = ledgerSnapshot(
            listOf(
                pago("a1", "Abono", 100.0, "Efectivo"),
                pago("r-orphan", "Reverso", 40.0, "Efectivo", reversaPagoId = "gone"),
                pago("r-null", "Reverso", 10.0, "Efectivo"),
            ),
        )

        assertEquals(listOf("a1"), snapshot.unreversedCredits.map { it.id })
        assertEquals(listOf("r-orphan", "r-null"), snapshot.legacyDebits.map { it.id })
        assertEquals(50.0, snapshot.netPaid, 0.001)
    }

    @Test
    fun zeroEffectTiposAreIgnored() {
        val snapshot = ledgerSnapshot(
            listOf(pago("a1", " Abono ", 60.0, "Efectivo"), pago("c1", "CONTADO", 999.0, "Efectivo")),
        )

        assertEquals(listOf("a1"), snapshot.unreversedCredits.map { it.id })
        assertEquals(emptyList<String>(), snapshot.legacyDebits.map { it.id })
        assertEquals(mapOf("Efectivo" to 60.0), snapshot.netByMetodo)
    }
}
