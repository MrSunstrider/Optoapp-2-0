package com.example.optoapp.viewmodel

import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.serviciotipo.ServicioTipoDao
import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import com.example.optoapp.domain.ServicioTipoCatalogUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServicioTiposViewModelTest {

    private val opticaId = "o1"
    private val testDispatcher = UnconfinedTestDispatcher()
    private val catalogo = MutableStateFlow<List<ServicioTipoEntity>>(emptyList())
    private val dao = mockk<ServicioTipoDao>()
    private val catalogUseCase = mockk<ServicioTipoCatalogUseCase>(relaxed = true)
    private val scheduler = mockk<PostSaveSyncScheduler>(relaxed = true)
    private val sessionManager = mockk<SessionManager>()

    private val biselado = ServicioTipoEntity(
        id = "t1", opticaId = opticaId, nombre = "Biselado", costoPropio = 5.0, costoTercero = 12.0, updatedBy = "u1",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { dao.observeByOptica(opticaId) } returns catalogo
        every { sessionManager.opticaId } returns MutableStateFlow(opticaId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(rol: String = "admin"): ServicioTiposViewModel {
        every { sessionManager.opticaRol } returns flowOf(rol)
        return ServicioTiposViewModel(dao, catalogUseCase, sessionManager, scheduler)
    }

    @Test
    fun creatingATypeSavesItAndSchedulesFinanzasSync() = runTest(testDispatcher) {
        val vm = buildViewModel()
        val guardado = slot<ServicioTipoEntity>()
        vm.nuevo()
        vm.cambiarForm { it.copy(nombre = " Calibrado ", costoPropio = "4,50", costoTercero = "") }

        vm.guardar()

        coVerify { catalogUseCase.guardar(capture(guardado)) }
        assertEquals("Calibrado", guardado.captured.nombre)
        assertEquals(opticaId, guardado.captured.opticaId)
        assertEquals(4.5, guardado.captured.costoPropio, 0.0)
        assertEquals(0.0, guardado.captured.costoTercero, 0.0)
        assertTrue(guardado.captured.activo)
        verify { scheduler.scheduleFinanzasSync(opticaId) }
        assertNull(vm.uiState.value.form)
    }

    @Test
    fun theCatalogListsEveryTypeIncludingInactiveOnes() = runTest(testDispatcher) {
        val vm = buildViewModel()

        catalogo.value = listOf(biselado, biselado.copy(id = "t2", nombre = "Tallado", activo = false))

        assertEquals(listOf("t1", "t2"), vm.uiState.value.tipos.map { it.id })
    }

    @Test
    fun editingKeepsTheIdAndAuthorOfTheType() = runTest(testDispatcher) {
        val vm = buildViewModel()
        val guardado = slot<ServicioTipoEntity>()
        vm.editar(biselado)
        vm.cambiarForm { it.copy(costoTercero = "15") }

        vm.guardar()

        coVerify { catalogUseCase.guardar(capture(guardado)) }
        assertEquals(biselado.copy(costoTercero = 15.0), guardado.captured)
    }

    @Test
    fun deactivatingAndReactivatingGoThroughTheCatalogAndSync() = runTest(testDispatcher) {
        val vm = buildViewModel()

        vm.desactivar(biselado)
        vm.reactivar(biselado)

        coVerify { catalogUseCase.desactivar("t1", opticaId) }
        coVerify { catalogUseCase.reactivar("t1", opticaId) }
        verify(exactly = 2) { scheduler.scheduleFinanzasSync(opticaId) }
    }

    @Test
    fun aBlankNameOrNegativeCostShowsFieldErrorsAndSavesNothing() = runTest(testDispatcher) {
        val vm = buildViewModel()
        vm.nuevo()
        vm.cambiarForm { it.copy(nombre = "  ", costoPropio = "-1") }

        vm.guardar()

        val form = vm.uiState.value.form
        assertNotNull(form?.nombreError)
        assertNotNull(form?.costoError)
        coVerify(exactly = 0) { catalogUseCase.guardar(any()) }
        verify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
    }

    @Test
    fun anAsesorSeesTheCatalogReadOnly() = runTest(testDispatcher) {
        val vm = buildViewModel(rol = "asesor")

        vm.nuevo()
        vm.desactivar(biselado)

        assertFalse(vm.uiState.value.puedeEditar)
        assertNull(vm.uiState.value.form)
        coVerify(exactly = 0) { catalogUseCase.desactivar(any(), any()) }
    }

    @Test
    fun anEmptyCatalogOffersTheCreateCallToActionOnlyToEditors() = runTest(testDispatcher) {
        assertTrue(buildViewModel().uiState.value.muestraCrearCta)
        assertFalse(buildViewModel(rol = "asesor").uiState.value.muestraCrearCta)
    }

    @Test
    fun theViewModelOffersNoDeleteAction() {
        val names = ServicioTiposViewModel::class.java.declaredMethods.map { it.name.lowercase() }

        assertTrue(names.none { "delete" in it || "eliminar" in it || "borrar" in it })
    }
}
