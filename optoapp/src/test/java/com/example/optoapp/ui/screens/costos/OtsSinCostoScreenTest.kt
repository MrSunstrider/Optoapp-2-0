package com.example.optoapp.ui.screens.costos

import com.example.optoapp.data.OtSinCostoRow
import com.example.optoapp.viewmodel.OtsSinCostoUiState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class OtsSinCostoScreenTest {

    private val mayo = YearMonth.of(2026, 5)
    private val fila = OtSinCostoRow("d1", "OT-2026-0042", "p1", "Ana Perez", LocalDate.of(2026, 5, 9), 350.0, 2)

    @Test
    fun rowShowsTheOtCodePatientAndDate() {
        val texto = OtSinCostoFilaTexto.from(fila, sinPaciente = "Paciente sin nombre")

        assertEquals("OT-2026-0042", texto.identificacion)
        assertEquals("Ana Perez", texto.paciente)
        assertEquals("09/05/2026", texto.fecha)
    }

    @Test
    fun rowWithoutCodeOrPatientStillHasAnIdentification() {
        val texto = OtSinCostoFilaTexto.from(fila.copy(ot = "", pacienteNombre = null), sinPaciente = "Paciente sin nombre")

        assertEquals("d1", texto.identificacion)
        assertEquals("Paciente sin nombre", texto.paciente)
    }

    @Test
    fun emptyLoadedMonthShowsTheEmptyState() {
        assertEquals(OtsSinCostoContenido.VACIO, OtsSinCostoContenido.de(OtsSinCostoUiState(mes = mayo, isLoading = false)))
        assertEquals(OtsSinCostoContenido.CARGANDO, OtsSinCostoContenido.de(OtsSinCostoUiState(mes = mayo)))
        assertEquals(
            OtsSinCostoContenido.LISTA,
            OtsSinCostoContenido.de(OtsSinCostoUiState(mes = mayo, isLoading = false, filas = listOf(fila))),
        )
    }
}
