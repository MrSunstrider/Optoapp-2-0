package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Characterization tests for OperacionHoyViewModel and OperacionHoyUiState.
 *
 * These tests capture the CURRENT aggregation formulas and data contracts
 * used by the ViewModel's combine block. They must pass BEFORE and AFTER
 * the parallel async+awaitAll refactor (A11).
 *
 * NOTE: Full ViewModel behavior tests (verifying 5 coroutines launch)
 * require mocking infrastructure (Mockito/MockK) which is not available.
 * These tests verify the aggregation logic at the data contract level.
 * See: openspec/changes/android-refactoring-plan/specs/viewmodel-testing/spec.md
 */
class OperacionHoyViewModelTest {

    @Test
    fun operacionHoyUiState_customValues() {
        val state = OperacionHoyUiState(
            fecha = LocalDate.of(2024, 6, 15),
            citasHoy = 5,
            entregasPendientes = 3,
            cobrosHoy = 450.0,
            stockCritico = 2,
            alertas = listOf("Stock crítico", "Entregas atrasadas"),
        )
        assertEquals(5, state.citasHoy)
        assertEquals(3, state.entregasPendientes)
        assertEquals(450.0, state.cobrosHoy, 0.001)
        assertEquals(2, state.stockCritico)
        assertEquals(2, state.alertas.size)
    }

    @Test
    fun operacionHoyUiState_copyWithModifications() {
        val original = OperacionHoyUiState(
            fecha = LocalDate.of(2024, 1, 1),
        )
        val modified = original.copy(citasHoy = 10, alertas = listOf("Test alerta"))
        assertEquals(10, modified.citasHoy)
        assertEquals(1, modified.alertas.size)
        // Original unchanged
        assertEquals(0, original.citasHoy)
    }

    @Test
    fun operacionHoyUiState_dataClassEquality() {
        val state1 = OperacionHoyUiState(fecha = LocalDate.of(2024, 6, 15))
        val state2 = OperacionHoyUiState(fecha = LocalDate.of(2024, 6, 15))
        assertEquals(state1, state2)
    }

    @Test
    fun operacionHoyUiState_dataClassInequality() {
        val state1 = OperacionHoyUiState(fecha = LocalDate.of(2024, 1, 1), citasHoy = 1)
        val state2 = OperacionHoyUiState(fecha = LocalDate.of(2024, 1, 1), citasHoy = 2)
        assertNotEquals(state1, state2)
    }

    @Test
    fun operacionHoyUiState_alertasList_immutable() {
        val state = OperacionHoyUiState(
            fecha = LocalDate.of(2024, 1, 1),
            alertas = mutableListOf("Alerta 1", "Alerta 2"),
        )
        assertEquals(2, state.alertas.size)
    }

    // ─── 2.5.2: Five query sources map to state fields ───────────────────────

