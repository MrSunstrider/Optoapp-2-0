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
    <ul className="space-y-3">
      {rows.map((r) => (
        <li
          key={r.id}
          className="rounded-xl border border-zinc-700 bg-zinc-900/60 p-4 shadow-sm"
        >
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <p className="font-medium text-white">
                {r.fecha
                  ? new Date(r.fecha + "T12:00:00").toLocaleDateString("es-PE", {
                      day: "numeric",
                      month: "short",
                      year: "numeric"
                    })
                  : "Sin fecha"}
              </p>
              <p className="mt-1 text-sm text-zinc-400">
                {formatFormulaResumida(r)}
              </p>
              {r.diagnostico?.trim() && (
                <p className="mt-2 line-clamp-2 text-sm">{r.diagnostico.trim()}</p>
              )}
            </div>
            <button
              type="button"
              className="rounded-md border border-zinc-600 px-2 py-1 text-xs font-medium text-zinc-300"
              onClick={() => setOpenId(openId === r.id ? null : r.id)}
            >
              Ver resumen
            </button>
          </div>

          {openId === r.id && (
            <div className="mt-3 rounded-lg bg-zinc-800/80 p-3 text-sm text-zinc-200">
              <p>
                <span className="font-medium">Fórmula:</span>{" "}
                {formatFormulaResumida(r)}
              </p>
              {r.diagnostico?.trim() && (
                <p className="mt-2">
                  <span className="font-medium">Diagnóstico:</span> {r.diagnostico}
                </p>
              )}
              {canManage && (
                <Link
                  href={`/pacientes/${pacienteId}/evaluaciones/${r.id}/editar`}
                  className="mt-2 inline-block text-xs font-medium text-sky-400 hover:underline"
                >
                  Editar evaluación
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
              className="mt-3 flex flex-wrap items-center gap-2 border-t border-zinc-700 pt-3"
            >
              <input type="hidden" name="pacienteId" value={pacienteId} />
              <input type="hidden" name="evaluacionId" value={r.id} />
              <button
                type="submit"
                className="text-xs font-medium text-destructive hover:underline"
              >
                Eliminar
              </button>
            </form>
          )}
        </li>
      ))}
    </ul>
  );
}
