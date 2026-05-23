"use client";

import Link from "next/link";
import { useState } from "react";

/** FAB violeta como en la app Android (lista Pacientes). */
export function PacientesListFab({
  puedeCrear,
  maxPacientes,
  pacientesActuales
}: {
  puedeCrear: boolean;
  maxPacientes: number | null;
  pacientesActuales: number;
}) {
  const [open, setOpen] = useState(false);

  const fabBase =
    "fixed bottom-24 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full text-2xl font-light text-white shadow-lg md:bottom-20";

  if (puedeCrear) {
    return (
      <Link
        href="/pacientes/nuevo"
        className={
          fabBase +
          " bg-violet-600 shadow-violet-900/40 transition-colors hover:bg-violet-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-400"
        }
        title="Nuevo paciente"
        aria-label="Nuevo paciente"
      >
        +
      </Link>
    );
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className={
          fabBase +
          " bg-zinc-600 text-zinc-200 hover:bg-zinc-500"
        }
        title="Límite de pacientes"
        aria-label="Límite de pacientes"
      >
        +
      </button>
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="max-w-md rounded-xl border border-border bg-card p-5 shadow-xl">
            <h3 className="text-lg font-semibold">Límite de pacientes</h3>
            <p className="mt-2 text-sm text-muted-foreground">
              Tu plan actual alcanzó el máximo de pacientes
              {maxPacientes != null ? ` (${pacientesActuales}/${maxPacientes})` : ""}.
              Actualiza a un plan superior (PRO) o contacta al administrador de tu óptica para
              aumentar el cupo en Supabase{" "}
              <code className="text-xs">opticas.max_pacientes_por_optica</code>.
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              <Link
                href="/configuracion"
                className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
                onClick={() => setOpen(false)}
              >
                Actualizar plan
              </Link>
              <button
                type="button"
                className="rounded-md border border-border px-4 py-2 text-sm font-medium"
                onClick={() => setOpen(false)}
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
