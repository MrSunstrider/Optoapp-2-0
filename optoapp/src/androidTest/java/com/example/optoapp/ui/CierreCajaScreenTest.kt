package com.example.optoapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.optoapp.data.Pago
import com.example.optoapp.ui.components.cierre_caja.TransactionItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@LargeTest
class CierreCajaScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun fakePago(
        id: String,
        metodoPago: String = "Efectivo",
        monto: Double = 50.0,
        dispensacionId: String? = null,
        servicioExtraId: String? = null,
        nota: String = ""
    ) = Pago(
        id = id,
        dispensacionId = dispensacionId,
        servicioExtraId = servicioExtraId,
        fecha = LocalDate.now(),
        tipo = "abono",
        monto = monto,
        metodoPago = metodoPago,
        nota = nota,
        opticaId = "optica-test"
    )

    // Regression test for Bug #1: LazyColumn inside verticalScroll crash.
    // The fix replaced LazyColumn with Column{forEach}. This test verifies that
    // rendering a non-empty pagos list inside a verticalScroll Column does NOT crash.
    @Test
    fun transactionList_multipleItems_insideScrollColumn_rendersWithoutCrash() {
        val pagos = listOf(
            fakePago("p-1", "Efectivo", 100.0),
            fakePago("p-2", "Tarjeta", 200.0),
            fakePago("p-3", "Transferencia", 50.0),
            fakePago("p-4", "Móvil", 75.0),
        )

        composeTestRule.setContent {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pagos.forEach { pago -> TransactionItem(pago) }
            }
        }

        composeTestRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tarjeta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transferencia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Móvil").assertIsDisplayed()
    }

    @Test
    fun transactionItem_withDispensacionId_showsDispensacionLabel() {
        val pago = fakePago("p-1", dispensacionId = "disp-123", monto = 150.0)

        composeTestRule.setContent { TransactionItem(pago) }

        composeTestRule.onNodeWithText("Dispensación").assertIsDisplayed()
    }

    @Test
    fun transactionItem_withoutDispensacionId_showsServicioExtraLabel() {
        val pago = fakePago("p-1", dispensacionId = null, servicioExtraId = "serv-1", monto = 80.0)

        composeTestRule.setContent { TransactionItem(pago) }

        composeTestRule.onNodeWithText("Servicio Extra").assertIsDisplayed()
    }

    @Test
    fun transactionItem_withNota_showsNota() {
        val pago = fakePago("p-1", nota = "Pago inicial lentes bifocales")

        composeTestRule.setContent { TransactionItem(pago) }

        composeTestRule.onNodeWithText("Pago inicial lentes bifocales").assertIsDisplayed()
    }
}
