import Link from "next/link";
import type { ReactNode } from "react";
import { redirect } from "next/navigation";

import { PacienteForm } from "@/components/pacientes/paciente-form";
import { AppShell } from "@/components/app-shell";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getOpticaPacienteLimitInfo } from "@/lib/optica-limits";
import { canManagePacientes } from "@/lib/roles";

import { createPacienteAction } from "@/app/pacientes/_actions/paciente-crud";

export default async function NuevoPacientePage({
  searchParams
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const params = await searchParams;
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  if (!canManagePacientes(activeOptica.rol)) {
    return (
      <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
        <h1 className="mb-2 text-2xl font-semibold">Nuevo Paciente</h1>
        <p className="text-sm text-muted-foreground">
          Tu rol actual no tiene permiso para crear pacientes.
        </p>
      </AppShell>
    );
  }

  const supabase = await createClient();
  const limit = await getOpticaPacienteLimitInfo(supabase, activeOptica.opticaId);
  const fiscal = await fetchOpticaFiscal(supabase, activeOptica.opticaId);

  /** Misma regla que el FAB: sin cupo no se muestra el formulario (evita crear por URL). */
  if (!limit.puedeCrearMas && params.error !== "limite") {
    redirect("/pacientes");
  }

  let banner: ReactNode = null;
  if (params.error === "validacion") {
    banner = (
      <p className="mb-4 text-sm text-red-400">
        Revisa los campos obligatorios (nombre, edad, teléfono).
      </p>
    );
  } else if (params.error === "guardar") {
    banner = (
      <p className="mb-4 text-sm text-red-400">
        No se pudo guardar. Intenta nuevamente.
      </p>
    );
  } else if (params.error === "duplicado_ho") {
    banner = (
      <p className="mb-4 text-sm text-red-400">
        Ya existe esa historia optométrica en esta óptica. Cambia el número HO.
      </p>
    );
  } else if (params.error === "limite") {
    banner = (
      <div className="mb-4 rounded-lg border border-amber-500/40 bg-amber-500/10 p-4 text-sm text-zinc-100">
        <p className="font-medium text-amber-200">Límite de pacientes del plan</p>
        <p className="mt-2 text-zinc-300">
          Tu óptica alcanzó el máximo configurado
          {limit.maxPacientes != null
            ? ` (${limit.pacientesActuales}/${limit.maxPacientes})`
            : ""}
          . Actualiza a PRO o pide aumentar{" "}
          <code className="rounded bg-black/30 px-1 text-xs">max_pacientes_por_optica</code>.
        </p>
        <Link
          href="/configuracion"
          className="mt-3 inline-flex rounded-md bg-amber-300 px-3 py-1.5 text-xs font-semibold text-amber-950 hover:bg-amber-200"
        >
          Ir a Configuración / Upgrade
        </Link>
      </div>
    );
  }

  const contextLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-mx-2 rounded-2xl bg-black px-4 py-5 text-zinc-100 sm:mx-0 sm:px-6">
        {/* Franja contexto + barra navegación (paridad app Material oscuro) */}
        <div className="mb-4 rounded-lg bg-violet-950/70 px-3 py-2.5 text-center text-[13px] leading-snug text-sky-400 sm:text-left">
          {contextLine}
        </div>

        <nav className="mb-6 flex items-center gap-2">
          <Link
            href="/pacientes"
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-zinc-300 hover:bg-zinc-900 hover:text-white"
            aria-label="Volver a pacientes"
          >
            <span className="text-xl leading-none">←</span>
          </Link>
          <h1 className="text-xl font-semibold tracking-tight text-white sm:text-2xl">
            Nuevo Paciente
          </h1>
        </nav>

        <PacienteForm
          action={createPacienteAction}
          submitLabel="Guardar paciente"
          cancelHref="/pacientes"
          errorBanner={banner}
          variant="materialDark"
          mode="create"
        />
      </div>
    </AppShell>
  );
}
