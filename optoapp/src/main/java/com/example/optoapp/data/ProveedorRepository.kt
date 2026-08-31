package com.example.optoapp.data

import com.example.optoapp.data.proveedor.CategoriaMonturaDao
import com.example.optoapp.data.proveedor.MonturaProveedorDao
import com.example.optoapp.data.proveedor.ProveedorDao
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps supplier management isolated from OptoRepository's god-repository coupling.
 */
@Singleton
open class ProveedorRepository @Inject constructor(
    private val proveedorDao: ProveedorDao,
    private val monturaProveedorDao: MonturaProveedorDao,
    private val categoriaMonturaDao: CategoriaMonturaDao,
) {

    fun getActivosByOptica(opticaId: String): Flow<List<Proveedor>> = proveedorDao.getActivosByOptica(opticaId)

    suspend fun getListByOptica(opticaId: String): List<Proveedor> = proveedorDao.getListByOptica(opticaId)

    suspend fun getById(id: String, opticaId: String): Proveedor? = proveedorDao.getById(id, opticaId)

    open suspend fun insert(proveedor: Proveedor) {
        proveedorDao.insert(proveedor.copy(updatedAt = Instant.now().toString()))
    }

    open suspend fun update(proveedor: Proveedor) {
        persistUpdate(proveedor.copy(updatedAt = Instant.now().toString()))
    }

    open suspend fun softDelete(id: String, opticaId: String) {
        val existing = proveedorDao.getById(id, opticaId) ?: return
        persistUpdate(existing.copy(activo = false, updatedAt = Instant.now().toString()))
    }

    fun getProveedoresByMontura(monturaId: String): Flow<List<MonturaProveedor>> = monturaProveedorDao.getByMontura(monturaId)

    suspend fun getProveedorLinksByProveedor(proveedorId: String): List<MonturaProveedor> = monturaProveedorDao.getByProveedor(proveedorId)

    open suspend fun linkMonturaProveedor(link: MonturaProveedor) = monturaProveedorDao.insert(link)

    open suspend fun unlinkMonturaProveedor(id: String) {
        val existing = monturaProveedorDao.getById(id) ?: return
        monturaProveedorDao.update(existing.copy(activo = false))
    }

    open suspend fun updateMonturaProveedor(link: MonturaProveedor) {
        monturaProveedorDao.update(link)
    }

    fun getCategoriasByOptica(opticaId: String): Flow<List<CategoriaMontura>> = categoriaMonturaDao.getByOptica(opticaId)

    suspend fun getCategoriaListByOptica(opticaId: String): List<CategoriaMontura> = categoriaMonturaDao.getListByOptica(opticaId)

    open suspend fun insertCategoria(categoria: CategoriaMontura) = categoriaMonturaDao.insert(categoria)

    open suspend fun updateCategoria(categoria: CategoriaMontura) = categoriaMonturaDao.update(
        id = categoria.id,
        opticaId = categoria.opticaId,
        nombre = categoria.nombre,
        descripcion = categoria.descripcion,
    )

    open suspend fun upsertProveedor(proveedor: Proveedor) {
        // WHY: download can carry null/blank updatedAt; Room must not re-store null after backfill.
        val stamped = if (proveedor.updatedAt.isNullOrBlank()) {
            proveedor.copy(updatedAt = Instant.now().toString())
        } else {
            proveedor
        }
        val existing = proveedorDao.getById(stamped.id, stamped.opticaId)
        if (existing != null) {
            persistUpdate(stamped)
        } else {
            runCatching { proveedorDao.insert(stamped) }
        }
    }

    open suspend fun upsertCategoria(categoria: CategoriaMontura) {
        runCatching { categoriaMonturaDao.insert(categoria) }.onFailure {
            categoriaMonturaDao.update(
                id = categoria.id,
                opticaId = categoria.opticaId,
                nombre = categoria.nombre,
                descripcion = categoria.descripcion,
            )
        }
    }

    private suspend fun persistUpdate(proveedor: Proveedor) {
        proveedorDao.update(
            id = proveedor.id,
            opticaId = proveedor.opticaId,
            nombre = proveedor.nombre,
            ruc = proveedor.ruc,
            telefono = proveedor.telefono,
            email = proveedor.email,
            direccion = proveedor.direccion,
            contacto = proveedor.contacto,
            activo = proveedor.activo,
            tipo = proveedor.tipo,
            updatedAt = proveedor.updatedAt,
            updatedBy = proveedor.updatedBy,
        )
    }
}
