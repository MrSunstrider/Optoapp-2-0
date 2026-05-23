import Link from "next/link";
import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { InventarioSearch } from "@/components/inventario/inventario-search";
import { InventarioSummaryCard } from "@/components/inventario/inventario-summary-card";
import { DashboardExportActions } from "@/components/dashboard/dashboard-export-actions";
import { MonturaList } from "@/components/inventario/montura-list";
import { StockAlertCard } from "@/components/inventario/stock-alert-card";
import {
  computeInventarioSummary,
  fetchMonturasInventario
} from "@/lib/inventario";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule, canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

export default async function InventarioPage({
  searchParams
}: {
  searchParams: Promise<{ q?: string; msg?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canAccessModule(activeOptica.rol, "inventario")) redirect("/dashboard");

  const query = await searchParams;
  const q = String(query.q ?? "");
  const supabase = await createClient();
  const [fiscal, items] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    fetchMonturasInventario(supabase, activeOptica.opticaId, q)
  ]);
  const summary = computeInventarioSummary(items);
  const canWrite = canManagePacientes(activeOptica.rol);
  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="min-h-screen bg-background p-4 sm:p-8 text-foreground transition-colors duration-300">
        <div className="mx-auto w-full max-w-6xl space-y-6">
          <div className="flex flex-col gap-1">
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-primary/70">{opticaLine}</p>
            <div className="flex items-center justify-between gap-4">
              <h1 className="font-heading text-4xl font-black tracking-tight text-foreground sm:text-5xl">Inventario</h1>
              <Link href="/inventario/nuevo" className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-2xl font-bold text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-110 active:scale-95" title="Nueva montura">
                +
              </Link>
            </div>
          </div>

          {query.msg === "guardado" || query.msg === "actualizado" ? (
            <p className="rounded-lg border border-emerald-700/40 bg-emerald-950/30 px-3 py-2 text-sm text-emerald-300">
              Montura {query.msg === "guardado" ? "guardada" : "actualizada"} correctamente.
            </p>
          ) : null}

          <InventarioSearch initial={q} />
          
          <section className="rounded-3xl border border-border bg-card p-6 shadow-xl shadow-foreground/[0.02]">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="font-heading text-lg font-bold text-foreground">Exportaciones Rápidas</h2>
                <p className="text-sm font-medium text-muted-foreground">Reportes operativos e inventario del día.</p>
              </div>
            </div>
            <DashboardExportActions />
          </section>

          <StockAlertCard items={items} />
          <InventarioSummaryCard summary={summary} q={q} />
          <MonturaList items={items} canWrite={canWrite} />
        </div>
      </div>
    </AppShell>
  );
}
