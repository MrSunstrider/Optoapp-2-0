package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import javax.inject.Inject

data class HistorialSyncResult(val uploaded: Int, val downloaded: Int)

/**
 * FASE 3 – Paso 3.2
 * Sincronización de Evaluaciones Clínicas (historial clínico).
 *
 * Estrategia solo-subida (upload-first):
 *  • Las evaluaciones clínicas se crean en el dispositivo del optometrista.
 *  • Se suben a Supabase con upsert usando (id, opticaId) como clave única.
 *  • No se hace bajada aquí porque los datos clínicos son el origen de verdad local.
 *
 * Para escenarios multi-dispositivo futuros, deberá añadirse lógica de resolución
 * de conflictos basada en el campo "fecha".
 */
class SyncHistorialUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient
) {

    companion object {
        private const val TAG = "SyncHistorial"
        private const val TABLE = "evaluaciones"
    }

    /**
     * Ejecuta el ciclo completo: subida → bajada.
     */
    suspend operator fun invoke(opticaId: String): Resource<HistorialSyncResult> {
        return try {
            val uploaded = upload(opticaId)
            val downloaded = download(opticaId)
            Resource.Success(HistorialSyncResult(uploaded, downloaded))
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización de evaluaciones", e)
            Resource.Error("Error sincronizando historial clínico: ${e.localizedMessage}")
        }
    }

    // ─── SUBIDA ──────────────────────────────────────────────────────────────

    private suspend fun upload(opticaId: String): Int {
        val evaluaciones = repository.getAllEvaluacionesSnapshot()
            .filter { it.opticaId == opticaId }

        if (evaluaciones.isEmpty()) return 0

        val rows = evaluaciones.map { it.toRemoteMap() }
        supabase.postgrest[TABLE].upsert(rows)

        Log.d(TAG, "Subidas ${evaluaciones.size} evaluaciones a Supabase.")
        return evaluaciones.size
    }

    // ─── BAJADA ──────────────────────────────────────────────────────────────

    private suspend fun download(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE]
            .select {
                filter { eq("optica_id", opticaId) }
            }
            .decodeList<EvaluacionRemota>()

        if (remotos.isEmpty()) return 0

        remotos.forEach { remoto ->
            val local = remoto.toEntity()
            repository.insertEvaluacion(local)
        }

        Log.d(TAG, "Descargadas ${remotos.size} evaluaciones desde Supabase.")
        return remotos.size
    }
}

// ─── DTO de bajada ────────────────────────────────────────────────────────────

