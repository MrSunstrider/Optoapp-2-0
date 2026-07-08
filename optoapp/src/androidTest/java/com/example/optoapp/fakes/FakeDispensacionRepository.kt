package com.example.optoapp.fakes

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * In-memory [com.example.optoapp.data.DispensacionRepository] for E2E tests.
 *
 * Stores dispensaciones, items, pagos, and servicios extra in mutable lists.
 * All operations are synchronous — no Room, no Supabase.
 */
class FakeDispensacionRepository {

    private val dispensaciones = mutableListOf<DispensacionOptica>()
    private val items = mutableListOf<DispensacionItem>()
    private val pagos = mutableListOf<Pago>()
    private val servicios = mutableListOf<ServicioExtra>()

    private val _dispensacionesFlow = MutableStateFlow<List<DispensacionOptica>>(emptyList())
    private val _itemsFlow = MutableStateFlow<List<DispensacionItem>>(emptyList())
    private val _pagosFlow = MutableStateFlow<List<Pago>>(emptyList())
    private val _serviciosFlow = MutableStateFlow<List<ServicioExtra>>(emptyList())

    // ── Dispensaciones ───────────────────────────────────────────────────────

    fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionOptica>> =
        _dispensacionesFlow.map { list -> list.filter { it.pacienteId == pacienteId } }

    fun getAllDispensacionesForOptica(opticaId: String): Flow<List<DispensacionOptica>> =
        _dispensacionesFlow.map { list -> list.filter { it.opticaId == opticaId } }

    suspend fun getDispensacionById(id: String): Resource<DispensacionOptica> {
        val d = dispensaciones.find { it.id == id }
        return if (d != null) Resource.Success(d)
        else Resource.Error("Dispensación no encontrada")
    }

    suspend fun insertDispensacion(dispensacion: DispensacionOptica) {
        dispensaciones.add(dispensacion)
        _dispensacionesFlow.value = dispensaciones.toList()
    }

    suspend fun updateDispensacion(dispensacion: DispensacionOptica) {
        val index = dispensaciones.indexOfFirst { it.id == dispensacion.id }
        if (index >= 0) {
            dispensaciones[index] = dispensacion
            _dispensacionesFlow.value = dispensaciones.toList()
        }
    }

    suspend fun deleteDispensacionById(id: String, opticaId: String) {
        dispensaciones.removeAll { it.id == id }
        items.removeAll { it.dispensacionId == id }
        pagos.removeAll { it.dispensacionId == id }
        _dispensacionesFlow.value = dispensaciones.toList()
        _itemsFlow.value = items.toList()
        _pagosFlow.value = pagos.toList()
    }

    // ── Items ────────────────────────────────────────────────────────────────

    fun getItemsByDispensacion(dispensacionId: String): Flow<List<DispensacionItem>> =
        _itemsFlow.map { list -> list.filter { it.dispensacionId == dispensacionId } }

    suspend fun insertDispensacionItem(item: DispensacionItem) {
        items.add(item)
        _itemsFlow.value = items.toList()
    }

    // ── Pagos ────────────────────────────────────────────────────────────────

    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>> =
        _pagosFlow.map { list -> list.filter { it.dispensacionId == dispensacionId } }

    suspend fun insertPago(pago: Pago) {
        pagos.add(pago)
        _pagosFlow.value = pagos.toList()
    }

    // ── Servicios Extra ──────────────────────────────────────────────────────

    fun getAllServiciosForOptica(opticaId: String): Flow<List<ServicioExtra>> =
        _serviciosFlow.map { list -> list.filter { it.opticaId == opticaId } }

    suspend fun insertServicio(servicio: ServicioExtra) {
        servicios.add(servicio)
        _serviciosFlow.value = servicios.toList()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Remove all data — call in `@After` to reset state between tests. */
    fun clear() {
        dispensaciones.clear()
        items.clear()
        pagos.clear()
        servicios.clear()
        _dispensacionesFlow.value = emptyList()
        _itemsFlow.value = emptyList()
        _pagosFlow.value = emptyList()
        _serviciosFlow.value = emptyList()
    }
}
