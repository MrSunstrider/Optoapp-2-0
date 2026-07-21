package com.example.optoapp.fakes

import com.example.optoapp.data.Resource
import com.example.optoapp.domain.PacientesSyncResult

/**
 * Standalone fake for SyncPacientesUseCase — tracks invocations.
 *
 * Does NOT extend the real use case because `invoke` is a final operator.
 * Provides the same `invoke` signature for testing purposes.
 */
class FakeSyncPacientesUseCase {

    var uploadCallCount = 0
    var downloadCallCount = 0
    var lastOpticaId: String? = null

    /** Matches the signature of [com.example.optoapp.domain.SyncPacientesUseCase.invoke]. */
    suspend fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true,
    ): Resource<PacientesSyncResult> {
        uploadCallCount++
        lastOpticaId = opticaId
        if (downloadAfterUpload) downloadCallCount++
        return Resource.Success(PacientesSyncResult(1, 1))
    }
}

/**
 * Standalone fake for SyncHistorialUseCase — tracks invocations.
 */
class FakeSyncHistorialUseCase {

    var callCount = 0
    var lastOpticaId: String? = null

    suspend fun invoke(opticaId: String): Resource<Unit> {
        callCount++
        lastOpticaId = opticaId
        return Resource.Success(Unit)
    }
}

/**
 * Standalone fake for SyncFinanzasUseCase — tracks invocations.
 */
class FakeSyncFinanzasUseCase {

    var callCount = 0
    var lastOpticaId: String? = null

    suspend fun invoke(opticaId: String): Resource<Unit> {
        callCount++
        lastOpticaId = opticaId
        return Resource.Success(Unit)
    }
}

/**
 * Standalone fake for SyncInventarioUseCase — tracks invocations.
 */
class FakeSyncInventarioUseCase {

    var callCount = 0
    var lastOpticaId: String? = null

    suspend fun invoke(opticaId: String): Resource<Unit> {
        callCount++
        lastOpticaId = opticaId
        return Resource.Success(Unit)
    }
}
