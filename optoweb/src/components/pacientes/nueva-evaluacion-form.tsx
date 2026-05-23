"use client";

import Link from "next/link";
import { useMemo, useState, useActionState } from "react";

import type { SaveEvaluacionAction, EvaluacionDraft } from "@/lib/evaluacion-types";
import { TAB_ITEMS, type TabItem, toEmptyDraft, autoDiagEye, hasBalanceText, autoPresbiciaValue, autoAnisometropiaValue, autoAmbliopiaValue } from "@/lib/evaluacion-constants";
import { AnamnesisTab } from "@/components/pacientes/evaluacion/AnamnesisTab";
import { ExamenVisualTab } from "@/components/pacientes/evaluacion/ExamenVisualTab";
import { RefraccionTab } from "@/components/pacientes/evaluacion/RefraccionTab";
import { ContactologiaTab } from "@/components/pacientes/evaluacion/ContactologiaTab";
import { DiagnosticoTab } from "@/components/pacientes/evaluacion/DiagnosticoTab";
import { OsdiDialog } from "@/components/pacientes/evaluacion/OsdiDialog";

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
  const [actionState, action, isPending] = useActionState(saveEvaluacionAction, null);

  const storageKey = useMemo(
    () => `optoapp:web:evaluacion-form:${mode}:${pacienteId}:${evaluacionId ?? "new"}`,
    [mode, pacienteId, evaluacionId]
  );

  const initSessionTab = (): TabItem => {
    if (typeof window === "undefined") return "Anamnesis";
    const raw = window.sessionStorage.getItem(`${storageKey}:tab`);
    return raw && TAB_ITEMS.includes(raw as TabItem) ? (raw as TabItem) : "Anamnesis";
  };

  const initSessionDraft = (): EvaluacionDraft => {
    const base = { ...toEmptyDraft(todayDate), ...(initialData ?? {}) };
    if (typeof window === "undefined") return base;
    const raw = window.sessionStorage.getItem(storageKey);
    if (!raw) return base;
    try {
      return { ...base, ...(JSON.parse(raw) as Partial<EvaluacionDraft>) };
    } catch {
      return base;
    }
  };

  const [activeTab, setActiveTab] = useState<TabItem>(initSessionTab);
  const [warnEmptyMotivo, setWarnEmptyMotivo] = useState(false);
  const [draft, setDraft] = useState<EvaluacionDraft>(initSessionDraft);
  const [osdiOpen, setOsdiOpen] = useState(false);

  // Persist draft to sessionStorage on every change
  useMemo(() => {
    if (typeof window !== "undefined") {
      window.sessionStorage.setItem(storageKey, JSON.stringify(draft));
    }
  }, [draft, storageKey]);

  // Persist activeTab to sessionStorage on every change
  useMemo(() => {
    if (typeof window !== "undefined") {
      window.sessionStorage.setItem(`${storageKey}:tab`, activeTab);
    }
  }, [activeTab, storageKey]);

  // Auto-compute diagnostic fields from prescription values
  // This runs during render so derived values are always in sync
  const autoDiagnostics = useMemo(() => {
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
    const resumen = [diagOd, diagOi]
      .filter(Boolean)
      .map((d, i) => `${i === 0 ? "OD" : "OI"}: ${d}`)
      .join(" | ");
    return {
      diagnosticoOd: diagOd,
      diagnosticoOi: diagOi,
      addCercaOi: isAddAo ? draft.addCercaOd : draft.addCercaOi,
      otrosPresbicia: draft.autoPresbicia === "true" ? String(presbAuto) : draft.otrosPresbicia,
      otrosAnisometropia: draft.autoAnisometropia === "true" ? String(anisoAuto) : draft.otrosAnisometropia,
      otrosAmbliopia: draft.autoAmbliopia === "true" && ambliAuto !== null ? String(ambliAuto) : draft.otrosAmbliopia,
      diagnosticoResumen: resumen,
    };
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
    draft.autoAmbliopia,
    draft.otrosPresbicia,
    draft.otrosAnisometropia,
    draft.otrosAmbliopia,
  ]);

  // Merge auto-computed values into draft for rendering
  const computedDraft = useMemo(() => ({
    ...draft,
    ...autoDiagnostics,
  }), [draft, autoDiagnostics]);

  function update<K extends keyof EvaluacionDraft>(key: K, value: EvaluacionDraft[K]) {
    setDraft((prev) => ({ ...prev, [key]: value }));
  }

  function clearDraft() {
    window.sessionStorage.removeItem(storageKey);
    window.sessionStorage.removeItem(`${storageKey}:tab`);
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

  const tabBtnBase =
    "rounded-full border px-4 py-2 text-xs font-bold transition-all active:scale-95";
  const inputCls =
    "mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium text-foreground placeholder:text-muted-foreground/40 focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all";

  const hiddenPairs: Array<[string, string]> = [
    ["pacienteId", pacienteId],
    ["evaluacionId", evaluacionId ?? ""],
    ["fecha", computedDraft.fecha],
    ["motivoConsulta", computedDraft.motivoConsulta],
    ["sintomasSignos", computedDraft.sintomasSignos],
    ["antecedentesPersonalesOculares", computedDraft.antecedentesPersonalesOculares],
    ["antecedentesPersonalesSistemicos", computedDraft.antecedentesPersonalesSistemicos],
    ["antecedentesFamiliaresOculares", computedDraft.antecedentesFamiliaresOculares],
    ["antecedentesFamiliaresSistemicos", computedDraft.antecedentesFamiliaresSistemicos],
    ["medicacion", computedDraft.medicacion],
    ["alergias", computedDraft.alergias],
    ["necesidadVisual", computedDraft.necesidadVisual],
    ["avScAo", computedDraft.avScAo],
    ["avScOdLejos", computedDraft.avScOdLejos],
    ["avScOiLejos", computedDraft.avScOiLejos],
    ["avCcAoPx", computedDraft.avCcAoPx],
    ["avCcOdLejos", computedDraft.avCcOdLejos],
    ["avCcOiLejos", computedDraft.avCcOiLejos],
    ["estereopsisValor", computedDraft.estereopsisValor],
    ["estereopsisSegundos", computedDraft.estereopsisSegundos],
    ["lang", computedDraft.lang],
    ["worth", computedDraft.worth],
    ["ishihara", computedDraft.ishihara],
    ["farnsworth", computedDraft.farnsworth],
    ["schirmerOd", computedDraft.schirmerOd],
    ["schirmerOi", computedDraft.schirmerOi],
    ["osdiPuntuacion", computedDraft.osdiPuntuacion],
    ["osdiClasificacion", computedDraft.osdiClasificacion],
    ["sensibilidadContraste", computedDraft.sensibilidadContraste],
    ["sensibilidadFrecuencia", computedDraft.sensibilidadFrecuencia],
    ["amsler", computedDraft.amsler],
    ["campoVisual", computedDraft.campoVisual],
    ["campoVisualDescripcion", computedDraft.campoVisualDescripcion],
    ["phOd", computedDraft.phOd],
    ["phOi", computedDraft.phOi],
    ["kappaOd", computedDraft.kappaOd],
    ["kappaOi", computedDraft.kappaOi],
    ["hirshberg", computedDraft.hirshberg],
    ["duccionesOd", computedDraft.duccionesOd],
    ["duccionesOi", computedDraft.duccionesOi],
    ["versionesAo", computedDraft.versionesAo],
    ["coverTest6m", computedDraft.coverTest6m],
    ["coverTest40cm", computedDraft.coverTest40cm],
    ["coverTest10cm", computedDraft.coverTest10cm],
    ["ppcOr", computedDraft.ppcOr],
    ["ppcLuz", computedDraft.ppcLuz],
    ["ppcFrl", computedDraft.ppcFrl],
    ["reflejoFotomotor", computedDraft.reflejoFotomotor],
    ["reflejoConsensual", computedDraft.reflejoConsensual],
    ["reflejoAcomodativo", computedDraft.reflejoAcomodativo],
    ["objOdEsf", computedDraft.objOdEsf],
    ["objOdCil", computedDraft.objOdCil],
    ["objOdEje", computedDraft.objOdEje],
    ["objOiEsf", computedDraft.objOiEsf],
    ["objOiCil", computedDraft.objOiCil],
    ["objOiEje", computedDraft.objOiEje],
    ["subjOdEsf", computedDraft.subjOdEsf],
    ["subjOdCil", computedDraft.subjOdCil],
    ["subjOdEje", computedDraft.subjOdEje],
    ["subjOiEsf", computedDraft.subjOiEsf],
    ["subjOiCil", computedDraft.subjOiCil],
    ["subjOiEje", computedDraft.subjOiEje],
    ["recetaOdEsf", computedDraft.recetaOdEsf],
    ["recetaOdCil", computedDraft.recetaOdCil],
    ["recetaOdEje", computedDraft.recetaOdEje],
    ["recetaOdAv", computedDraft.recetaOdAv],
    ["recetaOiEsf", computedDraft.recetaOiEsf],
    ["recetaOiCil", computedDraft.recetaOiCil],
    ["recetaOiEje", computedDraft.recetaOiEje],
    ["recetaOiAv", computedDraft.recetaOiAv],
    ["addCercaOd", computedDraft.addCercaOd],
    ["addCercaOi", computedDraft.addCercaOi],
    ["addAv", computedDraft.addAv],
    ["isAddAo", computedDraft.isAddAo],
    ["dipLejos", computedDraft.dipLejos],
    ["dipCerca", computedDraft.dipCerca],
    ["prismaOdValor", computedDraft.prismaOdValor],
    ["prismaOdBase", computedDraft.prismaOdBase],
    ["prismaOiValor", computedDraft.prismaOiValor],
    ["prismaOiBase", computedDraft.prismaOiBase],
    ["k1Od", computedDraft.k1Od],
    ["k2Od", computedDraft.k2Od],
    ["k1Oi", computedDraft.k1Oi],
    ["k2Oi", computedDraft.k2Oi],
    ["lcOdEsf", computedDraft.lcOdEsf],
    ["lcOdCil", computedDraft.lcOdCil],
    ["lcOdEje", computedDraft.lcOdEje],
    ["lcOiEsf", computedDraft.lcOiEsf],
    ["lcOiCil", computedDraft.lcOiCil],
    ["lcOiEje", computedDraft.lcOiEje],
    ["lcRadioBaseOd", computedDraft.lcRadioBaseOd],
    ["lcRadioBaseOi", computedDraft.lcRadioBaseOi],
    ["lcOdDia", computedDraft.lcOdDia],
    ["lcOiDia", computedDraft.lcOiDia],
    ["lcLaboratorio", computedDraft.lcLaboratorio],
    ["lcTipoLente", computedDraft.lcTipoLente],
    ["lcMaterial", computedDraft.lcMaterial],
    ["lcFechaAdaptacion", computedDraft.lcFechaAdaptacion],
    ["lcObservaciones", computedDraft.lcObservaciones],
    ["diagnosticoOd", computedDraft.diagnosticoOd],
    ["diagnosticoOi", computedDraft.diagnosticoOi],
    ["balanceOd", computedDraft.balanceOd],
    ["balanceOi", computedDraft.balanceOi],
    ["otrosPresbicia", computedDraft.otrosPresbicia],
    ["otrosAnisometropia", computedDraft.otrosAnisometropia],
    ["otrosAmbliopia", computedDraft.otrosAmbliopia],
    ["autoPresbicia", computedDraft.autoPresbicia],
    ["autoAnisometropia", computedDraft.autoAnisometropia],
    ["autoAmbliopia", computedDraft.autoAmbliopia],
    ["planTratamiento", computedDraft.planTratamiento],
    ["citaEstado", computedDraft.citaEstado],
    ["diagnosticoResumen", computedDraft.diagnosticoResumen],
    ["indicaciones", computedDraft.indicaciones],
    ["proximaCita", computedDraft.proximaCita],
    ["notasFinales", computedDraft.notasFinales]
  ];

  return (
    <form action={action} onSubmit={handleSubmit} className="space-y-4">
      {actionState?.error && (
        <p className="mb-3 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {actionState.error}
        </p>
      )}
      {hiddenPairs.map(([k, v]) => (
        <input key={k} type="hidden" name={k} value={v} />
      ))}

      <div className="rounded-2xl border border-border bg-card text-foreground shadow-xl">
        <div className="border-b border-border px-4 py-2 text-[11px] font-black uppercase tracking-widest text-primary/70">
          {opticaLine}
        </div>
        <div className="flex items-center gap-3 bg-primary px-4 py-4 text-primary-foreground shadow-md">
          <Link
            href={backHref}
            className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 hover:bg-white/20 transition-all active:scale-95"
            aria-label="Volver a evaluaciones"
          >
            <span className="text-xl font-bold">{"←"}</span>
          </Link>
          <h2 className="flex-1 font-heading text-xl font-bold">
            {mode === "edit" ? "Editar Evaluación" : "Nueva Evaluación"}
          </h2>
          <button
            type="submit"
            disabled={isPending}
            className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 hover:bg-white/20 transition-all active:scale-95 disabled:opacity-50"
            title="Guardar evaluación"
            aria-label="Guardar evaluación"
          >
            {isPending ? "…" : "💾"}
          </button>
        </div>

        <div className="flex flex-wrap gap-2 border-b border-border px-4 py-4 bg-foreground/[0.01]">
          {TAB_ITEMS.map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setActiveTab(item)}
              className={
                tabBtnBase +
                (activeTab === item
                  ? " border-primary bg-primary text-primary-foreground shadow-lg shadow-primary/20"
                  : " border-border bg-card text-muted-foreground hover:bg-foreground/5")
              }
            >
              {item}
            </button>
          ))}
        </div>

        <div className="space-y-4 px-4 py-4">
          {warnEmptyMotivo && (
            <p className="rounded-lg border border-amber-900/60 bg-amber-950/40 px-3 py-2 text-sm text-amber-300">
              Motivo de consulta esta vacio. Presiona guardar otra vez si quieres continuar igual.
            </p>
          )}

          {activeTab === "Anamnesis" && (
            <AnamnesisTab draft={computedDraft} update={update} inputCls={inputCls} />
          )}
          {activeTab === "Examen Visual" && (
            <ExamenVisualTab
              draft={computedDraft}
              update={update}
              onOpenOsdi={() => setOsdiOpen(true)}
              inputCls={inputCls}
            />
          )}
          {activeTab === "Refraccion" && (
            <RefraccionTab draft={computedDraft} update={update} inputCls={inputCls} />
          )}
          {activeTab === "Contactologia" && (
            <ContactologiaTab draft={computedDraft} update={update} inputCls={inputCls} />
          )}
          {activeTab === "Cierre" && (
            <DiagnosticoTab
              draft={computedDraft}
              update={update}
              inputCls={inputCls}
              mode={mode}
              isPending={isPending}
            />
          )}
        </div>
      </div>

      {osdiOpen && (
        <OsdiDialog
          onClose={() => setOsdiOpen(false)}
          onSave={(s, c) => {
            update("osdiPuntuacion", String(s));
            update("osdiClasificacion", c);
            setOsdiOpen(false);
          }}
        />
      )}
    </form>
  );
}
