package com.example.optoapp.data.montura

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class MonturaDashboardKpiRepository @Inject constructor(
    private val monturaDao: MonturaDao,
    private val monturaMovimientoDao: MonturaMovimientoDao,
) {
    fun totalModelos(opticaId: String): Flow<Int> = monturaDao.getTotalModelosCount(opticaId)

    fun stockByCategory(opticaId: String): Flow<List<CategoriaCount>> = monturaDao.getStockByCategory(opticaId)

    fun lowStockCount(opticaId: String, defaultThreshold: Int = 2): Flow<Int> = monturaDao.getLowStockCount(opticaId, defaultThreshold)

    fun lastMovementDate(opticaId: String): Flow<LocalDate?> = monturaMovimientoDao.getLastMovementDate(opticaId)
}
