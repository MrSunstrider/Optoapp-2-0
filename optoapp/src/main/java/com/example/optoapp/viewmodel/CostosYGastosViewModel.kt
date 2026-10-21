package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.domain.AltoIndiceNormalizer
import com.example.optoapp.domain.GastoTipoResolver
import com.example.optoapp.domain.MatrizVigenciaUseCase
import com.example.optoapp.domain.OpticalCatalog
import com.example.optoapp.domain.ResultadoVigencia
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.ui.navigation.CostosTab
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.MesArg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

val COST_BLOCKS = listOf(
    "Stock Monofocal",
    "Stock Bifocal",
    "Stock Multifocal",
    "Fabricación Resina",
    "Fabricación Cristal",
)

/** Stock|fab key only — prefer [blockFilter] when tipo/material matter. */
fun blockToFilter(block: String): String = blockFilter(block).stockOFabricacion

data class CostBlockFilter(
    val stockOFabricacion: String,
    val tipoLente: String? = null,
    /** Substring match on material (e.g. Resina / Cristal families). */
    val materialContains: String? = null,
)

fun blockFilter(block: String): CostBlockFilter = when (block) {
    "Stock Monofocal" -> CostBlockFilter("stock", tipoLente = "Monofocal")
    "Stock Bifocal" -> CostBlockFilter("stock", tipoLente = "Bifocal")
    "Stock Multifocal" -> CostBlockFilter("stock", tipoLente = "Multifocal")
    "Fabricación Resina" -> CostBlockFilter("fabricacion", materialContains = "Resina")
    "Fabricación Cristal" -> CostBlockFilter("fabricacion", materialContains = "Cristal")
    else -> CostBlockFilter(block)
}

fun aplicarFiltroBloque(
    costos: List<CostoProductoEntity>,
    filter: CostBlockFilter,
): List<CostoProductoEntity> = costos.filter {
    (filter.tipoLente == null || it.tipoLente.equals(filter.tipoLente, ignoreCase = true)) &&
        (filter.materialContains == null || it.material.contains(filter.materialContains, ignoreCase = true))
}

val MATRIZ_BLOQUES_EDITABLES = COST_BLOCKS + listOf("Biselado", "Lentes de Contacto")

// WHY: Codes mirror the server CHECK constraints and the cost lookup keys, so a free-text value would never be priced.
val TIPOS_ARO_BISELADO = listOf("aro_completo", "semi_aire", "al_aire")
val MODALIDADES_BISELADO = listOf("stock", "fabricacion")
/** Matches CalcularCostoItemUseCase.biseladoMaterialKey outputs. */
val MATERIALES_BISELADO = listOf("Resina", "Cristal", "Policarbonato", "Trivex")
val TIPOS_LC = listOf("cosmetico", "graduado", "terapeutico")
val MODALIDADES_LC = listOf("diario", "quincenal", "mensual", "anual")
val INDICES_REFRACCION_MATRIZ = listOf("1.56", "1.60", "1.67", "1.70", "1.74", "1.80")
val CURVAS_BASE_MATRIZ = listOf("4", "6", "8")

fun filtrarCostos(
    costos: List<CostoProductoEntity>,
    material: String?,
    tipoLente: String?,
    serie: Int?,
): List<CostoProductoEntity> = costos.filter {
    (material == null || it.material == material) &&
        (tipoLente == null || it.tipoLente == tipoLente) &&
        (serie == null || it.serie == serie)
}

data class VersionPrecio(val id: String, val costo: Double, val vigenteDesde: String, val vigenteHasta: String?)

data class FormBiselado(
    val material: String = "",
    val tipoAro: String = "",
    val modalidad: String = "",
    val serie: String = "",
    val altoIndice: String = "",
    val costo: String = "",
    val error: String? = null,
)

data class FormLc(
    val tipoLc: String = "",
    val material: String = "",
    val modalidad: String = "",
    val costo: String = "",
    val error: String? = null,
)

data class GastoFila(val gasto: GastoOperativoEntity, val tipo: String)

data class GrupoGastos(
    val categoria: String,
    val etiqueta: String,
    val subtotal: BigDecimal,
    val filas: List<GastoFila>,
)

fun agruparGastosDelMes(gastos: List<GastoOperativoEntity>, mes: YearMonth): List<GrupoGastos> =
    gastos
        .filter { YearMonth.from(it.fecha) == mes }
        .groupBy { it.categoria }
        .map { (categoria, delGrupo) ->
            GrupoGastos(
                categoria = categoria,
                etiqueta = categoria.replaceFirstChar { it.titlecase(Locale.ROOT) },
                subtotal = delGrupo.fold(BigDecimal.ZERO) { acc, g -> acc + g.monto },
                filas = delGrupo
                    .sortedByDescending { it.fecha }
                    .map { GastoFila(it, GastoTipoResolver.resolver(it.categoria, it.tipoGasto)) },
            )
        }
        .sortedWith(compareByDescending<GrupoGastos> { it.subtotal }.thenBy { it.categoria })

