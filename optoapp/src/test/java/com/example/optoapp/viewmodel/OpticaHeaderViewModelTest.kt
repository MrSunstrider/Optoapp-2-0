package com.example.optoapp.viewmodel

import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaFiscalSettingsStore
import com.example.optoapp.data.OpticaHeaderSummary
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.opticasettings.OpticaSettingsDao
import com.example.optoapp.data.opticasettings.OpticaSettingsEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpticaHeaderViewModelTest {

    private lateinit var viewModel: OpticaHeaderViewModel
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val membershipRepository = mockk<MembershipRepository>(relaxed = true)
    private val fiscalStore = mockk<OpticaFiscalSettingsStore>(relaxed = true)
    private val opticaSettingsDao = mockk<OpticaSettingsDao>(relaxed = true)
    private val opticaIdFlow = MutableStateFlow("test-optica")

    @Before
    fun setUp() {
        every { sessionManager.opticaId } returns opticaIdFlow
        // Default: OpticaHeaderViewModel init calls membershipRepository.fetchOpticaHeaderSummary
        // which returns null to fall through to fiscalStore
        coEvery { membershipRepository.fetchOpticaHeaderSummary(any()) } returns null
        every { fiscalStore.settingsFlow(any()) } returns MutableStateFlow(
            com.example.optoapp.data.OpticaFiscalSettings(
                nombreComercial = "Test Optica",
                docTipo = "RUC",
                docNumero = "12345678901",
            ),
        )
    }

    @Test
    fun `uiState includes horarioAtencion when optica_settings has business_hours`() = runBlocking {
        // Given: DAO returns settings with business_hours
        val settingsEntity = OpticaSettingsEntity(
            opticaId = "test-optica",
            configJson = """{"business_hours": "Lunes a Viernes de 9am a 7pm"}""",
        )
        val settingsFlow = MutableStateFlow(settingsEntity)
        every { opticaSettingsDao.getByOpticaId(any()) } returns settingsFlow

        // When: ViewModel is created
        viewModel = OpticaHeaderViewModel(
            sessionManager = sessionManager,
            membershipRepository = membershipRepository,
            fiscalStore = fiscalStore,
            opticaSettingsDao = opticaSettingsDao,
        )

        // Then: uiState reflects horarioAtencion from business_hours
        val state = viewModel.uiState.first { it.horarioAtencion.isNotEmpty() }
        assertEquals("Lunes a Viernes de 9am a 7pm", state.horarioAtencion)
        assertEquals("Test Optica", state.nombreOptica)
    }

    @Test
    fun `uiState horarioAtencion is blank when optica_settings has no business_hours`() = runBlocking {
        // Given: DAO returns settings without business_hours in configJson
        val settingsEntity = OpticaSettingsEntity(
            opticaId = "test-optica",
            configJson = """{}""",
        )
        val settingsFlow = MutableStateFlow(settingsEntity)
        every { opticaSettingsDao.getByOpticaId(any()) } returns settingsFlow

        // When: ViewModel is created
        viewModel = OpticaHeaderViewModel(
            sessionManager = sessionManager,
            membershipRepository = membershipRepository,
            fiscalStore = fiscalStore,
            opticaSettingsDao = opticaSettingsDao,
        )

        // Then: horarioAtencion is blank
        val state = viewModel.uiState.first()
        assertEquals("", state.horarioAtencion)
        assertEquals("Test Optica", state.nombreOptica)
    }

    @Test
    fun `uiState horarioAtencion is blank when optica_settings dao returns null`() = runBlocking {
        // Given: DAO returns null
        val settingsFlow = MutableStateFlow<OpticaSettingsEntity?>(null)
        every { opticaSettingsDao.getByOpticaId(any()) } returns settingsFlow

        // When: ViewModel is created
        viewModel = OpticaHeaderViewModel(
            sessionManager = sessionManager,
            membershipRepository = membershipRepository,
            fiscalStore = fiscalStore,
            opticaSettingsDao = opticaSettingsDao,
        )

        // Then: horarioAtencion is blank
        val state = viewModel.uiState.first()
        assertEquals("", state.horarioAtencion)
        assertEquals("Test Optica", state.nombreOptica)
    }
}
