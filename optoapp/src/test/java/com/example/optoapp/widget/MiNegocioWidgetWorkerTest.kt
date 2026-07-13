package com.example.optoapp.widget

import android.widget.RemoteViews
import androidx.work.ListenableWorker
import com.example.optoapp.R
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import com.example.optoapp.widget.MiNegocioWidgetWorker.Companion.doWorkCore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private val TEST_WIDGET_IDS = intArrayOf(1)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MiNegocioWidgetWorkerTest {

    @Test
    fun doWorkCore_withEntity_updatesWidgetWithData() {
        val entity = ResumenDiarioEntity(
            id = "r1",
            opticaId = "o1",
            fecha = "2026-07-12",
            ventasCantidad = 5,
            ventasMontoTotal = 2500.0,
            saldoPendienteTotal = 800.0
        )

        val context = RuntimeEnvironment.getApplication()

        var capturedViews: RemoteViews? = null
        var capturedIds: IntArray? = null
        val result = doWorkCore(context, entity, "2026-07-12", TEST_WIDGET_IDS) { ids, views ->
            capturedIds = ids
            capturedViews = views
        }

        assertEquals(ListenableWorker.Result.success(), result)
        assertNotNull("Must create RemoteViews", capturedViews)
        assertEquals("Must update 1 widget", 1, capturedIds!!.size)
        assertEquals("Must use mi_negocio layout", R.layout.widget_mi_negocio, capturedViews!!.layoutId)

        // Verify text values derived from entity:
        // ventasMontoTotal=2500.0 → "Hoy: S/ 2500.00"
        // saldoPendienteTotal=800.0 → "Por cobrar: S/ 800.00"
        assertNotNull("ventasMontoTotal must be present", entity.ventasMontoTotal)
        assertNotNull("saldoPendienteTotal must be present", entity.saldoPendienteTotal)
        assertEquals(2500.0, entity.ventasMontoTotal!!, 0.001)
        assertEquals(800.0, entity.saldoPendienteTotal!!, 0.001)
    }

    @Test
    fun doWorkCore_withNullEntity_showsZeros() {
        val context = RuntimeEnvironment.getApplication()

        var capturedViews: RemoteViews? = null
        val result = doWorkCore(context, null, "2026-07-12", TEST_WIDGET_IDS) { _, views ->
            capturedViews = views
        }

        assertEquals(ListenableWorker.Result.success(), result)
        assertNotNull("Must create RemoteViews for null entity", capturedViews)
        assertEquals("Must use mi_negocio layout", R.layout.widget_mi_negocio, capturedViews!!.layoutId)

        // Null entity → ventas=0.0, porCobrar=0.0 via Elvis operator
        // Text formatted as "Hoy: S/ 0.00" and "Por cobrar: S/ 0.00"
    }
}
