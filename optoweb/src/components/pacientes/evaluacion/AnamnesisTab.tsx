"use client";

import type { EvaluacionDraft, DraftUpdateFn } from "@/lib/evaluacion-types";

type Props = {
  draft: EvaluacionDraft;
  update: DraftUpdateFn;
  inputCls: string;
};

export function AnamnesisTab({ draft, update, inputCls }: Props) {
  return (
    <>
      <label className="block text-xs font-bold uppercase tracking-widest text-muted-foreground/60">
        Fecha de registro
        <input
          type="date"
          value={draft.fecha}
          onChange={(e) => update("fecha", e.target.value)}
          className={inputCls}
        />
      </label>
      <label className="block text-xs font-bold uppercase tracking-widest text-muted-foreground/60">
        Motivo de consulta
        <textarea
          value={draft.motivoConsulta}
          onChange={(e) => update("motivoConsulta", e.target.value)}
          rows={3}
          className={inputCls + " placeholder:text-muted-foreground/30"}
        />
      </label>
      <label className="block text-xs font-bold uppercase tracking-widest text-muted-foreground/60">
        Síntomas y signos
        <textarea
          value={draft.sintomasSignos}
          onChange={(e) => update("sintomasSignos", e.target.value)}
          rows={4}
          className={inputCls + " placeholder:text-muted-foreground/30"}
        />
      </label>
      <div className="space-y-4 rounded-2xl border border-border bg-foreground/[0.02] p-5 shadow-sm">
        <p className="font-heading text-sm font-bold text-primary">Antecedentes</p>
        <label className="block text-xs font-bold uppercase tracking-tight text-muted-foreground/60">
          Personales oculares
          <textarea
            value={draft.antecedentesPersonalesOculares}
            onChange={(e) => update("antecedentesPersonalesOculares", e.target.value)}
            rows={2}
            className={inputCls}
          />
        </label>
        <label className="block text-xs font-bold uppercase tracking-tight text-muted-foreground/60">
          Personales sistémicos
          <textarea
            value={draft.antecedentesPersonalesSistemicos}
            onChange={(e) => update("antecedentesPersonalesSistemicos", e.target.value)}
            rows={2}
            className={inputCls}
          />
        </label>
        <label className="block text-xs font-bold uppercase tracking-tight text-muted-foreground/60">
          Familiares oculares
          <textarea
            value={draft.antecedentesFamiliaresOculares}
            onChange={(e) => update("antecedentesFamiliaresOculares", e.target.value)}
            rows={2}
            className={inputCls}
          />
        </label>
        <label className="block text-xs font-bold uppercase tracking-tight text-muted-foreground/60">
          Familiares sistémicos
          <textarea
            value={draft.antecedentesFamiliaresSistemicos}
            onChange={(e) => update("antecedentesFamiliaresSistemicos", e.target.value)}
            rows={2}
            className={inputCls}
          />
        </label>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <label className="text-xs font-bold uppercase tracking-tight text-muted-foreground/60">
            Medicación
            <input
              value={draft.medicacion}
              onChange={(e) => update("medicacion", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-xs font-bold uppercase tracking-tight text-muted-foreground/60">
            Alergias
            <input
              value={draft.alergias}
              onChange={(e) => update("alergias", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <label className="block text-xs font-bold uppercase tracking-tight text-muted-foreground/60">
          Necesidad visual
          <textarea
            value={draft.necesidadVisual}
            onChange={(e) => update("necesidadVisual", e.target.value)}
            rows={2}
            className={inputCls}
            placeholder="Ej: Computadora, Conducir, Lectura prolongada"
          />
        </label>
      </div>
    </>
  );
}
