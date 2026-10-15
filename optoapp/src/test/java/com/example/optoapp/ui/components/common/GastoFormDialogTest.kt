package com.example.optoapp.ui.components.common

import org.junit.Assert.assertEquals
import org.junit.Test

class GastoFormDialogTest {

    @Test
    fun fijoIsTheFirstSegmentAndVariableTheSecond() {
        assertEquals(0, indiceTipoGasto("fijo"))
        assertEquals(1, indiceTipoGasto("variable"))
    }

    @Test
    fun segmentIndexMapsBackToTheType() {
        assertEquals("fijo", tipoGastoEnIndice(0))
        assertEquals("variable", tipoGastoEnIndice(1))
    }

    @Test
    fun unknownTypeSelectsNoSegment() {
        assertEquals(-1, indiceTipoGasto("mixto"))
    }
}