data class CostosYGastosUiState(
    val selectedTab: Int = 0,
    val selectedBlock: String? = null,
    val costosDelBloque: List<CostoProductoEntity> = emptyList(),
    val filtroMaterial: String? = null,
    val filtroTipoLente: String? = null,
    val filtroSerie: Int? = null,
    val costosBiselado: List<CostoBiseladoEntity> = emptyList(),
    val costosLc: List<CostoLcEntity> = emptyList(),
    val puedeEditarMatrices: Boolean = false,
    val formBiselado: FormBiselado? = null,
    val formLc: FormLc? = null,
    val editingCosto: CostoProductoEntity? = null,
    val editingBiselado: CostoBiseladoEntity? = null,
    val editingLc: CostoLcEntity? = null,
    val nuevoCostoUnitario: String = "",
    val vigenteDesde: LocalDate? = null,
    val vigenteDesdeError: String? = null,
    val historialPrecios: List<VersionPrecio> = emptyList(),
    val gastosOperativos: List<GastoOperativoEntity> = emptyList(),
    val mesGastos: YearMonth = YearMonth.from(DateUtils.today()),
    val gruposGastos: List<GrupoGastos> = emptyList(),
    val totalGastosMes: BigDecimal = BigDecimal.ZERO,
    val puedeEditarGastos: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDialogVisible: Boolean = false,
    val editingGasto: GastoOperativoEntity? = null,
    val categoria: String = "alquiler",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: LocalDate = DateUtils.today(),
    val nota: String = "",
    val isRecurring: Boolean = false,
    val tipoGasto: String = GastoTipoResolver.FIJO,
    val tipoGastoElegido: Boolean = false,
    val gastoFormError: String? = null,
    val deletingGasto: GastoOperativoEntity? = null,
    val gastoDeleteError: String? = null,
    val isCostoDialogVisible: Boolean = false,
    val creatingCosto: Boolean = false,
    val costoMaterial: String = "",
    val costoTipoLente: String = "",
    val costoStockOFabricacion: String = "",
    val costoTratamiento: String = "",
    val costoSerie: String = "",
    val costoSubtipoBifocal: String = "",
    val costoIndiceRefraccion: String = "",
    val costoCurvaBase: String = "",
    val costoCostoUnitario: String = "",
    val costoSaveError: String? = null,
    val deletingCosto: CostoProductoEntity? = null,
) {
    val costosVisibles: List<CostoProductoEntity>
        get() = filtrarCostos(costosDelBloque, filtroMaterial, filtroTipoLente, filtroSerie)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CostosYGastosViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val costoProductoDao: CostoProductoDao,
    private val costoBiseladoDao: CostoBiseladoDao,
    private val costoLcDao: CostoLcDao,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val syncFinanzasUseCase: SyncFinanzasUseCase,
    private val syncStateTracker: SyncStateTracker,
    private val matrizVigenciaUseCase: MatrizVigenciaUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tabInicial = CostosTab.fromArg(savedStateHandle.get<String>(CostosTab.KEY))

    private val _uiState = MutableStateFlow(
        CostosYGastosUiState(
            selectedTab = tabInicial.ordinal,
            mesGastos = MesArg.parseOrCurrent(savedStateHandle.get<String>(MesArg.KEY), YearMonth.from(DateUtils.today())),
        ),
    )
    val uiState: StateFlow<CostosYGastosUiState> = _uiState.asStateFlow()

    private var syncTriggered = false

    companion object {
        private const val TAG = "CostosYGastosVM"
        private val FECHA_UI = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private const val TIPO_BISELADO = "costo_biselado"
        private const val TIPO_LC = "costo_lc"
        private const val PRECIO_DUPLICADO = "Ya existe un precio vigente para esta combinación. Ábralo en la lista para editarlo."
        val CATEGORIAS = listOf("alquiler", "servicios", "personal", "proveedores", "insumos", "marketing", "impuestos", "otro")

        fun autoGenerarRecurrentes(
            templates: List<GastoOperativoEntity>,
            existentes: List<GastoOperativoEntity>,
            mesActual: LocalDate,
        ): List<GastoOperativoEntity> {
            val mesInicio = mesActual.withDayOfMonth(1)
            val mesFin = mesActual.withDayOfMonth(mesActual.lengthOfMonth())
            return templates
                .filter { it.isRecurring }
                .filter { template ->
                    existentes.none { existente ->
                        existente.categoria == template.categoria &&
                            !existente.fecha.isBefore(mesInicio) &&
                            !existente.fecha.isAfter(mesFin)
                    }
                }
                .map { template ->
                    template.copy(
                        id = UUID.randomUUID().toString(),
                        fecha = mesInicio,
                        isRecurring = false,
                        nota = "Auto-generado de ${template.categoria}",
                    )
                }
        }
    }

    init {
        viewModelScope.launch {
            sessionManager.opticaId.flatMapLatest { repository.getGastosOperativos(it) }
                .catch { e ->
                    Log.e(TAG, "Gastos flow crashed, restarting", e)
                    emit(emptyList())
                }
                .collect { gastos ->
                    val visibles = try {
                        autoGenerarSiFalta(gastos)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "autoGenerarSiFalta failed, showing raw gastos", e)
                        gastos
                    }
                    _uiState.update { conGastosDelMes(it.copy(gastosOperativos = visibles.sortedByDescending { g -> g.fecha })) }
                    if (!syncTriggered && gastos.isEmpty()) {
                        syncTriggered = true
                        val opticaId = sessionManager.opticaId.first()
                        Log.d(TAG, "Triggering finanzas download for gastos (opticaId=$opticaId)")
                        viewModelScope.launch {
                            syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = true)
                        }
                    }
                }
        }
        viewModelScope.launch {
            sessionManager.opticaRol
                .catch { e ->
                    Log.e(TAG, "Role flow crashed, gastos stay read-only", e)
                    emit("")
                }
                .collect { rol ->
                    val gestion = AppRoles.canWriteGastos(rol)
                    _uiState.update { it.copy(puedeEditarGastos = gestion, puedeEditarMatrices = gestion) }
                }
        }
        selectTab(tabInicial.ordinal)
    }

    private fun conGastosDelMes(state: CostosYGastosUiState): CostosYGastosUiState {
        val grupos = agruparGastosDelMes(state.gastosOperativos, state.mesGastos)
        return state.copy(
            gruposGastos = grupos,
            totalGastosMes = grupos.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotal },
        )
    }

    fun selectMesGastos(mes: YearMonth) {
        _uiState.update { conGastosDelMes(it.copy(mesGastos = mes)) }
        viewModelScope.launch {
            try {
                val actuales = _uiState.value.gastosOperativos
                val ampliados = autoGenerarSiFalta(actuales, mes.atDay(1))
                if (ampliados !== actuales) {
                    _uiState.update { conGastosDelMes(it.copy(gastosOperativos = ampliados.sortedByDescending { g -> g.fecha })) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "selectMesGastos auto-generate failed", e)
            }
        }
    }

    private suspend fun autoGenerarSiFalta(
        gastos: List<GastoOperativoEntity>,
        mesReferencia: LocalDate = _uiState.value.mesGastos.atDay(1),
    ): List<GastoOperativoEntity> {
        val nuevos = autoGenerarRecurrentes(
            templates = gastos.filter { it.isRecurring },
            existentes = gastos,
            mesActual = mesReferencia,
        )
        if (nuevos.isEmpty()) return gastos
        val opticaId = sessionManager.opticaId.first()
        nuevos.forEach { repository.upsertGastoOperativo(it) }
        postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
        return gastos + nuevos
    }

    fun refreshGastos() {
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                syncFinanzasUseCase(opticaId, downloadAfterUpload = true, skipUpload = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "refreshGastos failed", e)
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        when (index) {
            CostosTab.BISELADO.ordinal -> cargarMatriz("biselado", ::refrescarBiselado)
            CostosTab.LC.ordinal -> cargarMatriz("lentes de contacto", ::refrescarLc)
        }
    }

    private fun cargarMatriz(nombre: String, refrescar: suspend (String) -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                refrescar(sessionManager.opticaId.first())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Loading the $nombre matrix failed", e)
                _uiState.update { it.copy(error = "No se pudo cargar la matriz de $nombre.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun refrescarBiselado(opticaId: String) {
        val filas = costoBiseladoDao.getByOpticaIdList(opticaId)
        _uiState.update { it.copy(costosBiselado = filas) }
    }

    private suspend fun refrescarLc(opticaId: String) {
        val filas = costoLcDao.getByOpticaIdList(opticaId)
        _uiState.update { it.copy(costosLc = filas) }
    }

    fun updateFiltroMaterial(material: String?) {
        _uiState.update { it.copy(filtroMaterial = material) }
    }

    fun updateFiltroTipoLente(tipoLente: String?) {
        _uiState.update { it.copy(filtroTipoLente = tipoLente) }
    }

    fun updateFiltroSerie(serie: Int?) {
        _uiState.update { it.copy(filtroSerie = serie) }
    }

    fun loadBlock(block: String) {
        val filter = blockFilter(block)
        _uiState.update {
            it.copy(
                selectedBlock = block,
                isLoading = true,
                filtroMaterial = null,
                filtroTipoLente = filter.tipoLente,
                filtroSerie = null,
            )
        }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val raw = costoProductoDao.getByBloque(opticaId, filter.stockOFabricacion).first()
                val costos = aplicarFiltroBloque(raw, filter)
                _uiState.update { it.copy(isLoading = false, costosDelBloque = costos) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading block $block", e)
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar bloque: ${e.message}") }
            }
        }
    }

    fun showEditCosto(costo: CostoProductoEntity) {
        if (!_uiState.value.puedeEditarMatrices) return
        abrirEdicion(costo.vigenteDesde, costo.costoUnitario) { it.copy(editingCosto = costo) }
        cargarHistorial({ it.editingCosto?.id == costo.id }) {
            costoProductoDao.getHistorial(
                costo.opticaId, costo.material, costo.tipoLente, costo.stockOFabricacion,
                costo.tratamiento, costo.serie, costo.laboratorioId,
                costo.subtipoBifocal, costo.indiceRefraccion, costo.curvaBase,
            ).map { VersionPrecio(it.id, it.costoUnitario, it.vigenteDesde, it.vigenteHasta) }
        }
    }

    fun showEditBiselado(fila: CostoBiseladoEntity) {
        if (!_uiState.value.puedeEditarMatrices) return
        abrirEdicion(fila.vigenteDesde, fila.costoPorPar) { it.copy(editingBiselado = fila) }
        cargarHistorial({ it.editingBiselado?.id == fila.id }) {
            costoBiseladoDao.getHistorial(
                fila.opticaId, fila.material, fila.tipoAro, fila.stockOFabricacion, fila.serie, fila.altoIndice,
            ).map { VersionPrecio(it.id, it.costoPorPar, it.vigenteDesde, it.vigenteHasta) }
        }
    }

    fun showEditLc(fila: CostoLcEntity) {
        if (!_uiState.value.puedeEditarMatrices) return
        abrirEdicion(fila.vigenteDesde, fila.costoUnitario) { it.copy(editingLc = fila) }
        cargarHistorial({ it.editingLc?.id == fila.id }) {
            costoLcDao.getHistorial(fila.opticaId, fila.tipoLc, fila.materialLc, fila.modalidad, fila.laboratorioId)
                .map { VersionPrecio(it.id, it.costoUnitario, it.vigenteDesde, it.vigenteHasta) }
        }
    }

    private fun abrirEdicion(
        vigenteDesde: String,
        costoActual: Double,
        seleccionar: (CostosYGastosUiState) -> CostosYGastosUiState,
    ) {
        _uiState.update {
            seleccionar(
                it.copy(
                    nuevoCostoUnitario = costoActual.toString(),
                    vigenteDesde = MatrizVigenciaUseCase.fechaMinima(vigenteDesde, DateUtils.today()),
                    vigenteDesdeError = null,
                    historialPrecios = emptyList(),
                    error = null,
                ),
            )
        }
    }

    // WHY: The dialog may be closed or switched to another row before the query returns; a late result must not leak into it.
    private fun cargarHistorial(sigueAbierta: (CostosYGastosUiState) -> Boolean, leer: suspend () -> List<VersionPrecio>) {
        viewModelScope.launch {
            try {
                val historial = leer()
                _uiState.update { if (sigueAbierta(it)) it.copy(historialPrecios = historial) else it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Loading price history failed", e)
            }
        }
    }

    fun dismissEditPrecio() {
        _uiState.update {
            it.copy(
                editingCosto = null,
                editingBiselado = null,
                editingLc = null,
                nuevoCostoUnitario = "",
                vigenteDesde = null,
                vigenteDesdeError = null,
                historialPrecios = emptyList(),
            )
        }
    }

    fun updateNuevoCostoUnitario(value: String) {
        _uiState.update { it.copy(nuevoCostoUnitario = value) }
    }

    fun updateVigenteDesde(fecha: LocalDate) {
        _uiState.update { it.copy(vigenteDesde = fecha, vigenteDesdeError = null) }
    }

    fun saveCostoEdit() {
        val costo = _uiState.value.editingCosto ?: return
        guardarVersion(::refrescarBloque) { valor, desde ->
            matrizVigenciaUseCase.editarProducto(costo, valor, desde, DateUtils.today())
        }
    }

    fun saveBiseladoEdit() {
        val fila = _uiState.value.editingBiselado ?: return
        guardarVersion(::refrescarBiselado) { valor, desde ->
            matrizVigenciaUseCase.editarBiselado(fila, valor, desde, DateUtils.today())
        }
    }

    fun saveLcEdit() {
        val fila = _uiState.value.editingLc ?: return
        guardarVersion(::refrescarLc) { valor, desde ->
            matrizVigenciaUseCase.editarLc(fila, valor, desde, DateUtils.today())
        }
    }

    private fun guardarVersion(
        refrescar: suspend (String) -> Unit,
        editar: suspend (valor: Double, desde: LocalDate) -> ResultadoVigencia,
    ) {
        val s = _uiState.value
        if (!s.puedeEditarMatrices) return
        val desde = s.vigenteDesde ?: return
        val nuevoValor = costoPositivo(s.nuevoCostoUnitario)
        if (nuevoValor == null) {
            _uiState.update { it.copy(error = "Ingresa un costo válido") }
            return
        }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                when (val resultado = editar(nuevoValor, desde)) {
                    is ResultadoVigencia.Rechazado -> _uiState.update {
                        it.copy(vigenteDesdeError = "La fecha debe ser el ${resultado.fechaMinima.format(FECHA_UI)} o posterior")
                    }
                    is ResultadoVigencia.Aplicado -> {
                        postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
                        dismissEditPrecio()
                        refrescar(opticaId)
                    }
                    ResultadoVigencia.YaNoVigente -> {
                        dismissEditPrecio()
                        _uiState.update { it.copy(error = "Este precio ya no está vigente. Revisa la lista actualizada.") }
                        refrescar(opticaId)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Saving a price version failed", e)
                _uiState.update { it.copy(error = "No se pudo guardar el precio. Inténtalo de nuevo.") }
            }
        }
    }

    private fun costoPositivo(raw: String): Double? = raw.trim().toDoubleOrNull()?.takeIf { it > 0 }

    private suspend fun refrescarBloque(opticaId: String) {
        val bloque = _uiState.value.selectedBlock ?: return
        val filter = blockFilter(bloque)
        val refreshed = aplicarFiltroBloque(
            costoProductoDao.getByBloque(opticaId, filter.stockOFabricacion).first(),
            filter,
        )
        _uiState.update { it.copy(costosDelBloque = refreshed) }
    }

    fun showNewBiselado() {
        if (!_uiState.value.puedeEditarMatrices) return
        _uiState.update { it.copy(formBiselado = FormBiselado()) }
    }

    fun updateFormBiselado(cambio: (FormBiselado) -> FormBiselado) {
        _uiState.update { s -> s.formBiselado?.let { s.copy(formBiselado = cambio(it).copy(error = null)) } ?: s }
    }

    fun dismissFormBiselado() {
        _uiState.update { it.copy(formBiselado = null) }
    }

    fun saveBiselado() {
        val s = _uiState.value
        val form = s.formBiselado ?: return
        if (!s.puedeEditarMatrices) return
        val error = errorFormBiselado(form, s.costosBiselado)
        if (error != null) {
            _uiState.update { it.copy(formBiselado = form.copy(error = error)) }
            return
        }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val fila = CostoBiseladoEntity(
                    id = UUID.randomUUID().toString(),
                    opticaId = opticaId,
                    material = form.material,
                    tipoAro = form.tipoAro,
                    stockOFabricacion = form.modalidad,
                    serie = form.serie.trim().toIntOrNull(),
                    altoIndice = AltoIndiceNormalizer.normalize(form.altoIndice),
                    costoPorPar = form.costo.trim().toDouble(),
                    vigenteDesde = DateUtils.toIso(DateUtils.today()),
                    updatedAt = Instant.now().toString(),
                )
                repository.withTransaction {
                    costoBiseladoDao.upsertAll(listOf(fila))
                    syncStateTracker.markLocallyModified(opticaId, TIPO_BISELADO, fila.id)
                }
                _uiState.update { it.copy(formBiselado = null) }
                refrescarBiselado(opticaId)
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "saveBiselado failed", e)
                _uiState.update { it.copy(formBiselado = it.formBiselado?.copy(error = "No se pudo guardar el precio. Inténtalo de nuevo.")) }
            }
        }
    }

    // WHY: A second open row for the same combination would make the cost lookup ambiguous; edits must version the existing one.
    private fun errorFormBiselado(form: FormBiselado, abiertas: List<CostoBiseladoEntity>): String? {
        val serie = form.serie.trim()
        val altoIndice = AltoIndiceNormalizer.normalize(form.altoIndice)
        return when {
            form.material.isBlank() -> "Selecciona el material"
            form.tipoAro !in TIPOS_ARO_BISELADO -> "Selecciona el tipo de aro"
            form.modalidad !in MODALIDADES_BISELADO -> "Selecciona stock o fabricación"
            serie.isNotEmpty() && serie.toIntOrNull() == null -> "La serie debe ser un número entero"
            costoPositivo(form.costo) == null -> "Ingresa un costo válido"
            abiertas.any {
                it.material == form.material && it.tipoAro == form.tipoAro && it.stockOFabricacion == form.modalidad &&
                    it.serie == serie.toIntOrNull() && it.altoIndice == altoIndice
            } -> PRECIO_DUPLICADO
            else -> null
        }
    }

    fun showNewLc() {
        if (!_uiState.value.puedeEditarMatrices) return
        _uiState.update { it.copy(formLc = FormLc()) }
    }

    fun updateFormLc(cambio: (FormLc) -> FormLc) {
        _uiState.update { s -> s.formLc?.let { s.copy(formLc = cambio(it).copy(error = null)) } ?: s }
    }

    fun dismissFormLc() {
        _uiState.update { it.copy(formLc = null) }
    }

    fun saveLc() {
        val s = _uiState.value
        val form = s.formLc ?: return
        if (!s.puedeEditarMatrices) return
        val error = errorFormLc(form, s.costosLc)
        if (error != null) {
            _uiState.update { it.copy(formLc = form.copy(error = error)) }
            return
        }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val fila = CostoLcEntity(
                    id = UUID.randomUUID().toString(),
                    opticaId = opticaId,
                    tipoLc = form.tipoLc,
                    materialLc = form.material.trim(),
                    modalidad = form.modalidad,
                    costoUnitario = form.costo.trim().toDouble(),
                    vigenteDesde = DateUtils.toIso(DateUtils.today()),
                    updatedAt = Instant.now().toString(),
                )
                repository.withTransaction {
                    costoLcDao.upsertAll(listOf(fila))
                    syncStateTracker.markLocallyModified(opticaId, TIPO_LC, fila.id)
                }
                _uiState.update { it.copy(formLc = null) }
                refrescarLc(opticaId)
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "saveLc failed", e)
                _uiState.update { it.copy(formLc = it.formLc?.copy(error = "No se pudo guardar el precio. Inténtalo de nuevo.")) }
            }
        }
    }

    private fun errorFormLc(form: FormLc, abiertas: List<CostoLcEntity>): String? = when {
        form.tipoLc !in TIPOS_LC -> "Selecciona el tipo de lente de contacto"
        form.material.isBlank() -> "Ingresa el material"
        form.modalidad !in MODALIDADES_LC -> "Selecciona la modalidad"
        costoPositivo(form.costo) == null -> "Ingresa un costo válido"
        abiertas.any {
            it.tipoLc == form.tipoLc && it.materialLc == form.material.trim() && it.modalidad == form.modalidad &&
                it.laboratorioId == null
        } -> PRECIO_DUPLICADO
        else -> null
    }

    val categorias = CATEGORIAS

    fun showNewGasto() {
        if (!_uiState.value.puedeEditarGastos) return
        val categoria = CATEGORIAS.first()
        _uiState.update {
            it.copy(
                isDialogVisible = true,
                editingGasto = null,
                categoria = categoria,
                descripcion = "",
                monto = "",
                fecha = DateUtils.today(),
                nota = "",
                isRecurring = false,
                tipoGasto = GastoTipoResolver.preseleccionar(categoria),
                tipoGastoElegido = false,
                gastoFormError = null,
            )
        }
    }

    fun editGasto(gasto: GastoOperativoEntity) {
        if (!_uiState.value.puedeEditarGastos) return
        // Linked OT bevel COGS are managed from the dispensación; editing here desyncs OT vs gasto.
        if (gasto.origenDispensacionItemId != null) return
        _uiState.update {
            it.copy(
                isDialogVisible = true,
                editingGasto = gasto,
                categoria = gasto.categoria,
                descripcion = gasto.descripcion ?: "",
                monto = gasto.monto.toPlainString(),
                fecha = gasto.fecha,
                nota = gasto.nota ?: "",
                isRecurring = gasto.isRecurring,
                tipoGasto = GastoTipoResolver.resolver(gasto.categoria, gasto.tipoGasto),
                tipoGastoElegido = GastoTipoResolver.esValido(gasto.tipoGasto),
                gastoFormError = null,
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isDialogVisible = false, editingGasto = null, gastoFormError = null) }
    }

    fun updateCategoria(c: String) {
        _uiState.update {
            it.copy(
                categoria = c,
                tipoGasto = if (it.tipoGastoElegido) it.tipoGasto else GastoTipoResolver.preseleccionar(c),
            )
        }
    }
    fun updateTipoGasto(tipo: String) {
        if (!GastoTipoResolver.esValido(tipo)) return
        _uiState.update { it.copy(tipoGasto = tipo, tipoGastoElegido = true) }
    }
    fun updateDescripcion(d: String) {
        _uiState.update { it.copy(descripcion = d) }
    }
    fun updateMonto(m: String) {
        _uiState.update { it.copy(monto = m) }
    }
    fun updateFecha(f: LocalDate) {
        _uiState.update { it.copy(fecha = f) }
    }
    fun updateNota(n: String) {
        _uiState.update { it.copy(nota = n) }
    }
    fun toggleRecurrente() {
        _uiState.update { it.copy(isRecurring = !it.isRecurring) }
    }

    fun saveGasto() {
        val s = _uiState.value
        if (s.isLoading) return
        val monto = s.monto.toBigDecimalOrNull()
        if (monto == null || monto <= BigDecimal.ZERO) {
            _uiState.update { it.copy(gastoFormError = "Ingresa un monto válido") }
            return
        }
        _uiState.update { it.copy(isLoading = true, gastoFormError = null) }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val gasto = gastoDesdeFormulario(s, opticaId, monto)
                repository.upsertGastoOperativo(gasto)
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
                _uiState.update { it.copy(isDialogVisible = false, editingGasto = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "saveGasto failed", e)
                _uiState.update { it.copy(gastoFormError = "No se pudo guardar el gasto. Inténtalo de nuevo.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // WHY: Rebuilding the entity from the form alone would wipe fechaProgramada, which the recurrence
    // generator relies on, every time a user edits a gasto.
    private fun gastoDesdeFormulario(s: CostosYGastosUiState, opticaId: String, monto: BigDecimal): GastoOperativoEntity {
        val base = s.editingGasto ?: GastoOperativoEntity(
            id = UUID.randomUUID().toString(),
            opticaId = opticaId,
            categoria = s.categoria,
            monto = monto,
            fecha = s.fecha,
        )
        return base.copy(
            categoria = s.categoria,
            descripcion = s.descripcion.ifBlank { null },
            monto = monto,
            fecha = s.fecha,
            nota = s.nota.ifBlank { null },
            isRecurring = s.isRecurring,
            tipoGasto = s.tipoGasto,
        )
    }

    fun confirmDeleteGasto(gasto: GastoOperativoEntity) {
        if (!_uiState.value.puedeEditarGastos) return
        if (gasto.origenDispensacionItemId != null) return
        _uiState.update { it.copy(deletingGasto = gasto, gastoDeleteError = null) }
    }

    fun dismissDeleteGasto() {
        _uiState.update { it.copy(deletingGasto = null, gastoDeleteError = null) }
    }

    fun deleteGasto() {
        val s = _uiState.value
        val gasto = s.deletingGasto ?: return
        if (s.isLoading || !s.puedeEditarGastos) return
        if (gasto.origenDispensacionItemId != null) {
            _uiState.update { it.copy(deletingGasto = null) }
            return
        }
        _uiState.update { it.copy(isLoading = true, gastoDeleteError = null) }
        viewModelScope.launch {
            try {
                repository.deleteGastoOperativo(gasto)
                postSaveSyncScheduler.scheduleFinanzasSync(gasto.opticaId)
                _uiState.update { it.copy(deletingGasto = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "deleteGasto failed", e)
                _uiState.update { it.copy(gastoDeleteError = "No se pudo eliminar el gasto. Inténtalo de nuevo.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun materialesOpticos(tipoLente: String = _uiState.value.costoTipoLente): List<String> =
        com.example.optoapp.domain.costo.LensCatalogRules.familias(
            tipoLente.ifBlank { com.example.optoapp.domain.costo.LensProductCatalog.TIPO_MONOFOCAL },
        ).ifEmpty { OpticalCatalog.MATERIALES }
    val tiposLente = OpticalCatalog.TIPO_LENTE.filter { it != "Lentes de Contacto" }
    val tratamientos = OpticalCatalog.TRATAMIENTOS

    fun showNewCosto() {
        if (!_uiState.value.puedeEditarMatrices) return
        val block = _uiState.value.selectedBlock
        val filter = block?.let(::blockFilter)
        _uiState.update {
            it.copy(
                isCostoDialogVisible = true,
                creatingCosto = true,
                costoMaterial = "",
                costoTipoLente = filter?.tipoLente.orEmpty(),
                costoStockOFabricacion = filter?.stockOFabricacion.orEmpty(),
                costoTratamiento = "",
                costoSerie = "",
                costoSubtipoBifocal = "",
                costoIndiceRefraccion = "",
                costoCurvaBase = "",
                costoCostoUnitario = "",
                costoSaveError = null,
                deletingCosto = null,
            )
        }
    }

    fun dismissCostoDialog() {
        _uiState.update {
            it.copy(
                isCostoDialogVisible = false,
                creatingCosto = false,
                costoSaveError = null,
            )
        }
    }

    fun updateCostoMaterial(value: String) {
        _uiState.update { it.copy(costoMaterial = value) }
    }
    fun updateCostoTipoLente(value: String) {
        _uiState.update { it.copy(costoTipoLente = value) }
    }
    fun updateCostoTratamiento(value: String) {
        _uiState.update { it.copy(costoTratamiento = value) }
    }
    fun updateCostoSerie(value: String) {
        _uiState.update { it.copy(costoSerie = value) }
    }
    fun updateCostoSubtipoBifocal(value: String) {
        _uiState.update { it.copy(costoSubtipoBifocal = value) }
    }
    fun updateCostoIndiceRefraccion(value: String) {
        _uiState.update { it.copy(costoIndiceRefraccion = value) }
    }
    fun updateCostoCurvaBase(value: String) {
        _uiState.update { it.copy(costoCurvaBase = value) }
    }
    fun updateCostoCostoUnitario(value: String) {
        _uiState.update { it.copy(costoCostoUnitario = value) }
    }

    fun saveCosto() {
        val s = _uiState.value
        if (s.costoMaterial.isBlank()) {
            _uiState.update { it.copy(costoSaveError = "Selecciona un material") }
            return
        }
        if (s.costoTipoLente.isBlank()) {
            _uiState.update { it.copy(costoSaveError = "Selecciona un tipo de lente") }
            return
        }
        val costoUnitario = s.costoCostoUnitario.toDoubleOrNull()
        if (costoUnitario == null || costoUnitario <= 0) {
            _uiState.update { it.copy(costoSaveError = "Ingresa un costo unitario válido") }
            return
        }
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val entity = CostoProductoEntity(
                    id = UUID.randomUUID().toString(),
                    opticaId = opticaId,
                    material = s.costoMaterial,
                    tipoLente = s.costoTipoLente,
                    stockOFabricacion = s.costoStockOFabricacion,
                    tratamiento = s.costoTratamiento.ifBlank { null },
                    serie = s.costoSerie.toIntOrNull(),
                    subtipoBifocal = s.costoSubtipoBifocal.ifBlank { null },
                    indiceRefraccion = s.costoIndiceRefraccion.ifBlank { null },
                    curvaBase = s.costoCurvaBase.ifBlank { null },
                    costoUnitario = costoUnitario,
                    vigenteDesde = DateUtils.toIso(DateUtils.today()),
                )
                saveCostoLocal(opticaId, entity)
                val block = s.selectedBlock ?: return@launch
                val filter = blockFilter(block)
                val refreshed = aplicarFiltroBloque(
                    costoProductoDao.getByBloque(opticaId, filter.stockOFabricacion).first(),
                    filter,
                )
                _uiState.update {
                    it.copy(
                        isCostoDialogVisible = false,
                        creatingCosto = false,
                        costosDelBloque = refreshed,
                        costoSaveError = null,
                        costoMaterial = "",
                        costoTipoLente = "",
                        costoStockOFabricacion = "",
                        costoTratamiento = "",
                        costoSerie = "",
                        costoSubtipoBifocal = "",
                        costoIndiceRefraccion = "",
                        costoCurvaBase = "",
                        costoCostoUnitario = "",
                    )
                }
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: Exception) {
                _uiState.update { it.copy(costoSaveError = "Error al guardar: ${e.message}") }
            }
        }
    }

    // WHY: Matrix upload sends only rows marked here, so a price edit without the mark would never leave the device.
    private suspend fun saveCostoLocal(opticaId: String, costo: CostoProductoEntity) {
        repository.withTransaction {
            costoProductoDao.upsertAll(listOf(costo.copy(updatedAt = Instant.now().toString())))
            syncStateTracker.markLocallyModified(opticaId, "costo_producto", costo.id)
        }
    }

    fun confirmDeleteCosto(costo: CostoProductoEntity) {
        if (!_uiState.value.puedeEditarMatrices) return
        _uiState.update { it.copy(deletingCosto = costo) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deletingCosto = null) }
    }

    fun deleteCosto() {
        if (!_uiState.value.puedeEditarMatrices) return
        val costo = _uiState.value.deletingCosto ?: return
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                saveCostoLocal(opticaId, costo.copy(vigenteHasta = DateUtils.toIso(DateUtils.today())))
                val block = _uiState.value.selectedBlock ?: return@launch
                val filter = blockFilter(block)
                val refreshed = aplicarFiltroBloque(
                    costoProductoDao.getByBloque(opticaId, filter.stockOFabricacion).first(),
                    filter,
                )
                _uiState.update {
                    it.copy(
                        deletingCosto = null,
                        costosDelBloque = refreshed,
                    )
                }
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(costoSaveError = "Error al eliminar: ${e.message}") }
            }
        }
    }
}
