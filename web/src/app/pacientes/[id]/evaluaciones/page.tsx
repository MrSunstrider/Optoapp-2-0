import Link from "next/link";
import { redirect } from "next/navigation";

import { EvaluacionesCards } from "@/components/pacientes/evaluaciones-cards";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchEvaluacionesDetalladasPorPaciente } from "@/lib/paciente-clinico";
import { canManagePacientes } from "@/lib/roles";

export default async function PacienteEvaluacionesPage({
  params,
  searchParams
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{
    msg?: string;
    error?: string;
  }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  const { id } = await params;
  const query = await searchParams;
  const supabase = await createClient();
  const rows = await fetchEvaluacionesDetalladasPorPaciente(
    supabase,
    activeOptica.opticaId,
    id
  );
  const canManage = canManagePacientes(activeOptica.rol);

  return (
    <div>
      {query.msg === "creado" && (
        <p className="mb-4 text-sm text-emerald-400">Paciente creado correctamente.</p>
      )}
      {query.msg === "guardado" && (
        <p className="mb-4 text-sm text-emerald-400">Cambios guardados.</p>
      )}
      {query.msg === "eval_eliminada" && (
        <p className="mb-4 text-sm text-emerald-400">Evaluación eliminada.</p>
      )}
      {query.msg === "eval_creada" && (
        <p className="mb-4 text-sm text-emerald-400">
          Evaluación registrada correctamente.
        </p>
      )}
      {query.msg === "eval_actualizada" && (
        <p className="mb-4 text-sm text-emerald-400">Evaluación actualizada correctamente.</p>
      )}
      {query.error === "confirm" && (
        <p className="mb-4 text-sm text-red-400">Confirmación inválida para eliminar.</p>
      )}
      {query.error === "eliminar" && (
        <p className="mb-4 text-sm text-red-400">
          No se pudo eliminar el paciente. Revisa permisos o límite diario.
        </p>
      )}
      {query.error === "limite_borrado" && (
        <p className="mb-4 text-sm text-red-400">
          Límite diario de eliminaciones alcanzado (10 por día por usuario).
        </p>
      )}
      {query.error === "eliminar_eval" && (
        <p className="mb-4 text-sm text-red-400">
          No se pudo eliminar la evaluación (permisos o datos relacionados).
        </p>
      )}

      {canManage && (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2 md:hidden">
          <Link
            href={`/pacientes/${id}/evaluaciones/nueva`}
            className="rounded-lg bg-sky-500 px-3 py-1.5 text-sm font-medium text-zinc-900"
          >
            Nueva evaluación
          </Link>
        </div>
      )}

      {rows.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <svg
            className="mb-4 h-16 w-16 text-zinc-600"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.25}
            aria-hidden
          >
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" />
          </svg>
          <p className="text-base text-zinc-500">
            No hay evaluaciones registradas.
          </p>
          <p className="mt-2 max-w-sm text-sm text-zinc-600">
            Usa el botón + para añadir la primera evaluación clínica.
          </p>
        </div>
      ) : (
        <EvaluacionesCards pacienteId={id} rows={rows} canManage={canManage} />
      )}
    </div>
  );
}
