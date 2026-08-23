package com.example.optoapp.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.util.DateUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CierreCajaCashTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor
    private val today = LocalDate.of(2026, 8, 23)
    private val opticaId = "optica-1"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        mockkObject(DateUtils)
        every { DateUtils.today() } returns today
        repository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { sessionManager.opticaRol } returns flowOf("admin")
        every { repository.getPagosByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.getDispensacionesByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.getServiciosByDateRangeForOptica(any(), any(), any()) } returns flowOf(emptyList())
        every { repository.pacientesFlowForOptica(any()) } returns flowOf(emptyList())

        prefsEditor = mockk(relaxed = true)
        every { prefsEditor.putString(any(), any()) } returns prefsEditor
        every { prefsEditor.remove(any()) } returns prefsEditor
        every { prefsEditor.apply() } returns Unit
        prefs = mockk(relaxed = true)
        every { prefs.edit() } returns prefsEditor
        every { prefs.getString(any(), any()) } returns null
        appContext = mockk(relaxed = true)
        every { appContext.getSharedPreferences("optoapp_prefs", Context.MODE_PRIVATE) } returns prefs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun diferencia_contadoMinusEfectivoPagoEffect_isNegativeTen() {
        val efectivoNet = 150.0
        val contado = 140.0
        assertEquals(-10.0, CierreCajaCashMath.diferencia(contado, efectivoNet)!!, 0.0)
    }

    @Test
    fun diferencia_emptyContado_returnsNull() {
        assertNull(CierreCajaCashMath.diferencia(null, 150.0))
    }

    @Test
    fun prefsKey_usesOpticaIdAndFecha() {
        assertEquals(
            "cierre_contado_optica-1_2026-08-23",
            CierreCajaCashMath.prefsKey(opticaId, today),
        )
    }

    @Test
    fun viewModel_setContado_persistsPrefsAndExposesDiferencia() = runTest(testDispatcher) {
        val pagos = listOf(
            Pago(
                id = "p1",
                fecha = today,
                tipo = "Abono",
                monto = 150.0,
                metodoPago = "Efectivo",
                opticaId = opticaId,
            ),
        )
        every { repository.getPagosByDateRangeForOptica(today, today, opticaId) } returns flowOf(pagos)

        val vm = CierreCajaViewModel(repository, sessionManager, SavedStateHandle(), appContext)
        vm.uiState.first { !it.isLoading }
        assertEquals(150.0, vm.getTotalesPorMetodo()["Efectivo"]!!, 0.0)
        assertEquals(150.0, PagoEffect.signedAmount("Abono", 150.0), 0.0)

        vm.setContado(140.0)
        val state = vm.uiState.value
        assertEquals(140.0, state.contadoEfectivo!!, 0.0)
        assertEquals(-10.0, state.diferenciaEfectivo!!, 0.0)
        verify {
            prefsEditor.putString("cierre_contado_optica-1_2026-08-23", "140.0")
            prefsEditor.apply()
        }
    }

    @Test
    fun viewModel_clearContado_hidesDiferenciaAndRemovesPref() = runTest(testDispatcher) {
        val vm = CierreCajaViewModel(repository, sessionManager, SavedStateHandle(), appContext)
        vm.uiState.first { !it.isLoading }
        vm.setContado(100.0)
        vm.setContado(null)
        assertNull(vm.uiState.value.contadoEfectivo)
        assertNull(vm.uiState.value.diferenciaEfectivo)
        verify { prefsEditor.remove("cierre_contado_optica-1_2026-08-23") }
    }

    @Test
    fun noArqueoCajaTable_referencedInCashMath() {
        // Guard: counted cash lives in prefs/session only — never arqueo_caja.
        assertTrue(CierreCajaCashMath.prefsKey("x", today).startsWith("cierre_contado_"))
        assertTrue(!CierreCajaCashMath.prefsKey(opticaId, today).contains("arqueo"))
    }
}
