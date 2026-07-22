package com.example.optoapp.data

import kotlinx.coroutines.flow.first
import java.time.LocalDate

data class ContextoFinanciero(
    val ot: String,
    val pacienteNombre: String,
    val pacienteId: String,
    val fecha: LocalDate,
    val descripcion: String,
)

interface DispensacionFinancieraRepository {
    suspend fun obtenerDispensacion(dispensacionId: String): Resource<DispensacionOptica>
    suspend fun obtenerContexto(dispensacionId: String): ContextoFinanciero
    suspend fun obtenerPagos(dispensacionId: String): List<Pago>
    suspend fun actualizarMontoTotal(dispensacionId: String, montoTotal: Double, opticaId: String)
    suspend fun actualizarEstado(dispensacionId: String, estado: String, fechaEntrega: LocalDate?, opticaId: String)
    suspend fun agregarPago(pago: Pago)
    suspend fun editarPago(pago: Pago)
    suspend fun eliminarPago(pago: Pago, opticaId: String)
    fun runInTransaction(block: () -> Unit)
    suspend fun <T> withTransaction(block: suspend () -> T): T
}

class DispensacionFinancieraRepositoryImpl(
    private val optoRepository: OptoRepository,
) : DispensacionFinancieraRepository {

    override fun runInTransaction(block: () -> Unit) {
        optoRepository.runInTransaction(block)
    }

    override suspend fun <T> withTransaction(block: suspend () -> T): T = optoRepository.withTransaction(block)

    override suspend fun obtenerDispensacion(dispensacionId: String): Resource<DispensacionOptica> = optoRepository.getDispensacionById(dispensacionId)

    override suspend fun obtenerPagos(dispensacionId: String): List<Pago> = optoRepository.getPagosByDispensacion(dispensacionId).first()

    override suspend fun obtenerContexto(dispensacionId: String): ContextoFinanciero {
        val dispResult = optoRepository.getDispensacionById(dispensacionId)
        if (dispResult is Resource.Success && dispResult.data != null) {
            val d = dispResult.data
            val pacienteNombre = when (val pResult = optoRepository.getPacienteByIdScoped(d.pacienteId, d.opticaId)) {
                is Resource.Success -> pResult.data?.nombreCompleto ?: ""
                else -> ""
            }
            return ContextoFinanciero(
                ot = d.ot,
                pacienteNombre = pacienteNombre,
                pacienteId = d.pacienteId,
                fecha = d.fecha,
                descripcion = buildDescripcion(d),
            )
        }
        return ContextoFinanciero(ot = "", pacienteNombre = "", pacienteId = "", fecha = LocalDate.now(), descripcion = "")
    }

    private fun buildDescripcion(d: DispensacionOptica): String {
        val parts = mutableListOf<String>()
        if (d.tipoLente.isNotBlank()) parts.add(d.tipoLente)
        if (d.materialLente.isNotBlank()) parts.add(d.materialLente)
        return parts.joinToString(" · ").ifBlank { "" }
    }

    override suspend fun actualizarMontoTotal(dispensacionId: String, montoTotal: Double, opticaId: String) {
        val result = optoRepository.getDispensacionById(dispensacionId)
        if (result is Resource.Success && result.data != null) {
            val updated = result.data.copy(montoTotal = montoTotal)
            optoRepository.updateDispensacion(updated)
        }
    }

    override suspend fun actualizarEstado(dispensacionId: String, estado: String, fechaEntrega: LocalDate?, opticaId: String) {
        val result = optoRepository.getDispensacionById(dispensacionId)
        if (result is Resource.Success && result.data != null) {
            val updated = result.data.copy(estadoEntrega = estado, fechaEntrega = fechaEntrega)
            optoRepository.updateDispensacion(updated)
        }
    }

    override suspend fun agregarPago(pago: Pago) {
        optoRepository.insertPago(pago)
    }

    override suspend fun editarPago(pago: Pago) {
        optoRepository.updatePago(pago)
    }

    override suspend fun eliminarPago(pago: Pago, opticaId: String) {
        optoRepository.deletePagoRegistrandoAnulacionEnCaja(pago, opticaId)
    }
}
