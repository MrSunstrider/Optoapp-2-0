"use client";

import Link from "next/link";
import { useState } from "react";

import type { EvaluacionCardRow } from "@/lib/paciente-clinico";
import { formatFormulaResumida } from "@/lib/paciente-clinico";

import { deleteEvaluacionAction } from "@/app/pacientes/_actions/evaluacion-crud";

export function EvaluacionesCards({
  pacienteId,
  rows,
  canManage
}: {
  pacienteId: string;
  rows: EvaluacionCardRow[];
  canManage: boolean;
}) {
  const [openId, setOpenId] = useState<string | null>(null);

  return (
    <ul className="space-y-4">
      {rows.map((r) => (
        <li
          key={r.id}
          className="rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:shadow-md"
        >
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="min-w-0 flex-1">
              <p className="text-[10px] font-black uppercase tracking-widest text-primary mb-1">
                {r.fecha
                  ? new Date(r.fecha + "T12:00:00").toLocaleDateString("es-PE", {
                      day: "numeric",
                      month: "long",
                      year: "numeric"
                    })
                  : "Sin fecha"}
              </p>
              <p className="font-heading text-lg font-bold text-foreground leading-tight">
                {formatFormulaResumida(r)}
              </p>
              {r.diagnostico?.trim() && (
                <p className="mt-2 text-sm font-medium text-muted-foreground line-clamp-2 italic">"{r.diagnostico.trim()}"</p>
              )}
            </div>
            <button
              type="button"
              className="shrink-0 rounded-xl bg-foreground/[0.03] px-4 py-2 text-[10px] font-black uppercase tracking-widest text-foreground hover:bg-primary hover:text-primary-foreground transition-all"
              onClick={() => setOpenId(openId === r.id ? null : r.id)}
            >
              {openId === r.id ? "Cerrar" : "Detalles"}
            </button>
          </div>

          {openId === r.id && (
            <div className="mt-4 rounded-xl bg-foreground/[0.02] border border-border p-4 text-sm animate-in fade-in slide-in-from-top-2">
              <div className="space-y-3">
                <div className="flex justify-between items-center border-b border-border/50 pb-2">
                   <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">Fórmula Completa</span>
                   <span className="font-bold text-foreground">{formatFormulaResumida(r)}</span>
                </div>
                {r.diagnostico?.trim() && (
                  <div className="space-y-1">
                    <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">Diagnóstico</span>
                    <p className="font-medium text-foreground">{r.diagnostico}</p>
                  </div>
                )}
              </div>
              {canManage && (
                <Link
                  href={`/pacientes/${pacienteId}/evaluaciones/${r.id}/editar`}
                  className="mt-4 inline-flex w-full items-center justify-center rounded-xl bg-primary/10 py-2.5 text-[10px] font-black uppercase tracking-widest text-primary hover:bg-primary/20 transition-all"
                >
                  ✏️ EDITAR EVALUACIÓN
                </Link>
              )}
            </div>
          )}

          {canManage && (
            <form
              action={deleteEvaluacionAction}
              onSubmit={(e) => {
                if (
                  !confirm(
                    "¿Eliminar esta evaluación? Esta acción no se puede deshacer."
                  )
                ) {
                  e.preventDefault();
                }
              }}
              className="mt-4 flex flex-wrap items-center gap-2 border-t border-border pt-4"
            >
              <input type="hidden" name="pacienteId" value={pacienteId} />
              <input type="hidden" name="evaluacionId" value={r.id} />
              <button
                type="submit"
                className="text-[10px] font-black uppercase tracking-widest text-destructive hover:underline"
              >
                Eliminar Registro
              </button>
            </form>
          )}
        </li>
      ))}
    </ul>
  );
}
