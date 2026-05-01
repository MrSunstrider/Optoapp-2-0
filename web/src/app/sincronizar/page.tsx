import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { SyncActions } from "@/components/sync/sync-actions";
import { SyncEntityList } from "@/components/sync/sync-entity-list";
import { SyncStatusCard } from "@/components/sync/sync-status-card";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule } from "@/lib/roles";
import { canCloseCierre } from "@/lib/cierre-caja";
import { fetchSyncSnapshot } from "@/lib/sync";
import { createClient } from "@/lib/supabase/server";

export default async function SincronizarPage() {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canAccessModule(activeOptica.rol, "sincronizar")) redirect("/dashboard");

  const supabase = await createClient();
  const [fiscal, snapshot] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    fetchSyncSnapshot(supabase, activeOptica.opticaId)
  ]);
  const canRun = canCloseCierre(activeOptica.rol);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="mx-auto w-full max-w-6xl space-y-8 py-4">
        <div className="mx-auto w-full max-w-5xl space-y-4">
          <h1 className="text-2xl font-semibold tracking-tight text-white">Sincronizar Cloud</h1>
          <p className="text-xs text-zinc-400">
            {formatOpticaActivaLine(activeOptica.nombre, fiscal)}
          </p>
          <SyncStatusCard snapshot={snapshot} />
          <SyncEntityList entities={snapshot.entities} />
          <SyncActions canRun={canRun} snapshot={snapshot} />
        </div>
      </div>
    </AppShell>
  );
}
