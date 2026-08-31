package com.example.optoapp.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProveedorRepositoryStampTest {

    private val proveedorDao = mockk<com.example.optoapp.data.proveedor.ProveedorDao>(relaxed = true)
    private lateinit var repository: ProveedorRepository

    @Before
    fun setUp() {
        repository = ProveedorRepository(
            proveedorDao,
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    }

    @Test
    fun insert_setsUpdatedAt() = runTest {
        val slot = slot<Proveedor>()
        coEvery { proveedorDao.insert(capture(slot)) } returns Unit

        val proveedor = Proveedor(id = "p1", nombre = "Optica Supply", ruc = "20111111111", opticaId = "o1")
        repository.insert(proveedor)

        assertNotNull(slot.captured.updatedAt)
        coVerify { proveedorDao.insert(any()) }
    }

    @Test
    fun update_persistsTipoAndStampsUpdatedAt() = runTest {
        coEvery {
            proveedorDao.update(
                id = any(),
                opticaId = any(),
                nombre = any(),
                ruc = any(),
                telefono = any(),
                email = any(),
                direccion = any(),
                contacto = any(),
                activo = any(),
                tipo = any(),
                updatedAt = any(),
                updatedBy = any(),
            )
        } returns 1

        repository.update(
            Proveedor(
                id = "p1",
                nombre = "Optica Supply",
                ruc = "20111111111",
                opticaId = "o1",
                tipo = "lentes",
            ),
        )

        coVerify {
            proveedorDao.update(
                id = "p1",
                opticaId = "o1",
                nombre = "Optica Supply",
                ruc = "20111111111",
                telefono = any(),
                email = any(),
                direccion = any(),
                contacto = any(),
                activo = true,
                tipo = "lentes",
                updatedAt = match { !it.isNullOrBlank() },
                updatedBy = any(),
            )
        }
    }

    @Test
    fun softDelete_preservesTipo() = runTest {
        coEvery { proveedorDao.getById("p1", "o1") } returns Proveedor(
            id = "p1",
            nombre = "Optica Supply",
            ruc = "20111111111",
            opticaId = "o1",
            tipo = "accesorios",
            activo = true,
        )
        coEvery {
            proveedorDao.update(
                id = any(),
                opticaId = any(),
                nombre = any(),
                ruc = any(),
                telefono = any(),
                email = any(),
                direccion = any(),
                contacto = any(),
                activo = any(),
                tipo = any(),
                updatedAt = any(),
                updatedBy = any(),
            )
        } returns 1

        repository.softDelete("p1", "o1")

        coVerify {
            proveedorDao.update(
                id = "p1",
                opticaId = "o1",
                nombre = any(),
                ruc = any(),
                telefono = any(),
                email = any(),
                direccion = any(),
                contacto = any(),
                activo = false,
                tipo = "accesorios",
                updatedAt = match { !it.isNullOrBlank() },
                updatedBy = any(),
            )
        }
    }

    @Test
    fun upsertProveedor_updatePath_persistsTipo() = runTest {
        coEvery { proveedorDao.getById("p1", "o1") } returns Proveedor(
            id = "p1",
            nombre = "Old",
            ruc = "111",
            opticaId = "o1",
            tipo = "monturas",
        )
        coEvery {
            proveedorDao.update(
                id = any(),
                opticaId = any(),
                nombre = any(),
                ruc = any(),
                telefono = any(),
                email = any(),
                direccion = any(),
                contacto = any(),
                activo = any(),
                tipo = any(),
                updatedAt = any(),
                updatedBy = any(),
            )
        } returns 1

        repository.upsertProveedor(
            Proveedor(
                id = "p1",
                nombre = "New",
                ruc = "111",
                opticaId = "o1",
                tipo = "laboratorio",
                updatedAt = "2026-08-31T12:00:00Z",
            ),
        )

        coVerify {
            proveedorDao.update(
                id = "p1",
                opticaId = "o1",
                nombre = "New",
                ruc = "111",
                telefono = any(),
                email = any(),
                direccion = any(),
                contacto = any(),
                activo = true,
                tipo = "laboratorio",
                updatedAt = "2026-08-31T12:00:00Z",
                updatedBy = any(),
            )
        }
    }

    @Test
    fun upsertProveedor_nullUpdatedAt_stampsBeforePersist() = runTest {
        coEvery { proveedorDao.getById("p1", "o1") } returns null
        val slot = slot<Proveedor>()
        coEvery { proveedorDao.insert(capture(slot)) } returns Unit

        repository.upsertProveedor(
            Proveedor(
                id = "p1",
                nombre = "New",
                ruc = "111",
                opticaId = "o1",
                updatedAt = null,
            ),
        )

        assertNotNull(slot.captured.updatedAt)
        assertTrue(slot.captured.updatedAt!!.isNotBlank())
    }
}
