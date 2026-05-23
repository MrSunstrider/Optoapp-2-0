"use client";

import type { EvaluacionDraft, DraftUpdateFn } from "@/lib/evaluacion-types";
import {
  DIAG_OPTIONS,
  CITA_ESTADOS
} from "@/lib/evaluacion-constants";

type Props = {
  draft: EvaluacionDraft;
  update: DraftUpdateFn;
  inputCls: string;
  mode: "create" | "edit";
  isPending: boolean;
};

export function DiagnosticoTab({ draft, update, inputCls, mode, isPending }: Props) {
  const cardCls = "space-y-4 rounded-2xl border border-border bg-foreground/[0.02] p-5 shadow-sm";
  const hasProxCita = draft.proximaCita.trim() !== "";

  function toggleManual(
    flagKey: "autoPresbicia" | "autoAnisometropia" | "autoAmbliopia"
  ) {
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
            <select
              value={draft.diagnosticoOd}
              onChange={(e) => update("diagnosticoOd", e.target.value)}
              className={inputCls}
            >
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
            <select
              value={draft.diagnosticoOi}
              onChange={(e) => update("diagnosticoOi", e.target.value)}
              className={inputCls}
            >
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
              onChange={(e) =>
                toggleManualCheck("autoPresbicia", "otrosPresbicia", e.target.checked)
              }
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
              onChange={(e) =>
                toggleManualCheck("autoAmbliopia", "otrosAmbliopia", e.target.checked)
              }
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
        disabled={isPending}
        className="w-full rounded-lg bg-sky-400 px-4 py-2.5 text-sm font-semibold text-zinc-900 disabled:opacity-50"
      >
        {isPending
          ? "Guardando…"
          : mode === "edit"
            ? "Actualizar Evaluacion"
            : "Guardar Evaluacion"}
      </button>
    </div>
  );
}
