package com.example.optoapp.domain

import com.example.optoapp.data.CategoriaMontura
import com.example.optoapp.data.Proveedor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SyncProveedoresUseCase DTO and logic tests.
 * DTO conversion is tested without needing Supabase or MockK.
 */
class SyncProveedoresUseCaseKtTest {

    @Test
    fun proveedorEntity_allFieldsAreAccessible() {
        val p = Proveedor(
            id = "p1", nombre = "Optical Corp", ruc = "20123456789",
            telefono = "999888", email = "a@b.com", direccion = "Calle 1",
            contacto = "Juan", activo = true, opticaId = "o1",
            updatedAt = "2024-01-01T00:00:00Z"
        )
        assertEquals("p1", p.id)
        assertEquals("Optical Corp", p.nombre)
        assertEquals("20123456789", p.ruc)
        assertEquals("999888", p.telefono)
        assertEquals("a@b.com", p.email)
        assertEquals("Calle 1", p.direccion)
        assertEquals("Juan", p.contacto)
        assertTrue(p.activo)
        assertEquals("o1", p.opticaId)
        assertEquals("2024-01-01T00:00:00Z", p.updatedAt)
    }

    @Test
    fun proveedorEntity_defaultValues() {
        val p = Proveedor(
            id = "p1", nombre = "Test", ruc = "111", opticaId = "o1"
        )
        assertEquals("", p.telefono)
        assertEquals("", p.email)
        assertEquals("", p.direccion)
        assertEquals("", p.contacto)
        assertTrue(p.activo)
        assertTrue(p.updatedAt.isNullOrEmpty())
    }

    @Test
    fun proveedorEntity_inactiveProveedor() {
        val p = Proveedor(
            id = "p1", nombre = "Inactive", ruc = "111", opticaId = "o1", activo = false
        )
        assertEquals(false, p.activo)
    }

    @Test
    fun categoriaMonturaEntity_allFieldsAccessible() {
        val c = CategoriaMontura(
            id = "c1", nombre = "Sol", descripcion = "Lentes de sol", opticaId = "o1"
        )
        assertEquals("c1", c.id)
        assertEquals("Sol", c.nombre)
        assertEquals("Lentes de sol", c.descripcion)
        assertEquals("o1", c.opticaId)
    }

    @Test
    fun categoriaMonturaEntity_defaultDescription() {
        val c = CategoriaMontura(
            id = "c1", nombre = "Graduada", opticaId = "o1"
        )
        assertEquals("Graduada", c.nombre)
        assertEquals("", c.descripcion)
    }

    @Test
    fun proveedoresSyncResult_hasCorrectFields() {
        val result = ProveedoresSyncResult(
            uploadedProveedores = 5,
            uploadedCategorias = 3,
            downloadedProveedores = 2,
            downloadedCategorias = 1
        )
        assertEquals(5, result.uploadedProveedores)
        assertEquals(3, result.uploadedCategorias)
        assertEquals(2, result.downloadedProveedores)
        assertEquals(1, result.downloadedCategorias)
    }

    @Test
    fun proveedoresSyncResult_zeroForAll() {
        val result = ProveedoresSyncResult(0, 0, 0, 0)
        assertEquals(0, result.uploadedProveedores)
        assertEquals(0, result.uploadedCategorias)
        assertEquals(0, result.downloadedProveedores)
        assertEquals(0, result.downloadedCategorias)
    }

    @Test
    fun resourceSuccess_wrapsSyncResult() {
        val result = com.example.optoapp.data.Resource.Success(
            ProveedoresSyncResult(1, 0, 0, 0)
        )
        assertNotNull(result.data)
        assertEquals(1, result.data!!.uploadedProveedores)
    }
}