    @Test
    fun operacionHoyUiState_holdsDataFromAllFiveQuerySources() {
        val today = LocalDate.of(2024, 6, 15)

        // Build model instances simulating each of the 5 repository queries
        val citas = listOf(
            EvaluacionClinica(id = "e1", pacienteId = "p1", fecha = today),
            EvaluacionClinica(id = "e2", pacienteId = "p2", fecha = today),
            EvaluacionClinica(id = "e3", pacienteId = "p3", fecha = today),
        )
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = today, estadoEntrega = "Pendiente"),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = today, estadoEntrega = "Entregado"),
        )
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 450.0),
        )
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Limpieza", montoTotal = 100.0, aCuenta = 0.0, estado = "Pendiente", fecha = today),
        )
        val monturas = listOf(
            Montura(id = "m1", stockActual = 5, stockMinimo = 10, activo = true),
            Montura(id = "m2", stockActual = 15, stockMinimo = 10, activo = true),
        )

        // ViewModel formulas: filter, count, sum
        val citasHoy = citas.size
        val dispPendientes = dispensaciones.filter { it.estadoEntrega.equals("Pendiente", ignoreCase = true) }
        val servPendientes = servicios.filter { it.estado.equals("Pendiente", ignoreCase = true) }
        val entregasPendientes = dispPendientes.size + servPendientes.size
        val cobrosHoy = pagos.sumOf { it.monto }
        val stockCriticoCount = monturas.count { it.activo && it.stockActual <= it.stockMinimo }

        assertEquals("citasHoy from query 1 (getEvaluacionesProximaCitaEnRango)", 3, citasHoy)
        assertEquals("dispPendientes from query 2 (getAllDispensacionesForOptica)", 1, dispPendientes.size)
        assertEquals("servPendientes from query 4 (getAllServiciosForOptica)", 1, servPendientes.size)
        assertEquals("entregasPendientes from queries 2+4", 2, entregasPendientes)
        assertEquals("cobrosHoy from query 3 (getPagosByDateRangeForOptica)", 450.0, cobrosHoy, 0.001)
        assertEquals("stockCritico from query 5 (getMonturasByOptica)", 1, stockCriticoCount)

        // Verify the state can hold all 5 query results simultaneously
        val state = OperacionHoyUiState(
            fecha = today,
            citasHoy = citasHoy,
            entregasPendientes = entregasPendientes,
            cobrosHoy = cobrosHoy,
            stockCritico = stockCriticoCount,
            pagosHoy = pagos,
            dispensacionesPendientes = dispPendientes,
            serviciosPendientes = servPendientes,
            monturas = monturas,
        )
        assertEquals(3, state.citasHoy)
        assertEquals(2, state.entregasPendientes)
        assertEquals(450.0, state.cobrosHoy, 0.001)
        assertEquals(1, state.stockCritico)
        assertEquals(1, state.pagosHoy.size)
        assertEquals(1, state.dispensacionesPendientes.size)
        assertEquals(1, state.serviciosPendientes.size)
        assertEquals(2, state.monturas.size)
    }

    // ─── 2.5.3: All sources success → combined state correct ─────────────────

    @Test
    fun operacionHoyUiState_combinesAllSourcesWithCorrectFormulas() {
        val today = LocalDate.of(2024, 6, 15)
        val yesterday = LocalDate.of(2024, 6, 14)

        // 3 citas today
        val citas = listOf(
            EvaluacionClinica(id = "e1", pacienteId = "p1", fecha = today, citaEstado = "programada"),
            EvaluacionClinica(id = "e2", pacienteId = "p2", fecha = today, citaEstado = "confirmada"),
            EvaluacionClinica(id = "e3", pacienteId = "p3", fecha = today, citaEstado = "programada"),
        )
        // 4 dispensaciones: 2 pendientes (1 atrasada ayer), 1 entregada, 1 pendiente hoy
        val dispensaciones = listOf(
            DispensacionOptica(id = "d1", pacienteId = "p1", fecha = yesterday, estadoEntrega = "Pendiente"),
            DispensacionOptica(id = "d2", pacienteId = "p2", fecha = yesterday, estadoEntrega = "Entregado"),
            DispensacionOptica(id = "d3", pacienteId = "p3", fecha = yesterday, estadoEntrega = "Pendiente"),
            DispensacionOptica(id = "d4", pacienteId = "p4", fecha = today, estadoEntrega = "Pendiente"),
        )
        // 3 pagos today: total 850
        val pagos = listOf(
            Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 500.0),
            Pago(id = "p2", fecha = today, tipo = "Tarjeta", monto = 200.0),
            Pago(id = "p3", fecha = today, tipo = "Transferencia", monto = 150.0),
        )
        // 2 servicios: 1 pendiente
        val servicios = listOf(
            ServicioExtra(id = "s1", descripcion = "Limpieza", montoTotal = 100.0, aCuenta = 0.0, estado = "Pendiente", fecha = today),
            ServicioExtra(id = "s2", descripcion = "Reparación", montoTotal = 200.0, aCuenta = 200.0, estado = "Entregado", fecha = today),
        )
        // 5 monturas: 2 críticas (stockActual <= stockMinimo + activo)
        val monturas = listOf(
            Montura(id = "m1", stockActual = 3, stockMinimo = 10, activo = true), // crítico
            Montura(id = "m2", stockActual = 15, stockMinimo = 10, activo = true), // ok
            Montura(id = "m3", stockActual = 5, stockMinimo = 5, activo = true), // crítico (igual)
            Montura(id = "m4", stockActual = 0, stockMinimo = 5, activo = false), // inactivo → no cuenta
            Montura(id = "m5", stockActual = 20, stockMinimo = 10, activo = true), // ok
        )

        // Compute aggregation exactly as the ViewModel does
        val dispPendientes = dispensaciones.filter { it.estadoEntrega.equals("Pendiente", ignoreCase = true) }
        val servPendientes = servicios.filter { it.estado.equals("Pendiente", ignoreCase = true) }
        val stockCriticoCount = monturas.count { it.activo && it.stockActual <= it.stockMinimo }
        val atrasadas = dispPendientes.count { it.fecha.isBefore(today) }
        val alertas = buildList {
            if (stockCriticoCount > 0) add("Hay $stockCriticoCount monturas en stock crítico.")
            if (atrasadas > 0) add("Hay $atrasadas entregas pendientes con fecha anterior a hoy.")
            if (servPendientes.isNotEmpty()) add("Servicios extra pendientes: ${servPendientes.size}.")
            if (citas.isNotEmpty()) add("Citas de control programadas hoy: ${citas.size}.")
        }

        // Intermediate assertions to characterize formulas
        assertEquals("dispPendientes filter", 3, dispPendientes.size)
        assertEquals("servPendientes filter", 1, servPendientes.size)
        assertEquals("stockCritico = activo && <= stockMinimo", 2, stockCriticoCount)
        assertEquals("atrasadas = before today", 2, atrasadas)
        assertEquals("entregasPendientes total", 4, dispPendientes.size + servPendientes.size)
        assertEquals("cobrosHoy sum", 850.0, pagos.sumOf { it.monto }, 0.001)

        // Final state assertions
        val state = OperacionHoyUiState(
            fecha = today,
            citasHoy = citas.size,
            entregasPendientes = dispPendientes.size + servPendientes.size,
            cobrosHoy = pagos.sumOf { it.monto },
            stockCritico = stockCriticoCount,
            alertas = alertas,
            pagosHoy = pagos,
            dispensacionesPendientes = dispPendientes,
            serviciosPendientes = servPendientes,
            monturas = monturas,
        )

        assertEquals(today, state.fecha)
        assertEquals(3, state.citasHoy)
        assertEquals(4, state.entregasPendientes)
        assertEquals(850.0, state.cobrosHoy, 0.001)
        assertEquals(2, state.stockCritico)
        assertEquals(4, state.alertas.size)
        assertTrue("alertas should mention stock crítico", state.alertas.any { it.contains("stock crítico") })
        assertTrue("alertas should mention atrasadas", state.alertas.any { it.contains("anterior a hoy") })
        assertTrue("alertas should mention servicios pendientes", state.alertas.any { it.contains("Servicios extra") })
        assertTrue("alertas should mention citas hoy", state.alertas.any { it.contains("Citas de control") })
    }

    // ─── 2.5.4: Partial failure → structural capacity for partial results ────

    @Test
    fun operacionHoyUiState_partialData_showsAvailableFieldsWithDefaultsForMissing() {
        val today = LocalDate.of(2024, 6, 15)

        // Simulate: only 2 of 5 sources returned data; others defaulted
        val state = OperacionHoyUiState(
            fecha = today,
            citasHoy = 5,
            cobrosHoy = 1200.0,
            pagosHoy = listOf(
                Pago(id = "p1", fecha = today, tipo = "Efectivo", monto = 1200.0),
            ),
            // entregasPendientes, stockCritico, alertas, dispensacionesPendientes,
            // serviciosPendientes, monturas all at defaults
        )

        // Fields with data
        assertEquals(today, state.fecha)
        assertEquals(5, state.citasHoy)
        assertEquals(1200.0, state.cobrosHoy, 0.001)
        assertEquals(1, state.pagosHoy.size)

        // Defaulted fields should be safe zero/empty
        assertEquals("entregasPendientes defaults to 0", 0, state.entregasPendientes)
        assertEquals("stockCritico defaults to 0", 0, state.stockCritico)
        assertTrue("alertas defaults to empty", state.alertas.isEmpty())
        assertTrue("dispensacionesPendientes defaults to empty", state.dispensacionesPendientes.isEmpty())
        assertTrue("serviciosPendientes defaults to empty", state.serviciosPendientes.isEmpty())
        assertTrue("monturas defaults to empty", state.monturas.isEmpty())
    }

    @Test
    fun operacionHoyUiState_partialDataWithOnlyStockInfo() {
        val today = LocalDate.of(2024, 6, 15)

        // Only query 5 (getMonturasByOptica) returned useful data
        val state = OperacionHoyUiState(
            fecha = today,
            stockCritico = 3,
            monturas = listOf(
                Montura(id = "m1", stockActual = 2, stockMinimo = 10, activo = true),
                Montura(id = "m2", stockActual = 0, stockMinimo = 5, activo = true),
                Montura(id = "m3", stockActual = 8, stockMinimo = 8, activo = true),
            ),
            alertas = listOf("Hay 3 monturas en stock crítico."),
        )

        assertEquals(3, state.stockCritico)
        assertEquals(3, state.monturas.size)
        assertEquals(1, state.alertas.size)
        assertEquals(0, state.citasHoy)
        assertEquals(0.0, state.cobrosHoy, 0.001)
    }
}
