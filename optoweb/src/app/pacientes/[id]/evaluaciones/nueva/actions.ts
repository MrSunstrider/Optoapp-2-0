"use server";

import { redirect } from "next/navigation";

import { localTodayDateOnly } from "@/lib/pacientes";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

function textOrEmpty(formData: FormData, key: string): string {
  return String(formData.get(key) ?? "").trim();
}

function intOrNull(formData: FormData, key: string): number | null {
  const raw = String(formData.get(key) ?? "").trim();
  if (!raw) return null;
  const num = Number(raw);
  if (!Number.isFinite(num)) return null;
  return Math.max(0, Math.round(num));
}

function buildEvaluacionPayload(
  formData: FormData,
  activeOpticaId: string,
  pacienteId: string
) {
  const fecha = textOrEmpty(formData, "fecha") || localTodayDateOnly();
  return {
    paciente_id: pacienteId,
    optica_id: activeOpticaId,
    fecha,
    motivo_consulta: textOrEmpty(formData, "motivoConsulta"),
    sintomas: textOrEmpty(formData, "sintomasSignos"),
    antecedentes_personales_oculares: textOrEmpty(
      formData,
      "antecedentesPersonalesOculares"
    ),
    antecedentes_personales_sistemicos: textOrEmpty(
      formData,
      "antecedentesPersonalesSistemicos"
    ),
    antecedentes_familiares_oculares: textOrEmpty(
      formData,
      "antecedentesFamiliaresOculares"
    ),
    antecedentes_familiares_sistemicos: textOrEmpty(
      formData,
      "antecedentesFamiliaresSistemicos"
    ),
    medicacion: textOrEmpty(formData, "medicacion"),
    alergias: textOrEmpty(formData, "alergias"),
    necesidad_visual: textOrEmpty(formData, "necesidadVisual"),
    av_sc_ao: textOrEmpty(formData, "avScAo"),
    av_sc_od_lejos: textOrEmpty(formData, "avScOdLejos"),
    av_sc_oi_lejos: textOrEmpty(formData, "avScOiLejos"),
    av_cc_ao_px: textOrEmpty(formData, "avCcAoPx"),
    av_cc_od_lejos: textOrEmpty(formData, "avCcOdLejos"),
    av_cc_oi_lejos: textOrEmpty(formData, "avCcOiLejos"),
    estereopsis_valor: textOrEmpty(formData, "estereopsisValor"),
    estereopsis_segundos: textOrEmpty(formData, "estereopsisSegundos"),
    lang: textOrEmpty(formData, "lang"),
    worth: textOrEmpty(formData, "worth"),
    ishihara: textOrEmpty(formData, "ishihara"),
    farnsworth: textOrEmpty(formData, "farnsworth"),
    schirmer_od: textOrEmpty(formData, "schirmerOd"),
    schirmer_oi: textOrEmpty(formData, "schirmerOi"),
    osdi_puntuacion: intOrNull(formData, "osdiPuntuacion"),
    osdi_clasificacion: textOrEmpty(formData, "osdiClasificacion"),
    sensibilidad_contraste: textOrEmpty(formData, "sensibilidadContraste"),
    sensibilidad_frecuencia: textOrEmpty(formData, "sensibilidadFrecuencia"),
    amsler: textOrEmpty(formData, "amsler"),
    campo_visual: textOrEmpty(formData, "campoVisual"),
    campo_visual_descripcion: textOrEmpty(formData, "campoVisualDescripcion"),
    ph_od: textOrEmpty(formData, "phOd"),
    ph_oi: textOrEmpty(formData, "phOi"),
    kappa_od: textOrEmpty(formData, "kappaOd"),
    kappa_oi: textOrEmpty(formData, "kappaOi"),
    hirshberg: textOrEmpty(formData, "hirshberg"),
    ducciones_od: textOrEmpty(formData, "duccionesOd"),
    ducciones_oi: textOrEmpty(formData, "duccionesOi"),
    versiones_ao: textOrEmpty(formData, "versionesAo"),
    cover_test_6m: textOrEmpty(formData, "coverTest6m"),
    cover_test_40cm: textOrEmpty(formData, "coverTest40cm"),
    cover_test_10cm: textOrEmpty(formData, "coverTest10cm"),
    ppc_or: textOrEmpty(formData, "ppcOr"),
    ppc_luz: textOrEmpty(formData, "ppcLuz"),
    ppc_frl: textOrEmpty(formData, "ppcFrl"),
    reflejo_fotomotor: textOrEmpty(formData, "reflejoFotomotor"),
    reflejo_consensual: textOrEmpty(formData, "reflejoConsensual"),
    reflejo_acomodativo: textOrEmpty(formData, "reflejoAcomodativo"),
    obj_od_esf: textOrEmpty(formData, "objOdEsf"),
    obj_od_cil: textOrEmpty(formData, "objOdCil"),
    obj_od_eje: textOrEmpty(formData, "objOdEje"),
    obj_oi_esf: textOrEmpty(formData, "objOiEsf"),
    obj_oi_cil: textOrEmpty(formData, "objOiCil"),
    obj_oi_eje: textOrEmpty(formData, "objOiEje"),
    subj_od_esf: textOrEmpty(formData, "subjOdEsf"),
    subj_od_cil: textOrEmpty(formData, "subjOdCil"),
    subj_od_eje: textOrEmpty(formData, "subjOdEje"),
    subj_oi_esf: textOrEmpty(formData, "subjOiEsf"),
    subj_oi_cil: textOrEmpty(formData, "subjOiCil"),
    subj_oi_eje: textOrEmpty(formData, "subjOiEje"),
    receta_od_esf: textOrEmpty(formData, "recetaOdEsf"),
    receta_od_cil: textOrEmpty(formData, "recetaOdCil"),
    receta_od_eje: textOrEmpty(formData, "recetaOdEje"),
    receta_od_av: textOrEmpty(formData, "recetaOdAv"),
    receta_oi_esf: textOrEmpty(formData, "recetaOiEsf"),
    receta_oi_cil: textOrEmpty(formData, "recetaOiCil"),
    receta_oi_eje: textOrEmpty(formData, "recetaOiEje"),
    receta_oi_av: textOrEmpty(formData, "recetaOiAv"),
    add_cerca_od: textOrEmpty(formData, "addCercaOd"),
    add_cerca_oi: textOrEmpty(formData, "addCercaOi"),
    add_av: textOrEmpty(formData, "addAv"),
    dip_lejos: textOrEmpty(formData, "dipLejos"),
    dip_cerca: textOrEmpty(formData, "dipCerca"),
    prisma_od_valor: textOrEmpty(formData, "prismaOdValor"),
    prisma_od_base: textOrEmpty(formData, "prismaOdBase"),
    prisma_oi_valor: textOrEmpty(formData, "prismaOiValor"),
    prisma_oi_base: textOrEmpty(formData, "prismaOiBase"),
    k1_od: textOrEmpty(formData, "k1Od"),
    k2_od: textOrEmpty(formData, "k2Od"),
    k1_oi: textOrEmpty(formData, "k1Oi"),
    k2_oi: textOrEmpty(formData, "k2Oi"),
    lc_od_esf: textOrEmpty(formData, "lcOdEsf"),
    lc_od_cil: textOrEmpty(formData, "lcOdCil"),
    lc_od_eje: textOrEmpty(formData, "lcOdEje"),
    lc_oi_esf: textOrEmpty(formData, "lcOiEsf"),
    lc_oi_cil: textOrEmpty(formData, "lcOiCil"),
    lc_oi_eje: textOrEmpty(formData, "lcOiEje"),
    lc_radio_base_od: textOrEmpty(formData, "lcRadioBaseOd"),
    lc_radio_base_oi: textOrEmpty(formData, "lcRadioBaseOi"),
    lc_diametro_od: textOrEmpty(formData, "lcOdDia"),
    lc_diametro_oi: textOrEmpty(formData, "lcOiDia"),
    lc_laboratorio: textOrEmpty(formData, "lcLaboratorio"),
    lc_tipo_lente: textOrEmpty(formData, "lcTipoLente"),
    lc_material: textOrEmpty(formData, "lcMaterial"),
    lc_fecha_adaptacion: textOrEmpty(formData, "lcFechaAdaptacion") || null,
    lc_observaciones: textOrEmpty(formData, "lcObservaciones"),
    diagnostico_od: textOrEmpty(formData, "diagnosticoOd"),
    diagnostico_oi: textOrEmpty(formData, "diagnosticoOi"),
    balance_od: textOrEmpty(formData, "balanceOd") === "true",
    balance_oi: textOrEmpty(formData, "balanceOi") === "true",
    otros_presbicia: textOrEmpty(formData, "otrosPresbicia") === "true",
    otros_anisometropia: textOrEmpty(formData, "otrosAnisometropia") === "true",
    otros_ambliopia: textOrEmpty(formData, "otrosAmbliopia") === "true",
    auto_presbicia: textOrEmpty(formData, "autoPresbicia") !== "false",
    auto_anisometropia: textOrEmpty(formData, "autoAnisometropia") !== "false",
    auto_ambliopia: textOrEmpty(formData, "autoAmbliopia") !== "false",
    plan_tratamiento: textOrEmpty(formData, "planTratamiento"),
    cita_estado: textOrEmpty(formData, "citaEstado") || "programada",
    diagnostico: textOrEmpty(formData, "diagnosticoResumen"),
    indicaciones: textOrEmpty(formData, "indicaciones"),
    observaciones: textOrEmpty(formData, "notasFinales"),
    proxima_cita: textOrEmpty(formData, "proximaCita") || null
  };
}

