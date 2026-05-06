package com.example.optoapp.domain.repository

import com.example.optoapp.domain.model.DispensacionModel
import com.example.optoapp.domain.model.PagoModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DispensacionRepository {
    fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionModel>>
    fun getDispensacionesByRange(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<DispensacionModel>>
    suspend fun getDispensacionById(id: String): DispensacionModel?
    suspend fun insertDispensacion(dispensacion: DispensacionModel)
    suspend fun updateDispensacion(dispensacion: DispensacionModel)
    suspend fun deleteDispensacion(id: String)
}

interface PagoRepository {
    fun getPagosByDispensacion(dispensacionId: String): Flow<List<PagoModel>>
    fun getPagosByServicioExtra(servicioExtraId: String): Flow<List<PagoModel>>
    fun getPagosByRange(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<PagoModel>>
    suspend fun insertPago(pago: PagoModel)
    suspend fun deletePago(pagoId: String)
}
