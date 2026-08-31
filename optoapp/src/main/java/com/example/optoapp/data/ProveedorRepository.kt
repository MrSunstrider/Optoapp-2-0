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
        val stamped = proveedor.copy(updatedAt = Instant.now().toString())
        proveedorDao.insert(stamped)
    }

    open suspend fun update(proveedor: Proveedor) {
        val stamped = proveedor.copy(updatedAt = Instant.now().toString())
        proveedorDao.update(
            id = stamped.id, opticaId = stamped.opticaId,
            nombre = stamped.nombre, ruc = stamped.ruc,
            telefono = stamped.telefono, email = stamped.email,
            direccion = stamped.direccion, contacto = stamped.contacto,
            activo = stamped.activo, tipo = stamped.tipo,
            updatedAt = stamped.updatedAt, updatedBy = stamped.updatedBy,
        )
    }

    open suspend fun softDelete(id: String, opticaId: String) {
        val existing = proveedorDao.getById(id, opticaId) ?: return
        val stamped = existing.copy(activo = false, updatedAt = Instant.now().toString())
        proveedorDao.update(
            id = stamped.id, opticaId = stamped.opticaId,
            nombre = stamped.nombre, ruc = stamped.ruc,
            telefono = stamped.telefono, email = stamped.email,
            direccion = stamped.direccion, contacto = stamped.contacto,
            activo = stamped.activo, tipo = stamped.tipo,
            updatedAt = stamped.updatedAt, updatedBy = stamped.updatedBy,
        )
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
        val existing = proveedorDao.getById(proveedor.id, proveedor.opticaId)
        if (existing != null) {
            proveedorDao.update(
                id = proveedor.id, opticaId = proveedor.opticaId,
                nombre = proveedor.nombre, ruc = proveedor.ruc,
                telefono = proveedor.telefono, email = proveedor.email,
                direccion = proveedor.direccion, contacto = proveedor.contacto,
                activo = proveedor.activo, tipo = proveedor.tipo,
                updatedAt = proveedor.updatedAt, updatedBy = proveedor.updatedBy,
            )
        } else {
            runCatching { proveedorDao.insert(proveedor) }
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
}