export async function createEvaluacionAction(
  prevState: { error?: string } | null,
  formData: FormData
): Promise<{ error?: string } | null> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { error: "Sin óptica activa" };
  if (!canManagePacientes(activeOptica.rol)) return { error: "Sin permiso" };

  const pacienteId = textOrEmpty(formData, "pacienteId");
  if (!pacienteId) return { error: "Falta paciente" };

  const payload = {
    id: crypto.randomUUID(),
    ...buildEvaluacionPayload(formData, activeOptica.opticaId, pacienteId)
  };

  const supabase = await createClient();
  const { error } = await supabase.from("evaluaciones").insert(payload);
  if (error) {
    return { error: "No se pudo guardar la evaluación. Verifica permisos o intenta nuevamente." };
  }

  redirect(`/pacientes/${pacienteId}/evaluaciones?msg=eval_creada`);
  return null;
}

export async function updateEvaluacionAction(
  prevState: { error?: string } | null,
  formData: FormData
): Promise<{ error?: string } | null> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { error: "Sin óptica activa" };
  if (!canManagePacientes(activeOptica.rol)) return { error: "Sin permiso" };

  const pacienteId = textOrEmpty(formData, "pacienteId");
  const evaluacionId = textOrEmpty(formData, "evaluacionId");
  if (!pacienteId || !evaluacionId) return { error: "Faltan datos" };

  const supabase = await createClient();
  const { error } = await supabase
    .from("evaluaciones")
    .update(buildEvaluacionPayload(formData, activeOptica.opticaId, pacienteId))
    .eq("id", evaluacionId)
    .eq("optica_id", activeOptica.opticaId)
    .eq("paciente_id", pacienteId);

  if (error) {
    return { error: "No se pudo guardar la evaluación. Verifica permisos o intenta nuevamente." };
  }

  redirect(`/pacientes/${pacienteId}/evaluaciones?msg=eval_actualizada`);
  return null;
}
