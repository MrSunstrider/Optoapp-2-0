import type { SupabaseClient } from "@supabase/supabase-js";
import { assertNoDbError } from "@/lib/supabase/db-error";

export type EvaluacionListRow = {
  id: string;
  fecha: string | null;
  proxima_cita: string | null;
};

export type EvaluacionCardRow = EvaluacionListRow & {
  diagnostico: string | null;
  receta_od_esf: string | null;
  receta_od_cil: string | null;
  receta_od_eje: string | null;
  receta_od_av: string | null;
  receta_oi_esf: string | null;
  receta_oi_cil: string | null;
  receta_oi_eje: string | null;
  receta_oi_av: string | null;
};

const EVAL_CARD_SELECT =
  "id,fecha,proxima_cita,diagnostico," +
  "receta_od_esf,receta_od_cil,receta_od_eje,receta_od_av," +
  "receta_oi_esf,receta_oi_cil,receta_oi_eje,receta_oi_av";

export type DispensacionListRow = {
  id: string;
  fecha: string | null;
  monto_total: number | null;
  monto_pagado: number | null;
  estado_entrega: string | null;
  ot: string | null;
};

export type ServicioExtraListRow = {
  id: string;
  fecha: string | null;
  descripcion: string | null;
  monto_total: number | null;
  a_cuenta: number | null;
  estado: string | null;
  ot: string | null;
};

export async function fetchEvaluacionesPorPaciente(
  supabase: SupabaseClient,
  opticaId: string,
  pacienteId: string
): Promise<EvaluacionListRow[]> {
  const { data, error } = await supabase
    .from("evaluaciones")
    .select("id,fecha,proxima_cita")
    .eq("optica_id", opticaId)
    .eq("paciente_id", pacienteId)
    .order("fecha", { ascending: false })
    .abortSignal(AbortSignal.timeout(12_000));

  assertNoDbError(error, "Evaluaciones del paciente");
  return (data ?? []) as EvaluacionListRow[];
}

export async function fetchEvaluacionesDetalladasPorPaciente(
  supabase: SupabaseClient,
  opticaId: string,
  pacienteId: string
): Promise<EvaluacionCardRow[]> {
  const { data, error } = await supabase
    .from("evaluaciones")
    .select(EVAL_CARD_SELECT)
    .eq("optica_id", opticaId)
    .eq("paciente_id", pacienteId)
    .order("fecha", { ascending: false })
    .abortSignal(AbortSignal.timeout(12_000));

  assertNoDbError(error, "Evaluaciones del paciente");
  return (data ?? []) as unknown as EvaluacionCardRow[];
}

export type EvaluacionPdfRow = EvaluacionCardRow & {
  motivo_consulta: string | null;
  observaciones: string | null;
  dip_lejos: string | null;
  dip_cerca: string | null;
  av_cc_ao_px: string | null;
  av_cc_od_cerca: string | null;
  av_cc_oi_cerca: string | null;
  av_cc_ao_cerca: string | null;
  add_cerca_od: string | null;
  add_cerca_oi: string | null;
  add_intermedia_od: string | null;
  add_intermedia_oi: string | null;
  prisma_od_valor: string | null;
  prisma_od_base: string | null;
  prisma_oi_valor: string | null;
  prisma_oi_base: string | null;
  diagnostico_od: string | null;
  diagnostico_oi: string | null;
  otros_presbicia: boolean | null;
};

