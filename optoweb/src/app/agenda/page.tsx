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
      <div className="min-h-screen bg-background p-4 sm:p-8 text-foreground transition-colors duration-300">
        <div className="mx-auto w-full max-w-4xl space-y-8">
          <div className="flex flex-col gap-1">
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-primary/70">{opticaLine}</p>
            <h1 className="font-heading text-4xl font-black tracking-tight text-foreground sm:text-5xl">
              Agenda <span className="text-primary">Clínica</span>
            </h1>
            <p className="mt-2 text-sm font-medium text-muted-foreground">Gestión de próximas citas y flujo de pacientes.</p>
          </div>

          <div className="space-y-4">
            <AgendaPeriodTabs active={periodo} />
            <div className="flex items-center gap-2 ml-1">
              <div className="h-1.5 w-1.5 rounded-full bg-primary/60"></div>
              <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/50">{bounds.note}</p>
            </div>
          </div>

          {queryError ? (
            <div role="alert" className="rounded-2xl border border-destructive/20 bg-destructive/5 p-6 text-center shadow-sm">
              <p className="text-sm font-bold text-destructive">No se pudo sincronizar la agenda</p>
              <p className="mt-1 text-xs text-destructive/70">Hubo un problema de conexión con el servidor. Por favor, reintenta en unos momentos.</p>
            </div>
          ) : null}

          <AgendaList items={queryError ? [] : items} canWrite={canWrite} />
        </div>
      </div>
    </AppShell>
  );
}
