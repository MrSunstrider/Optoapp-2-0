package com.example.optoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Unknown costo_origen values count as missing, as in CoberturaCostoClassifier, so the CTA never disagrees with
// the server coverage. Servicios stay out because CostCoverageBadge already reports them.
private const val ITEM_FALTANTE_DE_D =
    "i.dispensacion_id = d.id AND i.optica_id = d.opticaId AND (" +
        "(i.costo_origen IS NULL AND COALESCE(i.costo_real_od, 0) + COALESCE(i.costo_real_oi, 0) + " +
        "COALESCE(i.costo_real_montura, 0) + COALESCE(i.costo_real_biselado, 0) + COALESCE(i.costo_real_lc, 0) <= 0) " +
        "OR (i.costo_origen IS NOT NULL AND i.costo_origen NOT IN ('manual', 'matriz', 'mixto')))"

private const val OT_SIN_COSTO =
    "d.opticaId = :opticaId AND d.fecha >= :desde AND d.fecha <= :hasta " +
        "AND d.estadoEntrega NOT IN ('Anulado', 'Reclamada') " +
        "AND EXISTS (SELECT 1 FROM dispensacion_items i WHERE $ITEM_FALTANTE_DE_D)"

data class OtSinCostoRow(
    val id: String,
    val ot: String,
    val pacienteId: String,
    val pacienteNombre: String?,
    val fecha: LocalDate,
    val montoTotal: Double,
    val itemsFaltantes: Int,
)

@Dao
interface DispensacionDao {
    @Query("SELECT COUNT(*) FROM dispensaciones d WHERE $OT_SIN_COSTO")
    fun countOtsSinCosto(opticaId: String, desde: LocalDate, hasta: LocalDate): Flow<Int>

    @Query(
        "SELECT d.id AS id, d.ot AS ot, d.pacienteId AS pacienteId, p.nombreCompleto AS pacienteNombre, " +
            "d.fecha AS fecha, d.montoTotal AS montoTotal, " +
            "(SELECT COUNT(*) FROM dispensacion_items i WHERE $ITEM_FALTANTE_DE_D) AS itemsFaltantes " +
            "FROM dispensaciones d LEFT JOIN pacientes p ON p.id = d.pacienteId " +
            "WHERE $OT_SIN_COSTO ORDER BY d.fecha DESC",
    )
    fun observeOtsSinCosto(opticaId: String, desde: LocalDate, hasta: LocalDate): Flow<List<OtSinCostoRow>>

    @Query("SELECT * FROM dispensaciones WHERE pacienteId = :pacienteId AND opticaId = :opticaId ORDER BY fecha DESC")
    fun getDispensacionesByPaciente(pacienteId: String, opticaId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones WHERE opticaId = :opticaId")
    fun getAllDispensacionesForOptica(opticaId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones WHERE id = :id AND opticaId = :opticaId")
    suspend fun getDispensacionById(id: String, opticaId: String): DispensacionOptica?

    @Upsert
    suspend fun insertDispensacion(dispensacion: DispensacionOptica)

    @Update
    suspend fun updateDispensacion(dispensacion: DispensacionOptica)

    @Query("UPDATE dispensaciones SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Query("SELECT * FROM dispensaciones WHERE opticaId = :opticaId")
    suspend fun getDispensacionesListByOptica(opticaId: String): List<DispensacionOptica>

    @Query("SELECT * FROM dispensaciones WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId")
    fun getDispensacionesByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT SUM(montoTotal) FROM dispensaciones WHERE opticaId = :opticaId")
    fun getTotalVendidoForOptica(opticaId: String): Flow<Double?>

    @Query("SELECT SUM(montoPagado) FROM dispensaciones WHERE opticaId = :opticaId")
    fun getTotalPagadoForOptica(opticaId: String): Flow<Double?>

    @Query("SELECT ot FROM dispensaciones WHERE opticaId = :opticaId AND ot LIKE ('OT-' || :year || '-%')")
    suspend fun getOtsWithYearPrefix(opticaId: String, year: String): List<String>

    @Query("SELECT ot FROM dispensaciones WHERE opticaId = :opticaId AND ot LIKE (:prefix || '%')")
    suspend fun getOtsWithPrefix(opticaId: String, prefix: String): List<String>

    @Query("SELECT * FROM dispensaciones WHERE reclamo_origen_id = :originalId AND opticaId = :opticaId LIMIT 1")
    suspend fun getByReclamoOrigenId(originalId: String, opticaId: String): DispensacionOptica?

    @Query("DELETE FROM dispensaciones WHERE id = :id AND opticaId = :opticaId")
    suspend fun deleteById(id: String, opticaId: String): Int

    @Query("SELECT * FROM dispensaciones WHERE pacienteId = :pacienteId AND opticaId = :opticaId ORDER BY fecha DESC LIMIT 1")
    suspend fun getLastDispensacionByPacienteId(pacienteId: String, opticaId: String): DispensacionOptica?

    @Query("SELECT * FROM dispensaciones WHERE id IN (:ids) AND opticaId = :opticaId")
    suspend fun getDispensacionesByIds(ids: List<String>, opticaId: String): List<DispensacionOptica>
}