export type EvaluacionEditRow = {
  id: string;
  fecha: string | null;
  motivo_consulta: string | null;
  sintomas: string | null;
  antecedentes_personales_oculares: string | null;
  antecedentes_personales_sistemicos: string | null;
  antecedentes_familiares_oculares: string | null;
  antecedentes_familiares_sistemicos: string | null;
  medicacion: string | null;
  alergias: string | null;
  necesidad_visual: string | null;
  av_sc_ao: string | null;
  av_sc_od_lejos: string | null;
  av_sc_oi_lejos: string | null;
  av_cc_ao_px: string | null;
  av_cc_od_lejos: string | null;
  av_cc_oi_lejos: string | null;
  estereopsis_valor: string | null;
  estereopsis_segundos: string | null;
  lang: string | null;
  worth: string | null;
  ishihara: string | null;
  farnsworth: string | null;
  schirmer_od: string | null;
  schirmer_oi: string | null;
  osdi_puntuacion: number | null;
  osdi_clasificacion: string | null;
  sensibilidad_contraste: string | null;
  sensibilidad_frecuencia: string | null;
  amsler: string | null;
  campo_visual: string | null;
  campo_visual_descripcion: string | null;
  ph_od: string | null;
  ph_oi: string | null;
  kappa_od: string | null;
  kappa_oi: string | null;
  hirshberg: string | null;
  ducciones_od: string | null;
  ducciones_oi: string | null;
  versiones_ao: string | null;
  cover_test_6m: string | null;
  cover_test_40cm: string | null;
  cover_test_10cm: string | null;
  ppc_or: string | null;
  ppc_luz: string | null;
  ppc_frl: string | null;
  reflejo_fotomotor: string | null;
  reflejo_consensual: string | null;
  reflejo_acomodativo: string | null;
  obj_od_esf: string | null;
  obj_od_cil: string | null;
  obj_od_eje: string | null;
  obj_oi_esf: string | null;
  obj_oi_cil: string | null;
  obj_oi_eje: string | null;
  subj_od_esf: string | null;
  subj_od_cil: string | null;
  subj_od_eje: string | null;
  subj_oi_esf: string | null;
  subj_oi_cil: string | null;
  subj_oi_eje: string | null;
  receta_od_esf: string | null;
  receta_od_cil: string | null;
  receta_od_eje: string | null;
  receta_od_av: string | null;
  receta_oi_esf: string | null;
  receta_oi_cil: string | null;
  receta_oi_eje: string | null;
  receta_oi_av: string | null;
  add_cerca_od: string | null;
  add_cerca_oi: string | null;
  add_av: string | null;
  dip_lejos: string | null;
  dip_cerca: string | null;
  prisma_od_valor: string | null;
  prisma_od_base: string | null;
  prisma_oi_valor: string | null;
  prisma_oi_base: string | null;
  k1_od: string | null;
  k2_od: string | null;
  k1_oi: string | null;
  k2_oi: string | null;
  lc_od_esf: string | null;
  lc_od_cil: string | null;
  lc_od_eje: string | null;
  lc_oi_esf: string | null;
  lc_oi_cil: string | null;
  lc_oi_eje: string | null;
  lc_radio_base_od: string | null;
  lc_radio_base_oi: string | null;
  lc_diametro_od: string | null;
  lc_diametro_oi: string | null;
  lc_laboratorio: string | null;
  lc_tipo_lente: string | null;
  lc_material: string | null;
  lc_fecha_adaptacion: string | null;
  lc_observaciones: string | null;
  diagnostico_od: string | null;
  diagnostico_oi: string | null;
  balance_od: boolean | null;
  balance_oi: boolean | null;
  otros_presbicia: boolean | null;
  otros_anisometropia: boolean | null;
  otros_ambliopia: boolean | null;
  auto_presbicia: boolean | null;
  auto_anisometropia: boolean | null;
  auto_ambliopia: boolean | null;
  plan_tratamiento: string | null;
  cita_estado: string | null;
  diagnostico: string | null;
  indicaciones: string | null;
  observaciones: string | null;
  proxima_cita: string | null;
};

export async function fetchUltimaEvaluacionParaPdf(
  supabase: SupabaseClient,
  opticaId: string,
  pacienteId: string
): Promise<EvaluacionPdfRow | null> {
  const { data, error } = await supabase
    .from("evaluaciones")
    .select(
      `${EVAL_CARD_SELECT},motivo_consulta,observaciones,` +
      `dip_lejos,dip_cerca,av_cc_ao_px,av_cc_od_cerca,av_cc_oi_cerca,av_cc_ao_cerca,` +
      `add_cerca_od,add_cerca_oi,add_intermedia_od,add_intermedia_oi,` +
      `prisma_od_valor,prisma_od_base,prisma_oi_valor,prisma_oi_base,` +
      `diagnostico_od,diagnostico_oi,otros_presbicia`
    )
    .eq("optica_id", opticaId)
    .eq("paciente_id", pacienteId)
    .order("fecha", { ascending: false })
    .limit(1)
    .maybeSingle();

  assertNoDbError(error, "Última evaluación");
  if (!data) return null;
  return data as unknown as EvaluacionPdfRow;
}

