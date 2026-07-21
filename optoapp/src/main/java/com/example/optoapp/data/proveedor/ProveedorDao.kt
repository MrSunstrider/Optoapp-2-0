package com.example.optoapp.data.proveedor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.optoapp.data.Proveedor
import kotlinx.coroutines.flow.Flow

@Dao
interface ProveedorDao {
    @Query("SELECT * FROM proveedores WHERE opticaId = :opticaId AND activo = 1 ORDER BY nombre ASC")
    fun getActivosByOptica(opticaId: String): Flow<List<Proveedor>>

    @Query("SELECT * FROM proveedores WHERE opticaId = :opticaId")
    suspend fun getListByOptica(opticaId: String): List<Proveedor>

    @Query("SELECT * FROM proveedores WHERE id = :id")
    suspend fun getById(id: String): Proveedor?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(proveedor: Proveedor)

    @Query(
        """
        UPDATE proveedores SET nombre=:nombre, ruc=:ruc, telefono=:telefono,
        email=:email, direccion=:direccion, contacto=:contacto, activo=:activo,
        opticaId=:opticaId, updatedAt=:updatedAt, updatedBy=:updatedBy
        WHERE id=:id AND opticaId=:opticaId
    """,
    )
    suspend fun update(
        id: String,
        opticaId: String,
        nombre: String,
        ruc: String,
        telefono: String,
        email: String,
        direccion: String,
        contacto: String,
        activo: Boolean,
        updatedAt: String?,
        updatedBy: String?,
    ): Int
}
