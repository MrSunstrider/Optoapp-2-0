package com.example.optoapp.domain

import com.example.optoapp.util.AppLogger
import com.example.optoapp.data.Resource
import com.example.optoapp.data.montura.CategoriaCount
import com.example.optoapp.data.montura.MonturaDashboardKpiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject

data class InventoryKpiSummary(
    val totalModelos: Int,
    val lowStockCount: Int,
    val stockByCategory: List<CategoriaCount>,
    val lastMovementDate: String?
)

class SyncInventoryKpisUseCase @Inject constructor(
    private val kpiRepository: MonturaDashboardKpiRepository
) {
    companion object {
        private const val TAG = "SyncInventoryKpis"
    }

    suspend operator fun invoke(opticaId: String): Resource<InventoryKpiSummary> {
        return try {
            val total = kpiRepository.totalModelos(opticaId).first()
            val lowStock = kpiRepository.lowStockCount(opticaId).first()
            val categories = kpiRepository.stockByCategory(opticaId).first()
            val lastDate = kpiRepository.lastMovementDate(opticaId).first()

            val summary = InventoryKpiSummary(
                totalModelos = total,
                lowStockCount = lowStock,
                stockByCategory = categories,
                lastMovementDate = lastDate?.toString()
            )
            AppLogger.d(TAG, "KPI sync completado para óptica $opticaId: $total modelos, $lowStock stock bajo")
            Resource.Success(summary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppLogger.e(TAG, "Error de red al sincronizar KPIs", e)
            Resource.Error(e.message ?: "Error al sincronizar KPIs")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error inesperado al sincronizar KPIs", e)
            Resource.Error(e.message ?: "Error al sincronizar KPIs")
        }
    }
}