export async function fetchEvaluacionById(
  supabase: SupabaseClient,
  opticaId: string,
  pacienteId: string,
  evaluacionId: string
): Promise<EvaluacionEditRow | null> {
  const { data, error } = await supabase
    .from("evaluaciones")
    .select(
      [
        "id,fecha,motivo_consulta,sintomas,",
        "antecedentes_personales_oculares,antecedentes_personales_sistemicos,",
        "antecedentes_familiares_oculares,antecedentes_familiares_sistemicos,",
        "medicacion,alergias,necesidad_visual,",
        "av_sc_ao,av_sc_od_lejos,av_sc_oi_lejos,",
        "av_cc_ao_px,av_cc_od_lejos,av_cc_oi_lejos,",
        "estereopsis_valor,estereopsis_segundos,lang,worth,",
        "ishihara,farnsworth,schirmer_od,schirmer_oi,",
        "osdi_puntuacion,osdi_clasificacion,sensibilidad_contraste,sensibilidad_frecuencia,",
        "amsler,campo_visual,campo_visual_descripcion,",
        "ph_od,ph_oi,kappa_od,kappa_oi,hirshberg,ducciones_od,ducciones_oi,versiones_ao,",
        "cover_test_6m,cover_test_40cm,cover_test_10cm,",
        "ppc_or,ppc_luz,ppc_frl,",
        "reflejo_fotomotor,reflejo_consensual,reflejo_acomodativo,",
        "obj_od_esf,obj_od_cil,obj_od_eje,obj_oi_esf,obj_oi_cil,obj_oi_eje,",
        "subj_od_esf,subj_od_cil,subj_od_eje,subj_oi_esf,subj_oi_cil,subj_oi_eje,",
        "receta_od_esf,receta_od_cil,receta_od_eje,receta_od_av,",
        "receta_oi_esf,receta_oi_cil,receta_oi_eje,receta_oi_av,",
        "add_cerca_od,add_cerca_oi,add_av,dip_lejos,dip_cerca,",
        "prisma_od_valor,prisma_od_base,prisma_oi_valor,prisma_oi_base,",
        "k1_od,k2_od,k1_oi,k2_oi,",
        "lc_od_esf,lc_od_cil,lc_od_eje,lc_oi_esf,lc_oi_cil,lc_oi_eje,",
        "lc_radio_base_od,lc_radio_base_oi,lc_diametro_od,lc_diametro_oi,",
        "lc_laboratorio,lc_tipo_lente,lc_material,lc_fecha_adaptacion,lc_observaciones,",
        "diagnostico_od,diagnostico_oi,balance_od,balance_oi,",
        "otros_presbicia,otros_anisometropia,otros_ambliopia,",
        "auto_presbicia,auto_anisometropia,auto_ambliopia,",
        "plan_tratamiento,cita_estado,",
        "diagnostico,indicaciones,observaciones,proxima_cita"
      ].join("")
    )
    .eq("id", evaluacionId)
    .eq("optica_id", opticaId)
    .eq("paciente_id", pacienteId)
    .maybeSingle();
  assertNoDbError(error, "Evaluación por ID");
  if (!data) return null;
  return data as unknown as EvaluacionEditRow;
}

export async function fetchDispensacionesPorPaciente(
  supabase: SupabaseClient,
  opticaId: string,
  pacienteId: string
): Promise<DispensacionListRow[]> {
  const { data, error } = await supabase
    .from("dispensaciones")
    .select("id,fecha,monto_total,monto_pagado,estado_entrega,ot")
    .eq("optica_id", opticaId)
    .eq("paciente_id", pacienteId)
    .order("fecha", { ascending: false })
    .abortSignal(AbortSignal.timeout(12_000));

  assertNoDbError(error, "Dispensaciones del paciente");
  return (data ?? []) as DispensacionListRow[];
}

export async function fetchServiciosExtraPorPaciente(
  supabase: SupabaseClient,
  opticaId: string,
  pacienteId: string
): Promise<ServicioExtraListRow[]> {
  const { data, error } = await supabase
    .from("servicios_extra")
    .select("id,fecha,descripcion,monto_total,a_cuenta,estado,ot")
    .eq("optica_id", opticaId)
    .eq("paciente_id", pacienteId)
    .order("fecha", { ascending: false })
    .abortSignal(AbortSignal.timeout(12_000));

  assertNoDbError(error, "Servicios extra del paciente");
  return (data ?? []) as ServicioExtraListRow[];
}

export function formatFormulaResumida(e: EvaluacionCardRow): string {
  const od = [
    e.receta_od_esf?.trim(),
    e.receta_od_cil?.trim(),
    e.receta_od_eje?.trim()
  ]
    .filter(Boolean)
    .join(" / ");
  const oi = [
    e.receta_oi_esf?.trim(),
    e.receta_oi_cil?.trim(),
    e.receta_oi_eje?.trim()
  ]
    .filter(Boolean)
    .join(" / ");
  if (!od && !oi) return "—";
  return `OD ${od || "—"} · OI ${oi || "—"}`;
}
