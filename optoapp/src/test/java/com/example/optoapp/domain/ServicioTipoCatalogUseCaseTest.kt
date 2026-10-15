package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.RoomTransactionRunner
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric because Room.inMemoryDatabaseBuilder needs an Android Context (AGENTS.md real-Room exception).
@RunWith(RobolectricTestRunner::class)
class ServicioTipoCatalogUseCaseTest {

    private val opticaId = "o1"
    private lateinit var db: OptoDatabase
    private lateinit var useCase: ServicioTipoCatalogUseCase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        useCase = ServicioTipoCatalogUseCase(
            servicioTipoDao = db.servicioTipoDao(),
            syncStateTracker = SyncStateTracker(db.syncEntityStateDao(), db),
            transactionRunner = RoomTransactionRunner(db),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun estado(id: String) = db.syncEntityStateDao().getState(opticaId, "servicio_tipo", id)?.status

    @Test
    fun savingATypeStampsUpdatedAtAndMarksItForUpload() = runTest {
        val tipo = ServicioTipoEntity(id = "t1", opticaId = opticaId, nombre = "Biselado", costoPropio = 5.0, costoTercero = 12.0)

        useCase.guardar(tipo)

        val stored = db.servicioTipoDao().getById("t1", opticaId)!!
        assertEquals(tipo.copy(updatedAt = stored.updatedAt), stored)
        assertNotNull(stored.updatedAt)
        assertEquals("pending_upload", estado("t1"))
    }

    @Test
    fun deactivatingAndReactivatingEditInPlaceAndMarkForUpload() = runTest {
        db.servicioTipoDao().upsert(
            ServicioTipoEntity(id = "t1", opticaId = opticaId, nombre = "Biselado", updatedAt = "2026-01-01T00:00:00Z"),
        )

        useCase.desactivar("t1", opticaId)
        val inactivo = db.servicioTipoDao().getById("t1", opticaId)!!
        useCase.reactivar("t1", opticaId)
        val activo = db.servicioTipoDao().getById("t1", opticaId)!!

        assertFalse(inactivo.activo)
        assertNotEquals("2026-01-01T00:00:00Z", inactivo.updatedAt)
        assertTrue(activo.activo)
        assertEquals("Biselado", activo.nombre)
        assertEquals("pending_upload", estado("t1"))
        assertEquals(1, db.servicioTipoDao().getByOpticaIdList(opticaId).size)
    }

    @Test
    fun togglingAMissingTypeChangesNothing() = runTest {
        useCase.desactivar("missing", opticaId)

        assertEquals(null, estado("missing"))
        assertTrue(db.servicioTipoDao().getByOpticaIdList(opticaId).isEmpty())
    }

    @Test
    fun theCatalogOffersNoDeletePath() {
        val names = ServicioTipoCatalogUseCase::class.java.declaredMethods.map { it.name.lowercase() }

        assertTrue(names.none { "delete" in it || "eliminar" in it || "borrar" in it })
    }
}
