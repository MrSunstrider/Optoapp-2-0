import { redirect } from "next/navigation";

import { AgendaList } from "@/components/agenda/agenda-list";
import { AgendaPeriodTabs } from "@/components/agenda/agenda-period-tabs";
import { AppShell } from "@/components/app-shell";
import {
  fetchAgendaItems,
  getAgendaPeriodBounds,
  normalizeAgendaPeriodo
} from "@/lib/agenda";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule, canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

export default async function AgendaPage({
  searchParams
}: {
  searchParams: Promise<{ periodo?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canAccessModule(activeOptica.rol, "agenda")) redirect("/dashboard");

  const params = await searchParams;
  const periodo = normalizeAgendaPeriodo(params.periodo);
  const bounds = getAgendaPeriodBounds(periodo);
  const supabase = await createClient();

  let items = [] as Awaited<ReturnType<typeof fetchAgendaItems>>;
  let queryError = false;
  try {
    items = await fetchAgendaItems(supabase, activeOptica.opticaId, periodo);
    console.info("[agenda] periodo=%s citas=%d", periodo, items.length);
  } catch (e) {
    queryError = true;
    const msg = e instanceof Error ? e.message : String(e);
    console.error("[agenda] query failed periodo=%s detalle=%s", periodo, msg);
  }

  const fiscal = await fetchOpticaFiscal(supabase, activeOptica.opticaId);
  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);
  const canWrite = canManagePacientes(activeOptica.rol);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-4xl space-y-4">
          <p className="text-xs text-zinc-400">{opticaLine}</p>

          <header className="space-y-1">
            <h1 className="text-4xl font-bold text-white">OptoApp</h1>
            <p className="text-3xl text-zinc-200">Agenda (próximas citas)</p>
          </header>

          <AgendaPeriodTabs active={periodo} />
          <p className="text-xs text-zinc-500">{bounds.note}</p>

          {queryError ? (
            <p role="alert" className="rounded-lg border border-red-700/60 bg-red-950/30 p-3 text-sm text-red-200">
              No se pudo cargar la agenda por ahora. Intenta nuevamente.
            </p>
          ) : null}

          <AgendaList items={queryError ? [] : items} canWrite={canWrite} />
        </div>
      </div>
    </AppShell>
  );
}
