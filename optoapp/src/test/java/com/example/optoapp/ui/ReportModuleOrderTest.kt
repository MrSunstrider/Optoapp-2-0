package com.example.optoapp.ui

import com.example.optoapp.ui.screens.ReportModule
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportModuleOrderTest {

    @Test
    fun modules_followEvidenceFirstUxPlanOrder() {
        assertEquals(
            listOf(
                ReportModule.RESUMEN,
                ReportModule.VENTAS,
                ReportModule.PACIENTES,
                ReportModule.INVENTARIO,
                ReportModule.CLINICO,
                ReportModule.EQUIPO,
            ),
            ReportModule.entries,
        )
    }
}
