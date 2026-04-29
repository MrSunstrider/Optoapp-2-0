"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

type SaveEvaluacionAction = (formData: FormData) => void | Promise<void>;

type EvaluacionDraft = {
  fecha: string;
  motivoConsulta: string;
  sintomasSignos: string;
  antecedentesPersonalesOculares: string;
  antecedentesPersonalesSistemicos: string;
  antecedentesFamiliaresOculares: string;
  antecedentesFamiliaresSistemicos: string;
  medicacion: string;
  alergias: string;
  necesidadVisual: string;
  avScAo: string;
  avScOdLejos: string;
  avScOiLejos: string;
  avCcAoPx: string;
  avCcOdLejos: string;
  avCcOiLejos: string;
  estereopsisValor: string;
  estereopsisSegundos: string;
  lang: string;
  worth: string;
  ishihara: string;
  farnsworth: string;
  schirmerOd: string;
  schirmerOi: string;
  osdiPuntuacion: string;
  osdiClasificacion: string;
  sensibilidadContraste: string;
  sensibilidadFrecuencia: string;
  amsler: string;
  campoVisual: string;
  campoVisualDescripcion: string;
  phOd: string;
  phOi: string;
  kappaOd: string;
  kappaOi: string;
  hirshberg: string;
  duccionesOd: string;
  duccionesOi: string;
  versionesAo: string;
  coverTest6m: string;
  coverTest40cm: string;
  coverTest10cm: string;
  ppcOr: string;
  ppcLuz: string;
  ppcFrl: string;
  reflejoFotomotor: string;
  reflejoConsensual: string;
  reflejoAcomodativo: string;
  objOdEsf: string;
  objOdCil: string;
  objOdEje: string;
  objOiEsf: string;
  objOiCil: string;
  objOiEje: string;
  subjOdEsf: string;
  subjOdCil: string;
  subjOdEje: string;
  subjOiEsf: string;
  subjOiCil: string;
  subjOiEje: string;
  recetaOdEsf: string;
  recetaOdCil: string;
  recetaOdEje: string;
  recetaOdAv: string;
  recetaOiEsf: string;
  recetaOiCil: string;
  recetaOiEje: string;
  recetaOiAv: string;
  addCercaOd: string;
  addCercaOi: string;
  addAv: string;
  isAddAo: string;
  dipLejos: string;
  dipCerca: string;
  prismaOdValor: string;
  prismaOdBase: string;
  prismaOiValor: string;
  prismaOiBase: string;
  k1Od: string;
  k2Od: string;
  k1Oi: string;
  k2Oi: string;
  lcOdEsf: string;
  lcOdCil: string;
  lcOdEje: string;
  lcOiEsf: string;
  lcOiCil: string;
  lcOiEje: string;
  lcRadioBaseOd: string;
  lcRadioBaseOi: string;
  lcOdDia: string;
  lcOiDia: string;
  lcLaboratorio: string;
  lcTipoLente: string;
  lcMaterial: string;
  lcFechaAdaptacion: string;
  lcObservaciones: string;
  contactologia: string;
  diagnosticoOd: string;
  diagnosticoOi: string;
  balanceOd: string;
  balanceOi: string;
  otrosPresbicia: string;
  otrosAnisometropia: string;
  otrosAmbliopia: string;
  autoPresbicia: string;
  autoAnisometropia: string;
  autoAmbliopia: string;
  planTratamiento: string;
  citaEstado: string;
  diagnosticoResumen: string;
  indicaciones: string;
  proximaCita: string;
  notasFinales: string;
};

const TAB_ITEMS = [
  "Anamnesis",
  "Examen Visual",
  "Refraccion",
  "Contactologia",
  "Cierre"
] as const;
const ESTEREOPSIS_OPTIONS = ["Normal", "Reducida", "Ausente"] as const;
const LANG_OPTIONS = ["Positivo", "Negativo"] as const;
const WORTH_OPTIONS = ["Fusion normal", "Supresion OD", "Supresion OI", "Diplopia"] as const;
const FARNSWORTH_OPTIONS = ["Normal", "Deutan", "Protan", "Tritan"] as const;
const SENSIBILIDAD_OPTIONS = ["Normal", "Disminuida"] as const;
const CAMPO_VISUAL_OPTIONS = ["Normal", "Anomalia detectada"] as const;
const BASES_PRISMA = ["Nasal", "Temporal", "Superior", "Inferior"] as const;

type TabItem = (typeof TAB_ITEMS)[number];

function toEmptyDraft(todayDate: string): EvaluacionDraft {
  return {
    fecha: todayDate,
    motivoConsulta: "",
    sintomasSignos: "",
    antecedentesPersonalesOculares: "",
    antecedentesPersonalesSistemicos: "",
    antecedentesFamiliaresOculares: "",
    antecedentesFamiliaresSistemicos: "",
    medicacion: "",
    alergias: "",
    necesidadVisual: "",
    avScAo: "",
    avScOdLejos: "",
    avScOiLejos: "",
    avCcAoPx: "",
    avCcOdLejos: "",
    avCcOiLejos: "",
    estereopsisValor: "",
    estereopsisSegundos: "",
    lang: "",
    worth: "",
    ishihara: "",
    farnsworth: "",
    schirmerOd: "",
    schirmerOi: "",
    osdiPuntuacion: "",
    osdiClasificacion: "",
    sensibilidadContraste: "",
    sensibilidadFrecuencia: "",
    amsler: "",
    campoVisual: "",
    campoVisualDescripcion: "",
    phOd: "",
    phOi: "",
    kappaOd: "",
    kappaOi: "",
    hirshberg: "",
    duccionesOd: "",
    duccionesOi: "",
    versionesAo: "",
    coverTest6m: "",
    coverTest40cm: "",
    coverTest10cm: "",
    ppcOr: "",
    ppcLuz: "",
    ppcFrl: "",
    reflejoFotomotor: "",
    reflejoConsensual: "",
    reflejoAcomodativo: "",
    objOdEsf: "",
    objOdCil: "",
    objOdEje: "",
    objOiEsf: "",
    objOiCil: "",
    objOiEje: "",
    subjOdEsf: "",
    subjOdCil: "",
    subjOdEje: "",
    subjOiEsf: "",
    subjOiCil: "",
    subjOiEje: "",
    recetaOdEsf: "",
    recetaOdCil: "",
    recetaOdEje: "",
    recetaOdAv: "",
    recetaOiEsf: "",
    recetaOiCil: "",
    recetaOiEje: "",
    recetaOiAv: "",
    addCercaOd: "",
    addCercaOi: "",
    addAv: "",
    isAddAo: "false",
    dipLejos: "",
    dipCerca: "",
    prismaOdValor: "",
    prismaOdBase: "",
    prismaOiValor: "",
    prismaOiBase: "",
    k1Od: "",
    k2Od: "",
    k1Oi: "",
    k2Oi: "",
    lcOdEsf: "",
    lcOdCil: "",
    lcOdEje: "",
    lcOiEsf: "",
    lcOiCil: "",
    lcOiEje: "",
    lcRadioBaseOd: "",
    lcRadioBaseOi: "",
    lcOdDia: "",
    lcOiDia: "",
    lcLaboratorio: "",
    lcTipoLente: "",
    lcMaterial: "",
    lcFechaAdaptacion: "",
    lcObservaciones: "",
    contactologia: "",
    diagnosticoOd: "",
    diagnosticoOi: "",
    balanceOd: "false",
    balanceOi: "false",
    otrosPresbicia: "false",
    otrosAnisometropia: "false",
    otrosAmbliopia: "false",
    autoPresbicia: "true",
    autoAnisometropia: "true",
    autoAmbliopia: "true",
    planTratamiento: "",
    citaEstado: "programada",
    diagnosticoResumen: "",
    indicaciones: "",
    proximaCita: "",
    notasFinales: ""
  };
}

