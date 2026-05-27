package com.example.optoapp.fakes

import com.example.optoapp.data.Resource
import com.example.optoapp.data.pacientes.PacientesSyncResult
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake [SyncPacientesUseCase] that records invocations instead of calling Supabase.
 *
 * Use in Hilt integration tests to verify the sync chain is triggered
 * without needing the test Supabase project schema.
 */
@Singleton
class FakeSyncPacientesUseCase @Inject constructor() : SyncPacientesUseCase(
    deletionSyncHelper = null!!,
    uploadSyncCoordinator = null!!,
    downloadSyncCoordinator = null!!,
    networkRetryHelper = null!!
) {
    var uploadCallCount = 0
    var downloadCallCount = 0
    var lastOpticaId: String? = null

    override suspend fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean
    ): Resource<PacientesSyncResult> {
        uploadCallCount++
        lastOpticaId = opticaId
        if (downloadAfterUpload) downloadCallCount++
        return Resource.Success(PacientesSyncResult(1, 1))
    }
}

/**
 * Fake [SyncHistorialUseCase] that records invocations.
 */
@Singleton
class FakeSyncHistorialUseCase @Inject constructor() : SyncHistorialUseCase(
    pacienteRepository = null!!,
    evaluacionDao = null!!,
    uploadSyncCoordinator = null!!,
    downloadSyncCoordinator = null!!,
    networkRetryHelper = null!!,
    postSaveSyncScheduler = null!!,
    supabase = null!!
) {
    var callCount = 0
    var lastOpticaId: String? = null

    override suspend fun invoke(opticaId: String): Resource<Unit> {
        callCount++
        lastOpticaId = opticaId
        return Resource.Success(Unit)
    }
}

/**
 * Fake [SyncFinanzasUseCase] that records invocations.
 * This class is `open` to enable test fakes. Must be kept in sync with
 * the real constructor signature.
 */
@Singleton
class FakeSyncFinanzasUseCase @Inject constructor() : SyncFinanzasUseCase(
    deletionSyncHelper = null!!,
    uploadSyncCoordinator = null!!,
    downloadSyncCoordinator = null!!,
    networkRetryHelper = null!!
) {
    var callCount = 0
    var lastOpticaId: String? = null

    override suspend fun invoke(opticaId: String): Resource<Unit> {
        callCount++
        lastOpticaId = opticaId
        return Resource.Success(Unit)
    }
}

/**
 * Fake [SyncInventarioUseCase] that records invocations.
 */
@Singleton
class FakeSyncInventarioUseCase @Inject constructor() : SyncInventarioUseCase(
    syncRepository = null!!,
    uploadSyncCoordinator = null!!,
    downloadSyncCoordinator = null!!,
    networkRetryHelper = null!!
) {
    var callCount = 0
    var lastOpticaId: String? = null

    override suspend fun invoke(opticaId: String): Resource<Unit> {
        callCount++
        lastOpticaId = opticaId
        return Resource.Success(Unit)
    }
}
