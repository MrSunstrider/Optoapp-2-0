package com.example.optoapp.data.montura

import androidx.room.*
import com.example.optoapp.data.Montura
import kotlinx.coroutines.flow.Flow

@Dao
interface MonturaDao {
    @Query("SELECT * FROM monturas WHERE opticaId = :opticaId ORDER BY activo DESC, marca ASC, modelo ASC")
    fun getMonturasByOptica(opticaId: String): Flow<List<Montura>>

    @Query("SELECT * FROM monturas WHERE id = :id AND opticaId = :opticaId")
    suspend fun getMonturaByIdForOptica(id: String, opticaId: String): Montura?

    @Query("UPDATE monturas SET stockActual = stockActual + :delta WHERE id = :monturaId AND opticaId = :opticaId AND (stockActual + :delta) >= 0")
    suspend fun adjustStock(monturaId: String, opticaId: String, delta: Int): Int

    @Upsert
    suspend fun insertMontura(montura: Montura)

    @Query("""
        UPDATE monturas SET sku=:sku, marca=:marca, modelo=:modelo, color=:color,
        talla=:talla, costo=:costo, precio=:precio, stockActual=:stockActual,
        stockMinimo=:stockMinimo, activo=:activo, tipoAro=:tipoAro,
        materialMontura=:materialMontura, anchoMm=:anchoMm, puenteMm=:puenteMm,
        alturaMm=:alturaMm, imagenUri=:imagenUri, categoria=:categoria,
        coleccion=:coleccion, temporada=:temporada, estadoComercial=:estadoComercial,
        genero=:genero, opticaId=:opticaId, updatedAt=:updatedAt, updatedBy=:updatedBy
        WHERE id=:id AND opticaId=:opticaId
    """)
    suspend fun updateMontura(
        id: String, opticaId: String, sku: String, marca: String, modelo: String,
        color: String, talla: String, costo: Double, precio: Double,
        stockActual: Int, stockMinimo: Int, activo: Boolean, tipoAro: String,
        materialMontura: String, anchoMm: Double?, puenteMm: Double?,
        alturaMm: Double?, imagenUri: String?, categoria: String,
        coleccion: String, temporada: String, estadoComercial: String,
        genero: String, updatedAt: String?, updatedBy: String?
    ): Int

    @Query("DELETE FROM monturas WHERE id = :id AND opticaId = :opticaId")
    suspend fun deleteMontura(id: String, opticaId: String): Int

    @Query("SELECT * FROM monturas WHERE opticaId = :opticaId")
    suspend fun getMonturasListByOptica(opticaId: String): List<Montura>

    @Query("""
        SELECT * FROM monturas
        WHERE opticaId = :opticaId
          AND activo = 1
          AND (:marca IS NULL OR marca LIKE '%' || :marca || '%')
          AND (:material IS NULL OR materialMontura LIKE '%' || :material || '%')
          AND (:categoria IS NULL OR categoria = :categoria)
          AND (:precioMin IS NULL OR precio >= :precioMin)
          AND (:precioMax IS NULL OR precio <= :precioMax)
          AND (CASE WHEN :stockBajo = 0 THEN 1
                    WHEN :stockBajo = 1 AND stockActual <= stockMinimo THEN 1
                    ELSE 0 END)
        ORDER BY marca ASC, modelo ASC
    """)
    suspend fun searchMonturas(
        opticaId: String,
        marca: String? = null,
        material: String? = null,
        categoria: String? = null,
        precioMin: Double? = null,
        precioMax: Double? = null,
        stockBajo: Int = 0
    ): List<Montura>

    @Query("SELECT COUNT(*) FROM monturas WHERE opticaId = :opticaId AND activo = 1")
    fun getTotalModelosCount(opticaId: String): Flow<Int>

    @Query("SELECT categoria, COUNT(*) as cnt FROM monturas WHERE opticaId = :opticaId AND activo = 1 AND categoria != '' GROUP BY categoria")
    fun getStockByCategory(opticaId: String): Flow<List<CategoriaCount>>

    @Query("SELECT COUNT(*) FROM monturas WHERE opticaId = :opticaId AND activo = 1 AND stockActual <= COALESCE(NULLIF(stockMinimo, 0), :defaultThreshold)")
    fun getLowStockCount(opticaId: String, defaultThreshold: Int = 2): Flow<Int>

    @Query("SELECT * FROM monturas WHERE opticaId = :opticaId AND activo = 1 AND stockActual <= COALESCE(NULLIF(stockMinimo, 0), :defaultThreshold)")
    suspend fun getLowStockList(opticaId: String, defaultThreshold: Int = 2): List<Montura>
}

data class CategoriaCount(
    val categoria: String,
    val cnt: Int
)