type Props = {
  pacienteId: string;
  evaluacionId?: string;
  mode?: "create" | "edit";
  backHref: string;
  opticaLine: string;
  todayDate: string;
  initialData?: Partial<EvaluacionDraft>;
  saveEvaluacionAction: SaveEvaluacionAction;
};

export function NuevaEvaluacionForm({
  pacienteId,
  evaluacionId,
  mode = "create",
  backHref,
  opticaLine,
  todayDate,
  initialData,
  saveEvaluacionAction
}: Props) {
  const storageKey = useMemo(
    () => `optoapp:web:evaluacion-form:${mode}:${pacienteId}:${evaluacionId ?? "new"}`,
    [mode, pacienteId, evaluacionId]
  );
  const [activeTab, setActiveTab] = useState<TabItem>("Anamnesis");
  const [warnEmptyMotivo, setWarnEmptyMotivo] = useState(false);
  const [draft, setDraft] = useState<EvaluacionDraft>({
    ...toEmptyDraft(todayDate),
    ...(initialData ?? {})
  });
  const [osdiOpen, setOsdiOpen] = useState(false);

  useEffect(() => {
    const base = { ...toEmptyDraft(todayDate), ...(initialData ?? {}) };
    const raw = window.localStorage.getItem(storageKey);
    const rawTab = window.localStorage.getItem(`${storageKey}:tab`);
    if (rawTab && TAB_ITEMS.includes(rawTab as TabItem)) {
      setActiveTab(rawTab as TabItem);
    } else {
      setActiveTab("Anamnesis");
    }
    if (!raw) {
      setDraft(base);
      return;
    }
    try {
      const parsed = JSON.parse(raw) as Partial<EvaluacionDraft>;
      setDraft({ ...base, ...parsed });
    } catch {
      setDraft(base);
    }
  }, [initialData, storageKey, todayDate]);

  useEffect(() => {
    window.localStorage.setItem(storageKey, JSON.stringify(draft));
  }, [draft, storageKey]);

  useEffect(() => {
    window.localStorage.setItem(`${storageKey}:tab`, activeTab);
  }, [activeTab, storageKey]);

  useEffect(() => {
    const diagOd = autoDiagEye(
      draft.recetaOdEsf,
      draft.recetaOdCil,
      draft.recetaOdEje,
      draft.balanceOd === "true"
    );
    const diagOi = autoDiagEye(
      draft.recetaOiEsf,
      draft.recetaOiCil,
      draft.recetaOiEje,
      draft.balanceOi === "true"
    );
    const isAddAo = draft.isAddAo === "true";
    const syncedAddOi = isAddAo ? draft.addCercaOd : draft.addCercaOi;
    const presbAuto = autoPresbiciaValue(draft.addCercaOd, syncedAddOi);
    const anisoAuto = autoAnisometropiaValue(
      draft.recetaOdEsf,
      draft.recetaOdCil,
      draft.recetaOiEsf,
      draft.recetaOiCil,
      draft.balanceOd === "true" ||
        hasBalanceText(draft.recetaOdEsf, draft.recetaOdCil, draft.recetaOdEje),
      draft.balanceOi === "true" ||
        hasBalanceText(draft.recetaOiEsf, draft.recetaOiCil, draft.recetaOiEje)
    );
    const ambliAuto = autoAmbliopiaValue(draft.avCcOdLejos, draft.avCcOiLejos);

    setDraft((prev) => {
      const next = { ...prev };
      if (diagOd !== prev.diagnosticoOd) next.diagnosticoOd = diagOd;
      if (diagOi !== prev.diagnosticoOi) next.diagnosticoOi = diagOi;
      if (isAddAo && prev.addCercaOi !== prev.addCercaOd) next.addCercaOi = prev.addCercaOd;
      if (prev.autoPresbicia === "true") next.otrosPresbicia = String(presbAuto);
      if (prev.autoAnisometropia === "true") next.otrosAnisometropia = String(anisoAuto);
      if (prev.autoAmbliopia === "true" && ambliAuto !== null) {
        next.otrosAmbliopia = String(ambliAuto);
      }
      const resumen = [diagOd, diagOi]
        .filter(Boolean)
        .map((d, i) => `${i === 0 ? "OD" : "OI"}: ${d}`)
        .join(" | ");
      if (next.diagnosticoResumen !== resumen) next.diagnosticoResumen = resumen;
      return next;
    });
  }, [
    draft.recetaOdEsf,
    draft.recetaOdCil,
    draft.recetaOdEje,
    draft.recetaOiEsf,
    draft.recetaOiCil,
    draft.recetaOiEje,
    draft.balanceOd,
    draft.balanceOi,
    draft.addCercaOd,
    draft.addCercaOi,
    draft.isAddAo,
    draft.avCcOdLejos,
    draft.avCcOiLejos,
    draft.autoPresbicia,
    draft.autoAnisometropia,
    draft.autoAmbliopia
  ]);

  function update<K extends keyof EvaluacionDraft>(key: K, value: EvaluacionDraft[K]) {
    setDraft((prev) => ({ ...prev, [key]: value }));
  }

  function clearDraft() {
    window.localStorage.removeItem(storageKey);
    window.localStorage.removeItem(`${storageKey}:tab`);
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    if (draft.motivoConsulta.trim().length > 0) {
      setWarnEmptyMotivo(false);
      clearDraft();
      return;
    }
    if (!warnEmptyMotivo) {
      event.preventDefault();
      setWarnEmptyMotivo(true);
      return;
    }
    clearDraft();
  }

  const tabBtnBase = "rounded-full border px-3 py-1.5 text-xs font-medium transition-colors";
  const inputCls = "mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2 text-zinc-100";

  const hiddenPairs: Array<[string, string]> = [
    ["pacienteId", pacienteId],
    ["evaluacionId", evaluacionId ?? ""],
    ["fecha", draft.fecha],
    ["motivoConsulta", draft.motivoConsulta],
    ["sintomasSignos", draft.sintomasSignos],
    ["antecedentesPersonalesOculares", draft.antecedentesPersonalesOculares],
    ["antecedentesPersonalesSistemicos", draft.antecedentesPersonalesSistemicos],
    ["antecedentesFamiliaresOculares", draft.antecedentesFamiliaresOculares],
    ["antecedentesFamiliaresSistemicos", draft.antecedentesFamiliaresSistemicos],
    ["medicacion", draft.medicacion],
    ["alergias", draft.alergias],
    ["necesidadVisual", draft.necesidadVisual],
    ["avScAo", draft.avScAo],
    ["avScOdLejos", draft.avScOdLejos],
    ["avScOiLejos", draft.avScOiLejos],
    ["avCcAoPx", draft.avCcAoPx],
    ["avCcOdLejos", draft.avCcOdLejos],
    ["avCcOiLejos", draft.avCcOiLejos],
    ["estereopsisValor", draft.estereopsisValor],
    ["estereopsisSegundos", draft.estereopsisSegundos],
    ["lang", draft.lang],
    ["worth", draft.worth],
    ["ishihara", draft.ishihara],
    ["farnsworth", draft.farnsworth],
    ["schirmerOd", draft.schirmerOd],
    ["schirmerOi", draft.schirmerOi],
    ["osdiPuntuacion", draft.osdiPuntuacion],
    ["osdiClasificacion", draft.osdiClasificacion],
    ["sensibilidadContraste", draft.sensibilidadContraste],
    ["sensibilidadFrecuencia", draft.sensibilidadFrecuencia],
    ["amsler", draft.amsler],
    ["campoVisual", draft.campoVisual],
    ["campoVisualDescripcion", draft.campoVisualDescripcion],
    ["phOd", draft.phOd],
    ["phOi", draft.phOi],
    ["kappaOd", draft.kappaOd],
    ["kappaOi", draft.kappaOi],
    ["hirshberg", draft.hirshberg],
    ["duccionesOd", draft.duccionesOd],
    ["duccionesOi", draft.duccionesOi],
    ["versionesAo", draft.versionesAo],
    ["coverTest6m", draft.coverTest6m],
    ["coverTest40cm", draft.coverTest40cm],
    ["coverTest10cm", draft.coverTest10cm],
    ["ppcOr", draft.ppcOr],
    ["ppcLuz", draft.ppcLuz],
    ["ppcFrl", draft.ppcFrl],
    ["reflejoFotomotor", draft.reflejoFotomotor],
    ["reflejoConsensual", draft.reflejoConsensual],
    ["reflejoAcomodativo", draft.reflejoAcomodativo],
    ["objOdEsf", draft.objOdEsf],
    ["objOdCil", draft.objOdCil],
    ["objOdEje", draft.objOdEje],
    ["objOiEsf", draft.objOiEsf],
    ["objOiCil", draft.objOiCil],
    ["objOiEje", draft.objOiEje],
    ["subjOdEsf", draft.subjOdEsf],
    ["subjOdCil", draft.subjOdCil],
    ["subjOdEje", draft.subjOdEje],
    ["subjOiEsf", draft.subjOiEsf],
    ["subjOiCil", draft.subjOiCil],
    ["subjOiEje", draft.subjOiEje],
    ["recetaOdEsf", draft.recetaOdEsf],
    ["recetaOdCil", draft.recetaOdCil],
    ["recetaOdEje", draft.recetaOdEje],
    ["recetaOdAv", draft.recetaOdAv],
    ["recetaOiEsf", draft.recetaOiEsf],
    ["recetaOiCil", draft.recetaOiCil],
    ["recetaOiEje", draft.recetaOiEje],
    ["recetaOiAv", draft.recetaOiAv],
    ["addCercaOd", draft.addCercaOd],
    ["addCercaOi", draft.addCercaOi],
    ["addAv", draft.addAv],
    ["isAddAo", draft.isAddAo],
    ["dipLejos", draft.dipLejos],
    ["dipCerca", draft.dipCerca],
    ["prismaOdValor", draft.prismaOdValor],
    ["prismaOdBase", draft.prismaOdBase],
    ["prismaOiValor", draft.prismaOiValor],
    ["prismaOiBase", draft.prismaOiBase],
    ["k1Od", draft.k1Od],
    ["k2Od", draft.k2Od],
    ["k1Oi", draft.k1Oi],
    ["k2Oi", draft.k2Oi],
    ["lcOdEsf", draft.lcOdEsf],
    ["lcOdCil", draft.lcOdCil],
    ["lcOdEje", draft.lcOdEje],
    ["lcOiEsf", draft.lcOiEsf],
    ["lcOiCil", draft.lcOiCil],
    ["lcOiEje", draft.lcOiEje],
    ["lcRadioBaseOd", draft.lcRadioBaseOd],
    ["lcRadioBaseOi", draft.lcRadioBaseOi],
    ["lcOdDia", draft.lcOdDia],
    ["lcOiDia", draft.lcOiDia],
    ["lcLaboratorio", draft.lcLaboratorio],
    ["lcTipoLente", draft.lcTipoLente],
    ["lcMaterial", draft.lcMaterial],
    ["lcFechaAdaptacion", draft.lcFechaAdaptacion],
    ["lcObservaciones", draft.lcObservaciones],
    ["diagnosticoOd", draft.diagnosticoOd],
    ["diagnosticoOi", draft.diagnosticoOi],
    ["balanceOd", draft.balanceOd],
    ["balanceOi", draft.balanceOi],
    ["otrosPresbicia", draft.otrosPresbicia],
    ["otrosAnisometropia", draft.otrosAnisometropia],
    ["otrosAmbliopia", draft.otrosAmbliopia],
    ["autoPresbicia", draft.autoPresbicia],
    ["autoAnisometropia", draft.autoAnisometropia],
    ["autoAmbliopia", draft.autoAmbliopia],
    ["planTratamiento", draft.planTratamiento],
    ["citaEstado", draft.citaEstado],
    ["diagnosticoResumen", draft.diagnosticoResumen],
    ["indicaciones", draft.indicaciones],
    ["proximaCita", draft.proximaCita],
    ["notasFinales", draft.notasFinales]
  ];

  return (
    <form action={saveEvaluacionAction} onSubmit={handleSubmit} className="space-y-4">
      {hiddenPairs.map(([k, v]) => (
        <input key={k} type="hidden" name={k} value={v} />
      ))}

      <div className="rounded-xl border border-zinc-800 bg-[#121212] text-zinc-100">
        <div className="border-b border-zinc-800 px-3 py-2 text-[13px] text-sky-400">{opticaLine}</div>
        <div className="flex items-center gap-2 bg-sky-400 px-2 py-2 text-zinc-900">
          <Link href={backHref} className="flex h-10 w-10 items-center justify-center rounded-lg hover:bg-black/10" aria-label="Volver a evaluaciones"><span className="text-xl font-semibold">{"<"}</span></Link>
          <h2 className="flex-1 text-lg font-bold">{mode === "edit" ? "Editar Evaluacion" : "Nueva Evaluacion"}</h2>
          <button type="submit" className="flex h-10 w-10 items-center justify-center rounded-lg text-xl hover:bg-black/10" title="Guardar evaluacion" aria-label="Guardar evaluacion">?</button>
        </div>

        <div className="flex flex-wrap gap-2 border-b border-zinc-800 px-3 py-3">
          {TAB_ITEMS.map((item) => (
            <button key={item} type="button" onClick={() => setActiveTab(item)} className={tabBtnBase + (activeTab === item ? " border-sky-400 bg-sky-400/20 text-sky-300" : " border-zinc-700 bg-zinc-900 text-zinc-400")}>{item}</button>
          ))}
        </div>

        <div className="space-y-4 px-4 py-4">
          {warnEmptyMotivo && <p className="rounded-lg border border-amber-900/60 bg-amber-950/40 px-3 py-2 text-sm text-amber-300">Motivo de consulta esta vacio. Presiona guardar otra vez si quieres continuar igual.</p>}

          {activeTab === "Anamnesis" && (
            <>
              <label className="block text-sm text-zinc-300">Fecha de registro<input type="date" value={draft.fecha} onChange={(e) => update("fecha", e.target.value)} className={inputCls} /></label>
              <label className="block text-sm text-zinc-300">Motivo de consulta<textarea value={draft.motivoConsulta} onChange={(e) => update("motivoConsulta", e.target.value)} rows={3} className={inputCls + " placeholder:text-zinc-500"} /></label>
              <label className="block text-sm text-zinc-300">Sintomas y signos<textarea value={draft.sintomasSignos} onChange={(e) => update("sintomasSignos", e.target.value)} rows={4} className={inputCls + " placeholder:text-zinc-500"} /></label>
              <div className="space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-3">
                <p className="text-sm font-medium text-zinc-300">Antecedentes</p>
                <label className="block text-sm text-zinc-400">Personales oculares<textarea value={draft.antecedentesPersonalesOculares} onChange={(e) => update("antecedentesPersonalesOculares", e.target.value)} rows={2} className={inputCls} /></label>
                <label className="block text-sm text-zinc-400">Personales sistemicos<textarea value={draft.antecedentesPersonalesSistemicos} onChange={(e) => update("antecedentesPersonalesSistemicos", e.target.value)} rows={2} className={inputCls} /></label>
                <label className="block text-sm text-zinc-400">Familiares oculares<textarea value={draft.antecedentesFamiliaresOculares} onChange={(e) => update("antecedentesFamiliaresOculares", e.target.value)} rows={2} className={inputCls} /></label>
                <label className="block text-sm text-zinc-400">Familiares sistemicos<textarea value={draft.antecedentesFamiliaresSistemicos} onChange={(e) => update("antecedentesFamiliaresSistemicos", e.target.value)} rows={2} className={inputCls} /></label>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  <label className="text-sm text-zinc-400">Medicacion<input value={draft.medicacion} onChange={(e) => update("medicacion", e.target.value)} className={inputCls} /></label>
                  <label className="text-sm text-zinc-400">Alergias<input value={draft.alergias} onChange={(e) => update("alergias", e.target.value)} className={inputCls} /></label>
                </div>
                <label className="block text-sm text-zinc-400">Necesidad visual<textarea value={draft.necesidadVisual} onChange={(e) => update("necesidadVisual", e.target.value)} rows={2} className={inputCls} placeholder="Ej: Computadora, Conducir, Lectura prolongada" /></label>
              </div>
            </>
          )}

          {activeTab === "Examen Visual" && (
            <ExamenVisualTab draft={draft} update={update} onOpenOsdi={() => setOsdiOpen(true)} inputCls={inputCls} />
          )}

          {activeTab === "Refraccion" && (
            <RefraccionTab draft={draft} update={update} inputCls={inputCls} />
          )}
          {activeTab === "Contactologia" && (
            <ContactologiaTab draft={draft} update={update} inputCls={inputCls} />
          )}
          {activeTab === "Cierre" && (
            <CierreTab draft={draft} update={update} inputCls={inputCls} mode={mode} />
          )}
        </div>
      </div>

      {osdiOpen && <OsdiDialog onClose={() => setOsdiOpen(false)} onSave={(s, c) => { update("osdiPuntuacion", String(s)); update("osdiClasificacion", c); setOsdiOpen(false); }} />}
    </form>
  );
}

