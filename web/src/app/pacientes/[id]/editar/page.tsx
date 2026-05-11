import Link from "next/link";
import type { ReactNode } from "react";
import { notFound, redirect } from "next/navigation";

import { PacienteForm } from "@/components/pacientes/paciente-form";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchPacienteById } from "@/lib/pacientes";
import { canManagePacientes } from "@/lib/roles";

import { updatePacienteAction } from "@/app/pacientes/_actions/paciente-crud";

export default async function EditarPacientePage({
  params,
  searchParams
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ error?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  if (!canManagePacientes(activeOptica.rol)) {
    return (
      <div>
        <h1 className="text-2xl font-semibold mb-2">Editar paciente</h1>
        <p className="text-sm text-muted-foreground">
          Tu rol no permite editar pacientes.
        </p>
      </div>
    );
  }

  const { id } = await params;
  const query = await searchParams;
  const supabase = await createClient();
  const row = await fetchPacienteById(supabase, activeOptica.opticaId, id);

  if (!row) notFound();

  let banner: ReactNode = null;
  if (query.error === "validacion") {
    banner = (
      <p className="mb-4 text-sm text-destructive">
        Revisa los campos obligatorios antes de guardar.
      </p>
    );
  } else if (query.error === "guardar") {
    banner = (
      <p className="mb-4 text-sm text-destructive">
        No se pudo guardar los cambios.
      </p>
    );
  } else if (query.error === "duplicado_ho") {
    banner = (
      <p className="mb-4 text-sm text-destructive">
        Ya existe otra ficha con esa HO en esta óptica.
      </p>
    );
  }

  return (
    <div>
      <div className="mb-6">
        <Link
          href={`/pacientes/${id}`}
          className="text-sm font-medium text-muted-foreground hover:text-foreground"
        >
          ← Ficha del paciente
        </Link>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">
          Editar paciente
        </h1>
      </div>

      <PacienteForm
        action={updatePacienteAction}
        pacienteId={row.id}
        defaultValues={row}
        submitLabel="Guardar cambios"
        cancelHref={`/pacientes/${id}`}
        errorBanner={banner}
      />
    </div>
  );
}