@Serializable
data class EvaluacionRemota(
    val id: String,
    val paciente_id: String,
    val fecha: Long,
    val optica_id: String,
    val motivo_consulta: String = "",
    val sintomas: String = "",
    val antecedentes_personales_oculares: String = "",
    val antecedentes_personales_sistemicos: String = "",
    val antecedentes_familiares_oculares: String = "",
    val antecedentes_familiares_sistemicos: String = "",
    val medicacion: String = "",
    val alergias: String = "",
    val necesidad_visual: String = "",
    val av_sc_od_lejos: String = "", val av_sc_oi_lejos: String = "",
    val av_sc_od_cerca: String = "", val av_sc_oi_cerca: String = "",
    val av_sc_ao: String = "", val av_sc_ao_cerca: String = "",
    val av_cc_od_lejos: String = "", val av_cc_oi_lejos: String = "",
    val av_cc_od_cerca: String = "", val av_cc_oi_cerca: String = "",
    val av_cc_ao_px: String = "", val av_cc_ao_cerca: String = "",
    val obj_od_esf: String = "", val obj_od_cil: String = "", val obj_od_eje: String = "",
    val obj_oi_esf: String = "", val obj_oi_cil: String = "", val obj_oi_eje: String = "",
    val subj_od_esf: String = "", val subj_od_cil: String = "", val subj_od_eje: String = "",
    val subj_oi_esf: String = "", val subj_oi_cil: String = "", val subj_oi_eje: String = "",
    val receta_od_esf: String = "", val receta_od_cil: String = "", val receta_od_eje: String = "", val receta_od_av: String = "",
    val receta_oi_esf: String = "", val receta_oi_cil: String = "", val receta_oi_eje: String = "", val receta_oi_av: String = "",
    val add_cerca_od: String = "", val add_cerca_oi: String = "",
    val add_intermedia_od: String = "", val add_intermedia_oi: String = "",
    val add_av: String = "",
    val dip_lejos: String = "", val dip_cerca: String = "", val dip_intermedio: String = "",
    val diagnostico: String = "",
    val diagnostico_od: String = "",
    val diagnostico_oi: String = "",
    val diagnostico_otros: String = "",
    val plan_tratamiento: String = "",
    val observaciones: String = "",
    val proxima_fecha_control: String = "",
    val proxima_cita: Long? = null,
    val balance_od: Boolean = false,
    val balance_oi: Boolean = false,
    val otros_presbicia: Boolean = false,
    val otros_anisometropia: Boolean = false,
    val otros_ambliopia: Boolean = false,
    val lc_od_esf: String = "", val lc_od_cil: String = "", val lc_od_eje: String = "",
    val lc_oi_esf: String = "", val lc_oi_cil: String = "", val lc_oi_eje: String = "",
    val lc_radio_base_od: String = "", val lc_diametro_od: String = "",
    val lc_radio_base_oi: String = "", val lc_diametro_oi: String = "",
    val lc_laboratorio: String = "", val lc_tipo_lente: String = "",
    val lc_material: String = "", val lc_observaciones: String = ""
) {
    fun toEntity(): EvaluacionClinica = EvaluacionClinica(
        id = id, pacienteId = paciente_id, fecha = fecha, opticaId = optica_id,
        motivoConsulta = motivo_consulta, sintomas = sintomas,
        antecedentesPersonalesOculares = antecedentes_personales_oculares,
        antecedentesPersonalesSistemicos = antecedentes_personales_sistemicos,
        antecedentesFamiliaresOculares = antecedentes_familiares_oculares,
        antecedentesFamiliaresSistemicos = antecedentes_familiares_sistemicos,
        medicacion = medicacion, alergias = alergias,
        necesidadVisual = necesidad_visual.split(",").filter { it.isNotBlank() },
        avScOdLejos = av_sc_od_lejos, avScOiLejos = av_sc_oi_lejos,
        avScOdCerca = av_sc_od_cerca, avScOiCerca = av_sc_oi_cerca,
        avScAo = av_sc_ao, avScAoCerca = av_sc_ao_cerca,
        avCcOdLejos = av_cc_od_lejos, avCcOiLejos = av_cc_oi_lejos,
        avCcOdCerca = av_cc_od_cerca, avCcOiCerca = av_cc_oi_cerca,
        avCcAoPx = av_cc_ao_px, avCcAoCerca = av_cc_ao_cerca,
        objOdEsf = obj_od_esf, objOdCil = obj_od_cil, objOdEje = obj_od_eje,
        objOiEsf = obj_oi_esf, objOiCil = obj_oi_cil, objOiEje = obj_oi_eje,
        subjOdEsf = subj_od_esf, subjOdCil = subj_od_cil, subjOdEje = subj_od_eje,
        subjOiEsf = subj_oi_esf, subjOiCil = subj_oi_cil, subjOiEje = subj_oi_eje,
        recetaOdEsf = receta_od_esf, recetaOdCil = receta_od_cil, recetaOdEje = receta_od_eje, recetaOdAv = receta_od_av,
        recetaOiEsf = receta_oi_esf, recetaOiCil = receta_oi_cil, recetaOiEje = receta_oi_eje, recetaOiAv = receta_oi_av,
        addCercaOd = add_cerca_od, addCercaOi = add_cerca_oi,
        addIntermediaOd = add_intermedia_od, addIntermediaOi = add_intermedia_oi,
        addAv = add_av,
        dipLejos = dip_lejos, dipCerca = dip_cerca, dipIntermedio = dip_intermedio,
        diagnostico = diagnostico,
        diagnosticoOd = diagnostico_od.split(",").filter { it.isNotBlank() },
        diagnosticoOi = diagnostico_oi.split(",").filter { it.isNotBlank() },
        diagnosticoOtros = diagnostico_otros.split(",").filter { it.isNotBlank() },
        planTratamiento = plan_tratamiento, observaciones = observaciones,
        proximaFechaControl = proxima_fecha_control, proximaCita = proxima_cita,
        balanceOd = balance_od, balanceOi = balance_oi,
        otrosPresbicia = otros_presbicia, otrosAnisometropia = otros_anisometropia, otrosAmbliopia = otros_ambliopia,
        lcOdEsf = lc_od_esf, lcOdCil = lc_od_cil, lcOdEje = lc_od_eje,
        lcOiEsf = lc_oi_esf, lcOiCil = lc_oi_cil, lcOiEje = lc_oi_eje,
        lcRadioBaseOd = lc_radio_base_od, lcDiametroOd = lc_diametro_od,
        lcRadioBaseOi = lc_radio_base_oi, lcDiametroOi = lc_diametro_oi,
        lcLaboratorio = lc_laboratorio, lcTipoLente = lc_tipo_lente,
        lcMaterial = lc_material, lcObservaciones = lc_observaciones
    )
}