function normalizeRx(esf: string, cil: string, eje: string): {
  esf: string;
  cil: string;
  eje: string;
} {
  const cleanEsf = esf.trim().replace(",", ".");
  const cleanCil = cil.trim().replace(",", ".");
  const cleanEje = eje.trim();
  const esfN = Number(cleanEsf);
  const cilN = Number(cleanCil);
  const ejeN = Number(cleanEje);
  if (!Number.isFinite(esfN) || !Number.isFinite(cilN) || !Number.isFinite(ejeN)) {
    return { esf: esf.trim(), cil: cil.trim(), eje: eje.trim() };
  }
  let nextEsf = esfN;
  let nextCil = cilN;
  let nextEje = ejeN;
  if (nextCil > 0) {
    nextEsf = nextEsf + nextCil;
    nextCil = -nextCil;
    nextEje = nextEje + 90;
  }
  while (nextEje > 180) nextEje -= 180;
  while (nextEje <= 0) nextEje += 180;
  return {
    esf: nextEsf.toFixed(2).replace(/\.00$/, ""),
    cil: nextCil.toFixed(2).replace(/\.00$/, ""),
    eje: String(Math.round(nextEje))
  };
}

function RefraccionTab({
  draft,
  update,
  inputCls
}: {
  draft: EvaluacionDraft;
  update: <K extends keyof EvaluacionDraft>(key: K, value: EvaluacionDraft[K]) => void;
  inputCls: string;
}) {
  const cardCls = "space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-3";
  const subtitle = "text-sm font-semibold text-sky-300";
  const addAo = draft.isAddAo === "true";

  function normalizeEye(side: "od" | "oi") {
    if (side === "od") {
      const n = normalizeRx(draft.recetaOdEsf, draft.recetaOdCil, draft.recetaOdEje);
      update("recetaOdEsf", n.esf);
      update("recetaOdCil", n.cil);
      update("recetaOdEje", n.eje);
      return;
    }
    const n = normalizeRx(draft.recetaOiEsf, draft.recetaOiCil, draft.recetaOiEje);
    update("recetaOiEsf", n.esf);
    update("recetaOiCil", n.cil);
    update("recetaOiEje", n.eje);
  }

  return (
    <div className="space-y-4">
      <div className={cardCls}>
        <p className={subtitle}>Refraccion Objetiva</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">OD Esf<input value={draft.objOdEsf} onChange={(e) => update("objOdEsf", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OD Cil<input value={draft.objOdCil} onChange={(e) => update("objOdCil", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OD Eje<input value={draft.objOdEje} onChange={(e) => update("objOdEje", e.target.value)} className={inputCls} /></label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">OI Esf<input value={draft.objOiEsf} onChange={(e) => update("objOiEsf", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OI Cil<input value={draft.objOiCil} onChange={(e) => update("objOiCil", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OI Eje<input value={draft.objOiEje} onChange={(e) => update("objOiEje", e.target.value)} className={inputCls} /></label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Refraccion Subjetiva</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">OD Esf<input value={draft.subjOdEsf} onChange={(e) => update("subjOdEsf", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OD Cil<input value={draft.subjOdCil} onChange={(e) => update("subjOdCil", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OD Eje<input value={draft.subjOdEje} onChange={(e) => update("subjOdEje", e.target.value)} className={inputCls} /></label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">OI Esf<input value={draft.subjOiEsf} onChange={(e) => update("subjOiEsf", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OI Cil<input value={draft.subjOiCil} onChange={(e) => update("subjOiCil", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OI Eje<input value={draft.subjOiEje} onChange={(e) => update("subjOiEje", e.target.value)} className={inputCls} /></label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Formula final (gafas)</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">OD Esf<input value={draft.recetaOdEsf} onChange={(e) => update("recetaOdEsf", e.target.value)} onBlur={() => normalizeEye("od")} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OD Cil<input value={draft.recetaOdCil} onChange={(e) => update("recetaOdCil", e.target.value)} onBlur={() => normalizeEye("od")} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OD Eje<input value={draft.recetaOdEje} onChange={(e) => update("recetaOdEje", e.target.value)} onBlur={() => normalizeEye("od")} className={inputCls} /></label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">OI Esf<input value={draft.recetaOiEsf} onChange={(e) => update("recetaOiEsf", e.target.value)} onBlur={() => normalizeEye("oi")} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OI Cil<input value={draft.recetaOiCil} onChange={(e) => update("recetaOiCil", e.target.value)} onBlur={() => normalizeEye("oi")} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OI Eje<input value={draft.recetaOiEje} onChange={(e) => update("recetaOiEje", e.target.value)} onBlur={() => normalizeEye("oi")} className={inputCls} /></label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">AV OD<input value={draft.recetaOdAv} onChange={(e) => update("recetaOdAv", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">AV OI<input value={draft.recetaOiAv} onChange={(e) => update("recetaOiAv", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">AV AO<input value={draft.avCcAoPx} onChange={(e) => update("avCcAoPx", e.target.value)} className={inputCls} /></label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Adicion (ADD)</p>
        <label className="flex items-center gap-2 text-sm text-zinc-300">
          <input type="checkbox" checked={addAo} onChange={(e) => update("isAddAo", e.target.checked ? "true" : "false")} />
          A/O
        </label>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">Add OD<input value={draft.addCercaOd} onChange={(e) => update("addCercaOd", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">Add OI<input value={draft.addCercaOi} onChange={(e) => update("addCercaOi", e.target.value)} disabled={addAo} className={inputCls + (addAo ? " opacity-50" : "")} /></label>
          <label className="text-sm text-zinc-300">AV Add<input value={draft.addAv} onChange={(e) => update("addAv", e.target.value)} className={inputCls} /></label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>DIP</p>
        <p className="text-xs text-zinc-500">Puedes ingresar DIP total o DNP OD/OI segun disponibilidad.</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">DIP Lejos<input value={draft.dipLejos} onChange={(e) => update("dipLejos", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">DIP Cerca<input value={draft.dipCerca} onChange={(e) => update("dipCerca", e.target.value)} className={inputCls} /></label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Prismas</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">Prisma OD (valor)<input value={draft.prismaOdValor} onChange={(e) => update("prismaOdValor", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">Base OD<select value={draft.prismaOdBase} onChange={(e) => update("prismaOdBase", e.target.value)} className={inputCls}><option value="">Seleccionar...</option>{BASES_PRISMA.map((x) => <option key={x} value={x}>{x}</option>)}</select></label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">Prisma OI (valor)<input value={draft.prismaOiValor} onChange={(e) => update("prismaOiValor", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">Base OI<select value={draft.prismaOiBase} onChange={(e) => update("prismaOiBase", e.target.value)} className={inputCls}><option value="">Seleccionar...</option>{BASES_PRISMA.map((x) => <option key={x} value={x}>{x}</option>)}</select></label>
        </div>
      </div>
    </div>
  );
}

const DIAG_OPTIONS = [
  "Emetropia",
  "Miopia",
  "Hipermetropia",
  "Astigmatismo miopico simple",
  "Astigmatismo miopico compuesto",
  "Astigmatismo hipermetropico simple",
  "Astigmatismo hipermetropico compuesto",
  "Astigmatismo mixto",
  "Balance"
] as const;

const CITA_ESTADOS: Array<{ code: string; label: string }> = [
  { code: "programada", label: "Programada" },
  { code: "confirmada", label: "Confirmada" },
  { code: "asistio", label: "Asistio" },
  { code: "no_asistio", label: "No asistio" },
  { code: "reprogramada", label: "Reprogramada" }
];

function CierreTab({
  draft,
  update,
  inputCls,
  mode
}: {
  draft: EvaluacionDraft;
  update: <K extends keyof EvaluacionDraft>(key: K, value: EvaluacionDraft[K]) => void;
  inputCls: string;
  mode: "create" | "edit";
}) {
  const cardCls = "space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-3";
  const hasProxCita = draft.proximaCita.trim() !== "";

  function toggleManual(flagKey: "autoPresbicia" | "autoAnisometropia" | "autoAmbliopia") {
    update(flagKey, draft[flagKey] === "true" ? "false" : "true");
  }

  function toggleManualCheck(
    flagKey: "autoPresbicia" | "autoAnisometropia" | "autoAmbliopia",
    valueKey: "otrosPresbicia" | "otrosAnisometropia" | "otrosAmbliopia",
    checked: boolean
  ) {
    update(flagKey, "false");
    update(valueKey, String(checked));
  }

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold text-sky-300">Diagnostico y Plan</h3>

      <div className={cardCls}>
        <p className="text-base font-semibold text-zinc-100">Diagnostico</p>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_auto] sm:items-end">
          <label className="text-sm text-zinc-300">
            Diagnostico OD
            <select value={draft.diagnosticoOd} onChange={(e) => update("diagnosticoOd", e.target.value)} className={inputCls}>
              <option value="">Seleccionar...</option>
              {DIAG_OPTIONS.map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>
          </label>
          <label className="flex items-center gap-2 pb-2 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={draft.balanceOd === "true"}
              onChange={(e) => update("balanceOd", e.target.checked ? "true" : "false")}
            />
            Balance
          </label>
        </div>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_auto] sm:items-end">
          <label className="text-sm text-zinc-300">
            Diagnostico OI
            <select value={draft.diagnosticoOi} onChange={(e) => update("diagnosticoOi", e.target.value)} className={inputCls}>
              <option value="">Seleccionar...</option>
              {DIAG_OPTIONS.map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>
          </label>
          <label className="flex items-center gap-2 pb-2 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={draft.balanceOi === "true"}
              onChange={(e) => update("balanceOi", e.target.checked ? "true" : "false")}
            />
            Balance
          </label>
        </div>

        <hr className="border-zinc-800" />
        <p className="text-sm font-semibold text-zinc-200">Otros Diagnosticos</p>

        <div className="flex items-center justify-between gap-3">
          <label className="flex items-center gap-2 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={draft.otrosPresbicia === "true"}
              onChange={(e) => toggleManualCheck("autoPresbicia", "otrosPresbicia", e.target.checked)}
            />
            Presbicia
          </label>
          <button
            type="button"
            onClick={() => toggleManual("autoPresbicia")}
            className="rounded-md border border-zinc-600 px-3 py-1 text-xs text-zinc-300"
          >
            {draft.autoPresbicia === "true" ? "Auto" : "Man"}
          </button>
        </div>
        <div className="flex items-center justify-between gap-3">
          <label className="flex items-center gap-2 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={draft.otrosAnisometropia === "true"}
              onChange={(e) =>
                toggleManualCheck("autoAnisometropia", "otrosAnisometropia", e.target.checked)
              }
            />
            Anisometropia
          </label>
          <button
            type="button"
            onClick={() => toggleManual("autoAnisometropia")}
            className="rounded-md border border-zinc-600 px-3 py-1 text-xs text-zinc-300"
          >
            {draft.autoAnisometropia === "true" ? "Auto" : "Man"}
          </button>
        </div>
        <div className="flex items-center justify-between gap-3">
          <label className="flex items-center gap-2 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={draft.otrosAmbliopia === "true"}
              onChange={(e) => toggleManualCheck("autoAmbliopia", "otrosAmbliopia", e.target.checked)}
            />
            Ambliopia
          </label>
          <button
            type="button"
            onClick={() => toggleManual("autoAmbliopia")}
            className="rounded-md border border-zinc-600 px-3 py-1 text-xs text-zinc-300"
          >
            {draft.autoAmbliopia === "true" ? "Auto" : "Man"}
          </button>
        </div>
      </div>

      <div className={cardCls}>
        <p className="text-base font-semibold text-zinc-100">Tratamiento</p>
        <label className="block text-sm text-zinc-300">
          Plan de Tratamiento
          <textarea
            value={draft.planTratamiento}
            onChange={(e) => update("planTratamiento", e.target.value)}
            rows={3}
            className={inputCls}
          />
        </label>
        <label className="block text-sm text-zinc-300">
          Observaciones Adicionales Generales
          <textarea
            value={draft.notasFinales}
            onChange={(e) => update("notasFinales", e.target.value)}
            rows={4}
            className={inputCls}
          />
        </label>
      </div>

      <div className={cardCls}>
        <p className="text-base font-semibold text-zinc-100">Proxima cita</p>
        <label className="block text-sm text-zinc-300">
          {hasProxCita ? `Proxima Cita: ${draft.proximaCita}` : "Programar Proxima Cita"}
          <input
            type="date"
            value={draft.proximaCita}
            onChange={(e) => {
              const v = e.target.value;
              update("proximaCita", v);
              if (!v) update("citaEstado", "programada");
            }}
            className={inputCls}
          />
        </label>
        {hasProxCita && (
          <button
            type="button"
            onClick={() => {
              update("proximaCita", "");
              update("citaEstado", "programada");
            }}
            className="rounded-md border border-zinc-600 px-3 py-1 text-xs text-zinc-300"
          >
            Limpiar fecha
          </button>
        )}

        {hasProxCita && (
          <label className="block text-sm text-zinc-300">
            Estado de la cita
            <select
              value={draft.citaEstado || "programada"}
              onChange={(e) => update("citaEstado", e.target.value)}
              className={inputCls}
            >
              {CITA_ESTADOS.map((s) => (
                <option key={s.code} value={s.code}>
                  {s.label}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>

      <button
        type="submit"
        className="w-full rounded-lg bg-sky-400 px-4 py-2.5 text-sm font-semibold text-zinc-900"
      >
        {mode === "edit" ? "Actualizar Evaluacion" : "Guardar Evaluacion"}
      </button>
    </div>
  );
}

function parsePower(raw: string): number {
  const t = raw.trim().toLowerCase();
  if (!t) return 0;
  if (["plano", "pl", "neutro", "nt"].includes(t)) return 0;
  const n = Number(t.replace(",", "."));
  return Number.isFinite(n) ? n : 0;
}

function hasBalanceText(...values: string[]): boolean {
  return values.some((v) => v.toLowerCase().includes("bal"));
}

function classifyRefraccion(esfRaw: string, cilRaw: string): string {
  let esfera = parsePower(esfRaw);
  let cil = parsePower(cilRaw);
  if (cil > 0) {
    esfera = esfera + cil;
    cil = -cil;
  }
  const m1 = esfera;
  const m2 = esfera + cil;
  const z = 0.0001;
  const eq0 = (x: number) => Math.abs(x) < z;
  if (eq0(m1) && eq0(m2)) return "Emetropia";
  if (esfera < 0 && eq0(cil)) return "Miopia";
  if (esfera > 0 && eq0(cil)) return "Hipermetropia";
  if ((eq0(m1) && m2 < 0) || (eq0(m2) && m1 < 0)) return "Astigmatismo miopico simple";
  if ((eq0(m1) && m2 > 0) || (eq0(m2) && m1 > 0)) return "Astigmatismo hipermetropico simple";
  if (m1 < 0 && m2 < 0) return "Astigmatismo miopico compuesto";
  if (m1 > 0 && m2 > 0) return "Astigmatismo hipermetropico compuesto";
  if ((m1 > 0 && m2 < 0) || (m1 < 0 && m2 > 0)) return "Astigmatismo mixto";
  return "Astigmatismo mixto";
}

function autoDiagEye(
  esf: string,
  cil: string,
  eje: string,
  balanceChecked: boolean
): string {
  const effBalance = balanceChecked || hasBalanceText(esf, cil, eje);
  if (effBalance) return "Balance";
  const hasData = esf.trim().length > 0 || cil.trim().length > 0;
  if (!hasData) return "";
  return classifyRefraccion(esf, cil);
}

function autoPresbiciaValue(addOd: string, addOi: string): boolean {
  const src = addOd.trim() !== "" ? addOd : addOi;
  return parsePower(src) > 0;
}

function autoAnisometropiaValue(
  odEsfRaw: string,
  odCilRaw: string,
  oiEsfRaw: string,
  oiCilRaw: string,
  odBalance: boolean,
  oiBalance: boolean
): boolean {
  if (odBalance || oiBalance) return false;
  if (odEsfRaw.trim() === "" || oiEsfRaw.trim() === "") return false;
  const odEE = parsePower(odEsfRaw) + parsePower(odCilRaw) / 2;
  const oiEE = parsePower(oiEsfRaw) + parsePower(oiCilRaw) / 2;
  return Math.abs(odEE - oiEE) >= 2.0;
}

function snellenToLogMar(raw: string): number | null {
  const m = raw.trim().match(/(\d+)\s*\/\s*(\d+)/);
  if (!m) return null;
  const den = Number(m[2]);
  if (!Number.isFinite(den) || den <= 0) return null;
  // Paridad con app móvil: se usa 20/x para convertir a logMAR.
  const decimalAV = 20 / den;
  return -Math.log10(decimalAV);
}

function autoAmbliopiaValue(avOd: string, avOi: string): boolean | null {
  const lOd = snellenToLogMar(avOd);
  const lOi = snellenToLogMar(avOi);
  if (lOd == null || lOi == null) return null;
  return Math.abs(lOd - lOi) >= 0.19;
}

function roundQuarter(value: number): number {
  return Math.round(value * 4) / 4;
}

function vertexCompensate(power: number): number {
  const result = power / (1 - 0.012 * power);
  return roundQuarter(result);
}

function formatPower(value: number): string {
  return value.toFixed(2);
}

function buildLcSuggestion(k1: string, k2: string): string | null {
  const n1 = Number(k1.replace(",", "."));
  const n2 = Number(k2.replace(",", "."));
  if (!Number.isFinite(n1) || !Number.isFinite(n2)) return null;
  const diff = Math.abs(n1 - n2);
  if (diff >= 4) {
    return "Sugerencia: Lente RGP (rigido gas permeable) - Astigmatismo corneal alto.";
  }
  if (diff >= 2.5) {
    return "Sugerencia: Valorar RGP o lente torico blando - Astigmatismo corneal moderado.";
  }
  return "Sugerencia: Lente blando (esferico o torico segun refraccion) - Astigmatismo corneal bajo.";
}

function autoCalculo(esfStr: string, cilStr: string, recorteActivo: boolean): {
  esf: number;
  cil: number;
  warning: boolean;
} {
  const sph = parsePower(esfStr);
  const cyl = parsePower(cilStr);
  const canRecortar = Math.abs(cyl) <= Math.abs(sph) / 4;
  const recortar = recorteActivo && canRecortar;
  if (recortar) {
    const sphTemp = sph + cyl / 2;
    return { esf: vertexCompensate(sphTemp), cil: 0, warning: false };
  }
  return {
    esf: vertexCompensate(sph),
    cil: cyl,
    warning: recorteActivo && !canRecortar && cyl !== 0
  };
}

function ContactologiaTab({
  draft,
  update,
  inputCls
}: {
  draft: EvaluacionDraft;
  update: <K extends keyof EvaluacionDraft>(key: K, value: EvaluacionDraft[K]) => void;
  inputCls: string;
}) {
  const cardCls = "space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-3";
  const subtitle = "text-sm font-semibold text-sky-300";
  const [aplicarRecorteOd, setAplicarRecorteOd] = useState(false);
  const [aplicarRecorteOi, setAplicarRecorteOi] = useState(false);

  const odSug = buildLcSuggestion(draft.k1Od, draft.k2Od);
  const oiSug = buildLcSuggestion(draft.k1Oi, draft.k2Oi);
  const odAuto = autoCalculo(draft.recetaOdEsf, draft.recetaOdCil, aplicarRecorteOd);
  const oiAuto = autoCalculo(draft.recetaOiEsf, draft.recetaOiCil, aplicarRecorteOi);

  function cargarOd() {
    update("lcOdEsf", formatPower(odAuto.esf));
    update("lcOdCil", formatPower(odAuto.cil));
  }
  function cargarOi() {
    update("lcOiEsf", formatPower(oiAuto.esf));
    update("lcOiCil", formatPower(oiAuto.cil));
  }

  return (
    <div className="space-y-4">
      <div className={cardCls}>
        <p className={subtitle}>Queratometria</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">K1 OD<input value={draft.k1Od} onChange={(e) => update("k1Od", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">K2 OD<input value={draft.k2Od} onChange={(e) => update("k2Od", e.target.value)} className={inputCls} /></label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">K1 OI<input value={draft.k1Oi} onChange={(e) => update("k1Oi", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">K2 OI<input value={draft.k2Oi} onChange={(e) => update("k2Oi", e.target.value)} className={inputCls} /></label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Sugerencias y Calculos</p>
        {odSug && (
          <div className="rounded-lg border border-zinc-700 bg-zinc-950/60 p-3 text-sm text-zinc-300">
            <p className="font-medium text-sky-300">OD</p>
            <p>{odSug}</p>
            <p className="mt-1 text-xs text-zinc-500">
              Considerar tambien comodidad del paciente, estilo de vida y regularidad corneal.
            </p>
          </div>
        )}
        {oiSug && (
          <div className="rounded-lg border border-zinc-700 bg-zinc-950/60 p-3 text-sm text-zinc-300">
            <p className="font-medium text-sky-300">OI</p>
            <p>{oiSug}</p>
            <p className="mt-1 text-xs text-zinc-500">
              Considerar tambien comodidad del paciente, estilo de vida y regularidad corneal.
            </p>
          </div>
        )}

        <div className="rounded-lg border border-sky-900/50 bg-sky-950/20 p-3">
          <p className="font-medium text-sky-300">Auto-Calculo OD</p>
          <label className="mt-2 flex items-center gap-2 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={aplicarRecorteOd}
              onChange={(e) => setAplicarRecorteOd(e.target.checked)}
            />
            Aplicar recorte de cilindro
          </label>
          <p className="mt-2 text-sm text-zinc-200">
            Recomendacion LC: Esf {formatPower(odAuto.esf)}
            {odAuto.cil !== 0 ? ` / Cil ${formatPower(odAuto.cil)}` : ""}
          </p>
          {odAuto.warning && (
            <p className="mt-1 text-xs text-amber-300">
              El cilindro supera el limite para recorte, se requiere lente torica.
            </p>
          )}
          <button type="button" onClick={cargarOd} className="mt-2 rounded-lg bg-sky-400 px-4 py-1.5 text-sm font-medium text-zinc-900">
            Cargar
          </button>
        </div>

        <div className="rounded-lg border border-sky-900/50 bg-sky-950/20 p-3">
          <p className="font-medium text-sky-300">Auto-Calculo OI</p>
          <label className="mt-2 flex items-center gap-2 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={aplicarRecorteOi}
              onChange={(e) => setAplicarRecorteOi(e.target.checked)}
            />
            Aplicar recorte de cilindro
          </label>
          <p className="mt-2 text-sm text-zinc-200">
            Recomendacion LC: Esf {formatPower(oiAuto.esf)}
            {oiAuto.cil !== 0 ? ` / Cil ${formatPower(oiAuto.cil)}` : ""}
          </p>
          {oiAuto.warning && (
            <p className="mt-1 text-xs text-amber-300">
              El cilindro supera el limite para recorte, se requiere lente torica.
            </p>
          )}
          <button type="button" onClick={cargarOi} className="mt-2 rounded-lg bg-sky-400 px-4 py-1.5 text-sm font-medium text-zinc-900">
            Cargar
          </button>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Prueba / Adaptacion Final</p>
        <p className="text-sm font-medium text-zinc-300">Poder del Lente</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">LC OD Esf<input value={draft.lcOdEsf} onChange={(e) => update("lcOdEsf", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">LC OD Cil<input value={draft.lcOdCil} onChange={(e) => update("lcOdCil", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">LC OD Eje<input value={draft.lcOdEje} onChange={(e) => update("lcOdEje", e.target.value)} className={inputCls} /></label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">LC OI Esf<input value={draft.lcOiEsf} onChange={(e) => update("lcOiEsf", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">LC OI Cil<input value={draft.lcOiCil} onChange={(e) => update("lcOiCil", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">LC OI Eje<input value={draft.lcOiEje} onChange={(e) => update("lcOiEje", e.target.value)} className={inputCls} /></label>
        </div>

        <p className="pt-1 text-sm font-medium text-zinc-300">Parametros Fisicos</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">Curva Base (CB) OD<input value={draft.lcRadioBaseOd} onChange={(e) => update("lcRadioBaseOd", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">Curva Base (CB) OI<input value={draft.lcRadioBaseOi} onChange={(e) => update("lcRadioBaseOi", e.target.value)} className={inputCls} /></label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">DIA OD<input value={draft.lcOdDia} onChange={(e) => update("lcOdDia", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">DIA OI<input value={draft.lcOiDia} onChange={(e) => update("lcOiDia", e.target.value)} className={inputCls} /></label>
        </div>

        <label className="block text-sm text-zinc-300">Laboratorio / Marca<input value={draft.lcLaboratorio} onChange={(e) => update("lcLaboratorio", e.target.value)} className={inputCls} /></label>
        <label className="block text-sm text-zinc-300">
          Tipo de Lente
          <select value={draft.lcTipoLente} onChange={(e) => update("lcTipoLente", e.target.value)} className={inputCls}>
            <option value="">Seleccionar...</option>
            <option value="Blanda">Blanda</option>
            <option value="Rigida (RGP)">Rigida (RGP)</option>
            <option value="Torica">Torica</option>
            <option value="Multifocal">Multifocal</option>
            <option value="Cosmetica">Cosmetica</option>
          </select>
        </label>
        <label className="block text-sm text-zinc-300">
          Material
          <select value={draft.lcMaterial} onChange={(e) => update("lcMaterial", e.target.value)} className={inputCls}>
            <option value="">Seleccionar...</option>
            <option value="Hidrogel">Hidrogel</option>
            <option value="Silicona Hidrogel">Silicona Hidrogel</option>
            <option value="PMMA">PMMA</option>
            <option value="Gas Permeable">Gas Permeable</option>
          </select>
        </label>
        <label className="block text-sm text-zinc-300">
          Fecha Adaptacion
          <input type="date" value={draft.lcFechaAdaptacion} onChange={(e) => update("lcFechaAdaptacion", e.target.value)} className={inputCls} />
        </label>
        <label className="block text-sm text-zinc-300">
          Notas Contactologia
          <textarea value={draft.lcObservaciones} onChange={(e) => update("lcObservaciones", e.target.value)} rows={3} className={inputCls} />
        </label>
      </div>
    </div>
  );
}

function ExamenVisualTab({
  draft,
  update,
  onOpenOsdi,
  inputCls
}: {
  draft: EvaluacionDraft;
  update: <K extends keyof EvaluacionDraft>(key: K, value: EvaluacionDraft[K]) => void;
  onOpenOsdi: () => void;
  inputCls: string;
}) {
  const cardCls = "space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-3";
  const subtitle = "text-sm font-semibold text-sky-300";
  return (
    <div className="space-y-4">
      <div className={cardCls}>
        <p className={subtitle}>Agudeza Visual SIN correccion</p>
        <label className="block text-sm text-zinc-300">Ambos ojos<input value={draft.avScAo} onChange={(e) => update("avScAo", e.target.value)} className={inputCls} /></label>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">OD<input value={draft.avScOdLejos} onChange={(e) => update("avScOdLejos", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OI<input value={draft.avScOiLejos} onChange={(e) => update("avScOiLejos", e.target.value)} className={inputCls} /></label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Agudeza Visual CON correccion PX</p>
        <label className="block text-sm text-zinc-300">Ambos ojos<input value={draft.avCcAoPx} onChange={(e) => update("avCcAoPx", e.target.value)} className={inputCls} /></label>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">OD<input value={draft.avCcOdLejos} onChange={(e) => update("avCcOdLejos", e.target.value)} className={inputCls} /></label>
          <label className="text-sm text-zinc-300">OI<input value={draft.avCcOiLejos} onChange={(e) => update("avCcOiLejos", e.target.value)} className={inputCls} /></label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Vision Binocular y Percepcion</p>
        <label className="block text-sm text-zinc-300">Estereopsis<select value={draft.estereopsisValor} onChange={(e) => update("estereopsisValor", e.target.value)} className={inputCls}><option value="">Seleccionar...</option>{ESTEREOPSIS_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}</select></label>
        <label className="block text-sm text-zinc-300">Segundos de arco (opcional)<input value={draft.estereopsisSegundos} onChange={(e) => update("estereopsisSegundos", e.target.value)} className={inputCls} /></label>
        <label className="block text-sm text-zinc-300">Test de Lang<select value={draft.lang} onChange={(e) => update("lang", e.target.value)} className={inputCls}><option value="">Seleccionar...</option>{LANG_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}</select></label>
        <label className="block text-sm text-zinc-300">Test de Worth<select value={draft.worth} onChange={(e) => update("worth", e.target.value)} className={inputCls}><option value="">Seleccionar...</option>{WORTH_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}</select></label>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Percepcion del Color</p>
        <label className="block text-sm text-zinc-300">Test de Ishihara<input value={draft.ishihara} onChange={(e) => update("ishihara", e.target.value)} className={inputCls} /></label>
        <label className="block text-sm text-zinc-300">Test de Farnsworth<select value={draft.farnsworth} onChange={(e) => update("farnsworth", e.target.value)} className={inputCls}><option value="">Seleccionar...</option>{FARNSWORTH_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}</select></label>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Salud de la Superficie Ocular y Funcion Visual</p>
        <p className="text-sm font-medium text-zinc-300">Test de Schirmer (mm)</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2"><label className="text-sm text-zinc-300">OD<input value={draft.schirmerOd} onChange={(e) => update("schirmerOd", e.target.value)} className={inputCls} /></label><label className="text-sm text-zinc-300">OI<input value={draft.schirmerOi} onChange={(e) => update("schirmerOi", e.target.value)} className={inputCls} /></label></div>
        <p className="text-sm font-medium text-zinc-300">Test OSDI</p>
        <div className="flex flex-wrap items-center gap-3"><button type="button" onClick={onOpenOsdi} className="rounded-lg border border-zinc-600 px-3 py-2 text-sm hover:bg-zinc-800">Realizar test OSDI</button>{draft.osdiPuntuacion && <span className="text-sm font-semibold text-sky-300">{draft.osdiPuntuacion} - {draft.osdiClasificacion}</span>}</div>
        <label className="block text-sm text-zinc-300">Sensibilidad al contraste<select value={draft.sensibilidadContraste} onChange={(e) => update("sensibilidadContraste", e.target.value)} className={inputCls}><option value="">Seleccionar...</option>{SENSIBILIDAD_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}</select></label>
        <label className="block text-sm text-zinc-300">Frecuencia espacial (opcional)<input value={draft.sensibilidadFrecuencia} onChange={(e) => update("sensibilidadFrecuencia", e.target.value)} className={inputCls} /></label>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Otras Pruebas y Examenes Previos</p>
        <label className="block text-sm text-zinc-300">Test de Amsler<input value={draft.amsler} onChange={(e) => update("amsler", e.target.value)} className={inputCls} /></label>
        <label className="block text-sm text-zinc-300">Campo visual por confrontacion<select value={draft.campoVisual} onChange={(e) => update("campoVisual", e.target.value)} className={inputCls}><option value="">Seleccionar...</option>{CAMPO_VISUAL_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}</select></label>
        {draft.campoVisual === "Anomalia detectada" && <label className="block text-sm text-zinc-300">Descripcion de anomalia (Campo Visual)<input value={draft.campoVisualDescripcion} onChange={(e) => update("campoVisualDescripcion", e.target.value)} className={inputCls} /></label>}
        <hr className="border-zinc-800" />
        <p className="text-sm font-medium text-zinc-300">Examenes Previos</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2"><label className="text-sm text-zinc-300">PH OD<input value={draft.phOd} onChange={(e) => update("phOd", e.target.value)} className={inputCls} /></label><label className="text-sm text-zinc-300">PH OI<input value={draft.phOi} onChange={(e) => update("phOi", e.target.value)} className={inputCls} /></label></div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2"><label className="text-sm text-zinc-300">Kappa OD<input value={draft.kappaOd} onChange={(e) => update("kappaOd", e.target.value)} className={inputCls} /></label><label className="text-sm text-zinc-300">Kappa OI<input value={draft.kappaOi} onChange={(e) => update("kappaOi", e.target.value)} className={inputCls} /></label></div>
        <label className="block text-sm text-zinc-300">Hirshberg<input value={draft.hirshberg} onChange={(e) => update("hirshberg", e.target.value)} className={inputCls} /></label>
        <p className="text-sm font-medium text-zinc-300">Ducciones</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2"><label className="text-sm text-zinc-300">OD<input value={draft.duccionesOd} onChange={(e) => update("duccionesOd", e.target.value)} className={inputCls} /></label><label className="text-sm text-zinc-300">OI<input value={draft.duccionesOi} onChange={(e) => update("duccionesOi", e.target.value)} className={inputCls} /></label></div>
        <label className="block text-sm text-zinc-300">Versiones Ambos Ojos<input value={draft.versionesAo} onChange={(e) => update("versionesAo", e.target.value)} className={inputCls} /></label>
        <p className="text-sm font-medium text-zinc-300">Cover Test</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3"><label className="text-sm text-zinc-300">6m<input value={draft.coverTest6m} onChange={(e) => update("coverTest6m", e.target.value)} className={inputCls} /></label><label className="text-sm text-zinc-300">40cm<input value={draft.coverTest40cm} onChange={(e) => update("coverTest40cm", e.target.value)} className={inputCls} /></label><label className="text-sm text-zinc-300">10cm<input value={draft.coverTest10cm} onChange={(e) => update("coverTest10cm", e.target.value)} className={inputCls} /></label></div>
        <p className="text-sm font-medium text-zinc-300">PPC</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3"><label className="text-sm text-zinc-300">OR<input value={draft.ppcOr} onChange={(e) => update("ppcOr", e.target.value)} className={inputCls} /></label><label className="text-sm text-zinc-300">Luz<input value={draft.ppcLuz} onChange={(e) => update("ppcLuz", e.target.value)} className={inputCls} /></label><label className="text-sm text-zinc-300">FR + L<input value={draft.ppcFrl} onChange={(e) => update("ppcFrl", e.target.value)} className={inputCls} /></label></div>
        <p className="text-sm font-medium text-zinc-300">Reflejos Pupilares</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3"><label className="text-sm text-zinc-300">Fotomotor<input value={draft.reflejoFotomotor} onChange={(e) => update("reflejoFotomotor", e.target.value)} className={inputCls} /></label><label className="text-sm text-zinc-300">Consensual<input value={draft.reflejoConsensual} onChange={(e) => update("reflejoConsensual", e.target.value)} className={inputCls} /></label><label className="text-sm text-zinc-300">Acomodativo<input value={draft.reflejoAcomodativo} onChange={(e) => update("reflejoAcomodativo", e.target.value)} className={inputCls} /></label></div>
      </div>
    </div>
  );
}

function OsdiDialog({ onClose, onSave }: { onClose: () => void; onSave: (score: number, clasificacion: string) => void; }) {
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const options: Array<{ label: string; value: number }> = [
    { label: "Ninguna vez (0)", value: 0 },
    { label: "Algunas veces (1)", value: 1 },
    { label: "Mitad de las veces (2)", value: 2 },
    { label: "La mayoria de las veces (3)", value: 3 },
    { label: "Todo el tiempo (4)", value: 4 }
  ];
  const questions = [
    "Sensibilidad a la luz?",
    "Sensacion de arenilla o polvo?",
    "Dolor o ardor en los ojos?",
    "Vision borrosa o baja?",
    "Leer o mirar pantallas?",
    "Conducir de noche?",
    "Usar cajeros automaticos o leer senales?",
    "Ver television?",
    "Viento o aire acondicionado?",
    "Ambientes muy secos (calefaccion, aire acondicionado)?",
    "Ambientes con humo o contaminacion?",
    "Usar lentes de contacto (si aplica)?"
  ];

  const save = () => {
    const vals = Object.values(answers);
    if (vals.length === 0) return onClose();
    const score = Math.round((vals.reduce((a, b) => a + b, 0) * 25) / vals.length);
    const clasificacion = score <= 12 ? "Normal" : score <= 22 ? "Leve" : score <= 32 ? "Moderado" : "Severo";
    onSave(score, clasificacion);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-3">
      <div className="absolute inset-0" onClick={onClose} role="presentation" />
      <div className="relative z-10 max-h-[90vh] w-full max-w-2xl overflow-auto rounded-xl border border-zinc-700 bg-zinc-900 p-4 text-zinc-100">
        <h3 className="text-lg font-semibold text-sky-300">Cuestionario OSDI</h3>
        <div className="mt-3 space-y-3">
          {questions.map((q, i) => (
            <label key={q} className="block text-sm text-zinc-300">{i + 1}. {q}
              <select
                value={answers[i] ?? ""}
                onChange={(e) => {
                  const v = e.target.value;
                  setAnswers((prev) => {
                    if (v === "") {
                      const next = { ...prev };
                      delete next[i];
                      return next;
                    }
                    return { ...prev, [i]: Number(v) };
                  });
                }}
                className="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
              >
                <option value="">Seleccionar...</option>
                {options.map((opt) => <option key={opt.label} value={opt.value}>{opt.label}</option>)}
              </select>
            </label>
          ))}
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <button type="button" onClick={onClose} className="rounded-lg border border-zinc-600 px-4 py-2 text-sm">Cancelar</button>
          <button type="button" onClick={save} className="rounded-lg bg-sky-500 px-4 py-2 text-sm font-medium text-zinc-900">Guardar y Cerrar</button>
        </div>
      </div>
    </div>
  );
}

