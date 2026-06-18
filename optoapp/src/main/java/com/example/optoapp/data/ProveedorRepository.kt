package com.example.optoapp.data

import com.example.optoapp.data.proveedor.CategoriaMonturaDao
import com.example.optoapp.data.proveedor.MonturaProveedorDao
import com.example.optoapp.data.proveedor.ProveedorDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps supplier management isolated from OptoRepository's god-repository coupling.
 */
@Singleton
open class ProveedorRepository @Inject constructor(
    private val proveedorDao: ProveedorDao,
    private val monturaProveedorDao: MonturaProveedorDao,
    private val categoriaMonturaDao: CategoriaMonturaDao
) {

    fun getActivosByOptica(opticaId: String): Flow<List<Proveedor>> =
        proveedorDao.getActivosByOptica(opticaId)

    suspend fun getListByOptica(opticaId: String): List<Proveedor> =
        proveedorDao.getListByOptica(opticaId)

    suspend fun getById(id: String): Proveedor? =
        proveedorDao.getById(id)

    open suspend fun insert(proveedor: Proveedor) =
        proveedorDao.insert(proveedor)

    open suspend fun update(proveedor: Proveedor) =
        proveedorDao.update(proveedor)

    open suspend fun softDelete(id: String) {
        val existing = proveedorDao.getById(id) ?: return
        proveedorDao.update(existing.copy(activo = false))
    }

    fun getProveedoresByMontura(monturaId: String): Flow<List<MonturaProveedor>> =
        monturaProveedorDao.getByMontura(monturaId)

    suspend fun getProveedorLinksByProveedor(proveedorId: String): List<MonturaProveedor> =
        monturaProveedorDao.getByProveedor(proveedorId)

    open suspend fun linkMonturaProveedor(link: MonturaProveedor) =
        monturaProveedorDao.insert(link)

    open suspend fun unlinkMonturaProveedor(id: String) {
        val existing = monturaProveedorDao.getById(id) ?: return
        monturaProveedorDao.update(existing.copy(activo = false))
    }

    open suspend fun updateMonturaProveedor(link: MonturaProveedor) {
        monturaProveedorDao.update(link)
    }

    fun getCategoriasByOptica(opticaId: String): Flow<List<CategoriaMontura>> =
        categoriaMonturaDao.getByOptica(opticaId)

    suspend fun getCategoriaListByOptica(opticaId: String): List<CategoriaMontura> =
        categoriaMonturaDao.getListByOptica(opticaId)

    open suspend fun insertCategoria(categoria: CategoriaMontura) =
        categoriaMonturaDao.insert(categoria)

    open suspend fun updateCategoria(categoria: CategoriaMontura) =
        categoriaMonturaDao.update(categoria)

    open suspend fun upsertProveedor(proveedor: Proveedor) {
        val existing = proveedorDao.getById(proveedor.id)
        if (existing != null) {
            proveedorDao.update(proveedor)
        } else {
            runCatching { proveedorDao.insert(proveedor) }
        }
    }

    open suspend fun upsertCategoria(categoria: CategoriaMontura) {
        runCatching { categoriaMonturaDao.insert(categoria) }.onFailure {
            categoriaMonturaDao.update(categoria)
        }
    }
}
