import { redirect } from "next/navigation";

import { createEvaluacionAction } from "@/app/pacientes/[id]/evaluaciones/nueva/actions";
import { NuevaEvaluacionForm } from "@/components/pacientes/nueva-evaluacion-form";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { localTodayDateOnly } from "@/lib/pacientes";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

export default async function NuevaEvaluacionPage({
  params,
  searchParams
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ error?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/pacientes");

  const { id } = await params;
  const query = await searchParams;
  const supabase = await createClient();
  const fiscal = await fetchOpticaFiscal(supabase, activeOptica.opticaId);
  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);

  return (
    <div className="max-w-3xl">
      {query.error === "guardar_eval" && (
        <p className="mb-3 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          No se pudo guardar la evaluación. Verifica permisos o intenta nuevamente.
        </p>
      )}

      <NuevaEvaluacionForm
        pacienteId={id}
        backHref={`/pacientes/${id}/evaluaciones`}
        opticaLine={opticaLine}
        todayDate={localTodayDateOnly()}
        saveEvaluacionAction={createEvaluacionAction}
      />
    </div>
  );
}
