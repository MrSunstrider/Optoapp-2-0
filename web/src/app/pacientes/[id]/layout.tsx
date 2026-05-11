import Link from "next/link";
import { notFound, redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { PacienteExpedienteFab } from "@/components/pacientes/paciente-expediente-fab";
import { PacienteFichaToolbar } from "@/components/pacientes/paciente-ficha-toolbar";
import { PacienteTabNav } from "@/components/paciente-tab-nav";
import { createClient } from "@/lib/supabase/server";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchUltimaEvaluacionParaPdf } from "@/lib/paciente-clinico";
import { fetchPacienteById, parseEtiquetasRecientes } from "@/lib/pacientes";
import { canReadPacientes } from "@/lib/roles";

function idPreview(id: string): string {
  return id.length > 8 ? `${id.slice(0, 8)}…` : id;
}

export default async function PacienteExpedienteLayout({
  children,
  params
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  if (!canReadPacientes(activeOptica.rol)) {
    redirect("/pacientes");
  }

  const { id } = await params;
  const supabase = await createClient();
  const row = await fetchPacienteById(supabase, activeOptica.opticaId, id);

  if (!row) {
    notFound();
  }

  const [fiscal, ultima] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    fetchUltimaEvaluacionParaPdf(supabase, activeOptica.opticaId, id)
  ]);
  const proximaCitaIso = ultima?.proxima_cita ?? null;
  const contextLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);
  const tags = parseEtiquetasRecientes(row.ultimas_etiquetas, 2);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-mx-2 overflow-hidden rounded-2xl bg-[#121212] text-zinc-100 sm:mx-0">
        <div className="px-3 py-2 text-center text-[13px] leading-snug text-sky-400 sm:text-left">
          {contextLine}
        </div>

        <div className="flex items-center gap-2 bg-sky-400 px-3 py-2.5 text-zinc-900">
          <Link
            href="/pacientes"
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg hover:bg-black/10"
            aria-label="Volver al listado de pacientes"
          >
            <span className="text-xl font-semibold leading-none">←</span>
          </Link>
          <h1 className="min-w-0 flex-1 text-lg font-bold tracking-tight sm:text-xl">
            Ficha del Paciente
          </h1>
          <PacienteFichaToolbar
            pacienteId={id}
            telefono={row.telefono}
            nombrePaciente={row.nombre_completo}
            proximaCitaIso={proximaCitaIso}
            hasEvaluaciones={!!ultima}
            pdfHref={`/api/pacientes/${id}/receta-pdf`}
            editarHref={`/pacientes/${id}/editar`}
            rol={activeOptica.rol}
          />
        </div>

        <div className="border-b border-zinc-800 px-4 py-4">
          <div className="flex gap-4 rounded-xl border border-zinc-800 bg-zinc-900/80 p-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-zinc-800 text-zinc-400">
              <svg
                viewBox="0 0 24 24"
                width={28}
                height={28}
                fill="none"
                stroke="currentColor"
                strokeWidth={1.5}
                aria-hidden
              >
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-lg font-semibold text-white">
                {row.nombre_completo}
              </p>
              <p className="mt-0.5 text-sm text-zinc-300">
                Tel: {row.telefono}
              </p>
              <p className="mt-0.5 font-mono text-xs text-zinc-500">
                ID: {idPreview(row.id)}
              </p>
              {tags.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-2">
                  {tags.map((t) => (
                    <span
                      key={t}
                      className="rounded-full border border-zinc-700 bg-zinc-800 px-2 py-0.5 text-[11px] text-zinc-400"
                    >
                      {t}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>

          <PacienteTabNav pacienteId={id} variant="fichaDark" />
        </div>

        <div className="min-h-[200px] px-4 pb-10 pt-2">{children}</div>
      </div>

      <PacienteExpedienteFab pacienteId={id} />
    </AppShell>
  );
}
