package com.example.optoapp.data.repository.impl

import com.example.optoapp.data.DispensacionDao
import com.example.optoapp.data.PagoDao
import com.example.optoapp.data.mappers.FinancialMapper
import com.example.optoapp.domain.model.DispensacionModel
import com.example.optoapp.domain.model.PagoModel
import com.example.optoapp.domain.repository.DispensacionRepository
import com.example.optoapp.domain.repository.PagoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class DispensacionRepositoryImpl @Inject constructor(
    private val dispensacionDao: DispensacionDao
) : DispensacionRepository {
    override fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionModel>> =
        dispensacionDao.getDispensacionesByPaciente(pacienteId).map { list ->
            list.map { FinancialMapper.toDomain(it) }
        }

    override fun getDispensacionesByRange(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<DispensacionModel>> =
        dispensacionDao.getDispensacionesByDateRangeForOptica(start, end, opticaId).map { list ->
            list.map { FinancialMapper.toDomain(it) }
        }

    override suspend fun getDispensacionById(id: String): DispensacionModel? =
        dispensacionDao.getDispensacionById(id)?.let { FinancialMapper.toDomain(it) }

    override suspend fun insertDispensacion(dispensacion: DispensacionModel) {
        dispensacionDao.insertDispensacion(FinancialMapper.toEntity(dispensacion))
    }

    override suspend fun updateDispensacion(dispensacion: DispensacionModel) {
        dispensacionDao.updateDispensacion(FinancialMapper.toEntity(dispensacion))
    }

    override suspend fun deleteDispensacion(id: String) {
        dispensacionDao.deleteById(id)
    }
}

class PagoRepositoryImpl @Inject constructor(
    private val pagoDao: PagoDao
) : PagoRepository {
    override fun getPagosByDispensacion(dispensacionId: String): Flow<List<PagoModel>> =
        pagoDao.getPagosByDispensacion(dispensacionId).map { list ->
            list.map { FinancialMapper.toDomain(it) }
        }

    override fun getPagosByServicioExtra(servicioExtraId: String): Flow<List<PagoModel>> =
        pagoDao.getPagosByServicioExtra(servicioExtraId).map { list ->
            list.map { FinancialMapper.toDomain(it) }
        }

    override fun getPagosByRange(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<PagoModel>> =
        pagoDao.getPagosByDateRangeForOptica(start, end, opticaId).map { list ->
            list.map { FinancialMapper.toDomain(it) }
        }

    override suspend fun insertPago(pago: PagoModel) {
        pagoDao.insertPago(FinancialMapper.toEntity(pago))
    }

    override suspend fun deletePago(pagoId: String) {
        pagoDao.getPagoById(pagoId)?.let {
            pagoDao.deletePago(it)
        }
    }
}
