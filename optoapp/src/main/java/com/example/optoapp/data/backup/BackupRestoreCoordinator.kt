package com.example.optoapp.data.backup

import android.util.Log
import androidx.room.withTransaction
import com.example.optoapp.data.BackupData
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.DispensacionRepository
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.EvaluacionDao
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.PacienteDao
import com.example.optoapp.data.PacienteRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import java.time.Instant
import javax.inject.Inject

class BackupRestoreCoordinator @Inject constructor(
    private val pacienteRepo: PacienteRepository,
    private val dispensacionRepo: DispensacionRepository,
    private val evaluacionDao: EvaluacionDao,
    private val pacienteDao: PacienteDao,
    private val postSaveSyncScheduler: Lazy<PostSaveSyncScheduler>,
    private val database: OptoDatabase,
) {
    companion object {
        private const val TAG = "BackupRestoreCoordinator"
    }

    suspend fun clearAllData(opticaId: String) {
        database.withTransaction {
            database.openHelper.writableDatabase.apply {
                execSQL("DELETE FROM dispensacion_items WHERE optica_id = ?", arrayOf(opticaId))
                execSQL("DELETE FROM pagos WHERE opticaId = ?", arrayOf(opticaId))
                execSQL("DELETE FROM servicios_extra WHERE opticaId = ?", arrayOf(opticaId))
                execSQL("DELETE FROM dispensaciones WHERE opticaId = ?", arrayOf(opticaId))
                execSQL("DELETE FROM evaluaciones WHERE opticaId = ?", arrayOf(opticaId))
                execSQL("DELETE FROM pacientes WHERE opticaId = ?", arrayOf(opticaId))
            }
        }
    }

    suspend fun getBackupDataForOptica(opticaId: String): BackupData = BackupData(
        sourceOpticaId = opticaId,
        pacientes = pacienteRepo.getPacientesSnapshotForOptica(opticaId),
        evaluaciones = pacienteRepo.getEvaluacionesSnapshotForOptica(opticaId),
        dispensaciones = dispensacionRepo.getDispensacionesSnapshotForOptica(opticaId),
        pagos = dispensacionRepo.getPagosSnapshotForOptica(opticaId),
        serviciosExtra = dispensacionRepo.getServiciosSnapshotForOptica(opticaId),
    )

    suspend fun restoreBackup(backupData: BackupData, currentOpticaId: String) {
        clearAllData(currentOpticaId)

        backupData.pacientes?.forEach {
            try {
                pacienteRepo.insertPaciente(it.withDefaults().copy(opticaId = currentOpticaId))
                postSaveSyncScheduler.get().schedulePacientesSync(currentOpticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al restaurar paciente", e)
            }
        }

        backupData.evaluaciones?.forEach {
            try {
                pacienteRepo.insertEvaluacion(it.withDefaults().copy(opticaId = currentOpticaId))
                postSaveSyncScheduler.get().scheduleHistorialSync(currentOpticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al restaurar evaluación", e)
            }
        }

        backupData.dispensaciones?.forEach {
            try {
                dispensacionRepo.insertDispensacion(it.withDefaults().copy(opticaId = currentOpticaId))
                postSaveSyncScheduler.get().scheduleFinanzasSync(currentOpticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al restaurar dispensación", e)
            }
        }

        backupData.pagos?.forEach {
            try {
                dispensacionRepo.insertPago(it.withDefaults().copy(opticaId = currentOpticaId))
                postSaveSyncScheduler.get().scheduleFinanzasSync(currentOpticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al restaurar pago", e)
            }
        }

        backupData.serviciosExtra?.forEach {
            try {
                dispensacionRepo.insertServicio(it.withDefaults().copy(opticaId = currentOpticaId))
                postSaveSyncScheduler.get().scheduleFinanzasSync(currentOpticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al restaurar servicio extra", e)
            }
        }
    }

    private fun Paciente.withDefaults(): Paciente = copy(
        dni = dni ?: "",
        sexo = sexo ?: "",
        email = email ?: "",
        historiaOptometrica = historiaOptometrica ?: "",
        direccion = direccion ?: "",
        distrito = distrito ?: "",
        ocupacion = ocupacion ?: "",
        acompanante = acompanante ?: "",
        hobbies = hobbies ?: "",
        updatedAt = coalesceUpdatedAt(updatedAt),
    )

    private fun EvaluacionClinica.withDefaults(): EvaluacionClinica =
        copy(updatedAt = coalesceUpdatedAt(updatedAt))

    private fun DispensacionOptica.withDefaults(): DispensacionOptica =
        copy(updatedAt = coalesceUpdatedAt(updatedAt))

    private fun Pago.withDefaults(): Pago =
        copy(updatedAt = coalesceUpdatedAt(updatedAt))

    private fun ServicioExtra.withDefaults(): ServicioExtra =
        copy(updatedAt = coalesceUpdatedAt(updatedAt))

    private fun coalesceUpdatedAt(existing: String?): String =
        if (existing.isNullOrBlank()) Instant.now().toString() else existing
}
