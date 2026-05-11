import { notFound, redirect } from "next/navigation";

import {
  updateEvaluacionAction
} from "@/app/pacientes/[id]/evaluaciones/nueva/actions";
import { NuevaEvaluacionForm } from "@/components/pacientes/nueva-evaluacion-form";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { fetchEvaluacionById } from "@/lib/paciente-clinico";
import { localTodayDateOnly } from "@/lib/pacientes";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

export default async function EditarEvaluacionPage({
  params,
  searchParams
}: {
  params: Promise<{ id: string; evalId: string }>;
  searchParams: Promise<{ error?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/pacientes");

  const { id: pacienteId, evalId } = await params;
  const query = await searchParams;
  const supabase = await createClient();
  const [fiscal, row] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    fetchEvaluacionById(supabase, activeOptica.opticaId, pacienteId, evalId)
  ]);
  if (!row) notFound();

  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);
  const initialData = {
    fecha: row.fecha ?? localTodayDateOnly(),
    motivoConsulta: row.motivo_consulta ?? "",
    sintomasSignos: row.sintomas ?? "",
    antecedentesPersonalesOculares: row.antecedentes_personales_oculares ?? "",
    antecedentesPersonalesSistemicos: row.antecedentes_personales_sistemicos ?? "",
    antecedentesFamiliaresOculares: row.antecedentes_familiares_oculares ?? "",
    antecedentesFamiliaresSistemicos: row.antecedentes_familiares_sistemicos ?? "",
    medicacion: row.medicacion ?? "",
    alergias: row.alergias ?? "",
    necesidadVisual: row.necesidad_visual ?? "",
    avScAo: row.av_sc_ao ?? "",
    avScOdLejos: row.av_sc_od_lejos ?? "",
    avScOiLejos: row.av_sc_oi_lejos ?? "",
    avCcAoPx: row.av_cc_ao_px ?? "",
    avCcOdLejos: row.av_cc_od_lejos ?? "",
    avCcOiLejos: row.av_cc_oi_lejos ?? "",
    estereopsisValor: row.estereopsis_valor ?? "",
    estereopsisSegundos: row.estereopsis_segundos ?? "",
    lang: row.lang ?? "",
    worth: row.worth ?? "",
    ishihara: row.ishihara ?? "",
    farnsworth: row.farnsworth ?? "",
    schirmerOd: row.schirmer_od ?? "",
    schirmerOi: row.schirmer_oi ?? "",
    osdiPuntuacion: row.osdi_puntuacion != null ? String(row.osdi_puntuacion) : "",
    osdiClasificacion: row.osdi_clasificacion ?? "",
    sensibilidadContraste: row.sensibilidad_contraste ?? "",
    sensibilidadFrecuencia: row.sensibilidad_frecuencia ?? "",
    amsler: row.amsler ?? "",
    campoVisual: row.campo_visual ?? "",
    campoVisualDescripcion: row.campo_visual_descripcion ?? "",
    phOd: row.ph_od ?? "",
    phOi: row.ph_oi ?? "",
    kappaOd: row.kappa_od ?? "",
    kappaOi: row.kappa_oi ?? "",
    hirshberg: row.hirshberg ?? "",
    duccionesOd: row.ducciones_od ?? "",
    duccionesOi: row.ducciones_oi ?? "",
    versionesAo: row.versiones_ao ?? "",
    coverTest6m: row.cover_test_6m ?? "",
    coverTest40cm: row.cover_test_40cm ?? "",
    coverTest10cm: row.cover_test_10cm ?? "",
    ppcOr: row.ppc_or ?? "",
    ppcLuz: row.ppc_luz ?? "",
    ppcFrl: row.ppc_frl ?? "",
    reflejoFotomotor: row.reflejo_fotomotor ?? "",
    reflejoConsensual: row.reflejo_consensual ?? "",
    reflejoAcomodativo: row.reflejo_acomodativo ?? "",
    objOdEsf: row.obj_od_esf ?? "",
    objOdCil: row.obj_od_cil ?? "",
    objOdEje: row.obj_od_eje ?? "",
    objOiEsf: row.obj_oi_esf ?? "",
    objOiCil: row.obj_oi_cil ?? "",
    objOiEje: row.obj_oi_eje ?? "",
    subjOdEsf: row.subj_od_esf ?? "",
    subjOdCil: row.subj_od_cil ?? "",
    subjOdEje: row.subj_od_eje ?? "",
    subjOiEsf: row.subj_oi_esf ?? "",
    subjOiCil: row.subj_oi_cil ?? "",
    subjOiEje: row.subj_oi_eje ?? "",
    recetaOdEsf: row.receta_od_esf ?? "",
    recetaOdCil: row.receta_od_cil ?? "",
    recetaOdEje: row.receta_od_eje ?? "",
    recetaOdAv: row.receta_od_av ?? "",
    recetaOiEsf: row.receta_oi_esf ?? "",
    recetaOiCil: row.receta_oi_cil ?? "",
    recetaOiEje: row.receta_oi_eje ?? "",
    recetaOiAv: row.receta_oi_av ?? "",
    addCercaOd: row.add_cerca_od ?? "",
    addCercaOi: row.add_cerca_oi ?? "",
    addAv: row.add_av ?? "",
    isAddAo: "false",
    dipLejos: row.dip_lejos ?? "",
    dipCerca: row.dip_cerca ?? "",
    prismaOdValor: row.prisma_od_valor ?? "",
    prismaOdBase: row.prisma_od_base ?? "",
    prismaOiValor: row.prisma_oi_valor ?? "",
    prismaOiBase: row.prisma_oi_base ?? "",
    k1Od: row.k1_od ?? "",
    k2Od: row.k2_od ?? "",
    k1Oi: row.k1_oi ?? "",
    k2Oi: row.k2_oi ?? "",
    lcOdEsf: row.lc_od_esf ?? "",
    lcOdCil: row.lc_od_cil ?? "",
    lcOdEje: row.lc_od_eje ?? "",
    lcOiEsf: row.lc_oi_esf ?? "",
    lcOiCil: row.lc_oi_cil ?? "",
    lcOiEje: row.lc_oi_eje ?? "",
    lcRadioBaseOd: row.lc_radio_base_od ?? "",
    lcRadioBaseOi: row.lc_radio_base_oi ?? "",
    lcOdDia: row.lc_diametro_od ?? "",
    lcOiDia: row.lc_diametro_oi ?? "",
    lcLaboratorio: row.lc_laboratorio ?? "",
    lcTipoLente: row.lc_tipo_lente ?? "",
    lcMaterial: row.lc_material ?? "",
    lcFechaAdaptacion: row.lc_fecha_adaptacion ?? "",
    lcObservaciones: row.lc_observaciones ?? "",
    diagnosticoOd: row.diagnostico_od ?? "",
    diagnosticoOi: row.diagnostico_oi ?? "",
    balanceOd: row.balance_od ? "true" : "false",
    balanceOi: row.balance_oi ? "true" : "false",
    otrosPresbicia: row.otros_presbicia ? "true" : "false",
    otrosAnisometropia: row.otros_anisometropia ? "true" : "false",
    otrosAmbliopia: row.otros_ambliopia ? "true" : "false",
    autoPresbicia: row.auto_presbicia === false ? "false" : "true",
    autoAnisometropia: row.auto_anisometropia === false ? "false" : "true",
    autoAmbliopia: row.auto_ambliopia === false ? "false" : "true",
    planTratamiento: row.plan_tratamiento ?? "",
    citaEstado: row.cita_estado ?? "programada",
    diagnosticoResumen: row.diagnostico ?? "",
    indicaciones: row.indicaciones ?? "",
    proximaCita: row.proxima_cita ?? "",
    notasFinales: row.observaciones ?? ""
  };

  return (
    <div className="max-w-3xl">
      {query.error === "guardar_eval" && (
        <p className="mb-3 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          No se pudo guardar la evaluación. Verifica permisos o intenta nuevamente.
        </p>
      )}
      <NuevaEvaluacionForm
        pacienteId={pacienteId}
        evaluacionId={evalId}
        mode="edit"
        backHref={`/pacientes/${pacienteId}/evaluaciones`}
        opticaLine={opticaLine}
        todayDate={localTodayDateOnly()}
        initialData={initialData}
        saveEvaluacionAction={updateEvaluacionAction}
      />
    </div>
  );
}