// ─── Extensión: EvaluacionClinica → Mapa para Supabase ───────────────────────
// Se mapean solo los campos clave para mantener la consulta manejable.
// Si la tabla de Supabase tiene todas las columnas, agregar los campos restantes aquí.

private fun EvaluacionClinica.toRemoteMap(): Map<String, Any?> = mapOf(
    "id"                                  to id,
    "paciente_id"                         to pacienteId,
    "fecha"                               to fecha,
    "optica_id"                           to opticaId,
    "motivo_consulta"                     to motivoConsulta,
    "sintomas"                            to sintomas,
    "antecedentes_personales_oculares"    to antecedentesPersonalesOculares,
    "antecedentes_personales_sistemicos"  to antecedentesPersonalesSistemicos,
    "antecedentes_familiares_oculares"    to antecedentesFamiliaresOculares,
    "antecedentes_familiares_sistemicos"  to antecedentesFamiliaresSistemicos,
    "medicacion"                          to medicacion,
    "alergias"                            to alergias,
    "necesidad_visual"                    to necesidadVisual.joinToString(","),
    // AV sin corrección
    "av_sc_od_lejos"  to avScOdLejos,  "av_sc_oi_lejos"  to avScOiLejos,
    "av_sc_od_cerca"  to avScOdCerca,  "av_sc_oi_cerca"  to avScOiCerca,
    "av_sc_ao"        to avScAo,       "av_sc_ao_cerca"  to avScAoCerca,
    // AV con corrección
    "av_cc_od_lejos"  to avCcOdLejos,  "av_cc_oi_lejos"  to avCcOiLejos,
    "av_cc_od_cerca"  to avCcOdCerca,  "av_cc_oi_cerca"  to avCcOiCerca,
    "av_cc_ao_px"     to avCcAoPx,     "av_cc_ao_cerca"  to avCcAoCerca,
    // Refracción objetiva
    "obj_od_esf" to objOdEsf, "obj_od_cil" to objOdCil, "obj_od_eje" to objOdEje,
    "obj_oi_esf" to objOiEsf, "obj_oi_cil" to objOiCil, "obj_oi_eje" to objOiEje,
    // Refracción subjetiva
    "subj_od_esf" to subjOdEsf, "subj_od_cil" to subjOdCil, "subj_od_eje" to subjOdEje,
    "subj_oi_esf" to subjOiEsf, "subj_oi_cil" to subjOiCil, "subj_oi_eje" to subjOiEje,
    // Receta final
    "receta_od_esf" to recetaOdEsf, "receta_od_cil" to recetaOdCil,
    "receta_od_eje" to recetaOdEje, "receta_od_av"  to recetaOdAv,
    "receta_oi_esf" to recetaOiEsf, "receta_oi_cil" to recetaOiCil,
    "receta_oi_eje" to recetaOiEje, "receta_oi_av"  to recetaOiAv,
    // ADD
    "add_cerca_od"       to addCercaOd,       "add_cerca_oi"       to addCercaOi,
    "add_intermedia_od"  to addIntermediaOd,   "add_intermedia_oi"  to addIntermediaOi,
    "add_av"             to addAv,
    // DIP/DNP
    "dip_lejos"      to dipLejos,
    "dip_cerca"      to dipCerca,
    "dip_intermedio" to dipIntermedio,
    // Diagnóstico
    "diagnostico"       to diagnostico,
    "diagnostico_od"    to diagnosticoOd.joinToString(","),
    "diagnostico_oi"    to diagnosticoOi.joinToString(","),
    "diagnostico_otros" to diagnosticoOtros.joinToString(","),
    "plan_tratamiento"  to planTratamiento,
    "observaciones"     to observaciones,
    "proxima_fecha_control" to proximaFechaControl,
    "proxima_cita"          to proximaCita,
    // Flags diagnóstico
    "balance_od"         to balanceOd,
    "balance_oi"         to balanceOi,
    "otros_presbicia"    to otrosPresbicia,
    "otros_anisometropia" to otrosAnisometropia,
    "otros_ambliopia"    to otrosAmbliopia,
    // Contactología
    "lc_od_esf"       to lcOdEsf,       "lc_od_cil"      to lcOdCil,      "lc_od_eje"      to lcOdEje,
    "lc_oi_esf"       to lcOiEsf,       "lc_oi_cil"      to lcOiCil,      "lc_oi_eje"      to lcOiEje,
    "lc_radio_base_od" to lcRadioBaseOd, "lc_diametro_od" to lcDiametroOd,
    "lc_radio_base_oi" to lcRadioBaseOi, "lc_diametro_oi" to lcDiametroOi,
    "lc_laboratorio"  to lcLaboratorio,  "lc_tipo_lente"  to lcTipoLente,
    "lc_material"     to lcMaterial,     "lc_observaciones" to lcObservaciones
)
