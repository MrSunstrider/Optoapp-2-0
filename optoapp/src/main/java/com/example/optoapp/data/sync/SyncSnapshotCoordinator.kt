package com.example.optoapp.data.sync

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.DispensacionRepository
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.PacienteDao
import com.example.optoapp.data.PacienteRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SyncRepository
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao
import javax.inject.Inject

class SyncSnapshotCoordinator @Inject constructor(
    private val pacienteDao: PacienteDao,
    private val monturaDao: MonturaDao,
    private val monturaMovimientoDao: MonturaMovimientoDao,
    private val pacienteRepo: PacienteRepository,
    private val dispensacionRepo: DispensacionRepository,
    private val syncRepo: SyncRepository
) {
    // ── Upserts para sync entrante ──────────────────────────────────────────
    suspend fun upsertPaciente(paciente: Paciente) = pacienteDao.insertPaciente(paciente)

    suspend fun upsertMontura(montura: Montura) = monturaDao.insertMontura(montura)

    suspend fun upsertMonturaMovimiento(movimiento: MonturaMovimiento) =
        monturaMovimientoDao.insertMovimiento(movimiento)

    // ── Snapshots para sync saliente ────────────────────────────────────────
    suspend fun getPacientesSnapshotForOptica(opticaId: String): List<Paciente> =
        pacienteRepo.getPacientesSnapshotForOptica(opticaId)

    suspend fun getEvaluacionesSnapshotForOptica(opticaId: String): List<EvaluacionClinica> =
        pacienteRepo.getEvaluacionesSnapshotForOptica(opticaId)

    suspend fun getDispensacionesSnapshotForOptica(opticaId: String): List<DispensacionOptica> =
        dispensacionRepo.getDispensacionesSnapshotForOptica(opticaId)

    suspend fun getDispensacionItemsSnapshotForOptica(opticaId: String): List<DispensacionItem> =
        dispensacionRepo.getItemsListByOptica(opticaId)

    suspend fun getPagosSnapshotForOptica(opticaId: String): List<Pago> =
        dispensacionRepo.getPagosSnapshotForOptica(opticaId)

    suspend fun getServiciosSnapshotForOptica(opticaId: String): List<ServicioExtra> =
        dispensacionRepo.getServiciosSnapshotForOptica(opticaId)

    suspend fun getMonturasSnapshotForOptica(opticaId: String): List<Montura> =
        syncRepo.getMonturasSnapshotForOptica(opticaId)

    suspend fun getMovimientosMonturaSnapshotForOptica(opticaId: String): List<MonturaMovimiento> =
        syncRepo.getMovimientosMonturaSnapshotForOptica(opticaId)
}
