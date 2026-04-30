import Link from "next/link";
import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { InventarioSearch } from "@/components/inventario/inventario-search";
import { InventarioSummaryCard } from "@/components/inventario/inventario-summary-card";
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
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-5xl space-y-4">
          <p className="text-xs text-zinc-400">{opticaLine}</p>
          <div className="flex items-center justify-between gap-3">
            <h1 className="text-4xl font-semibold">Inventario de Monturas</h1>
            <Link href="/inventario/nuevo" className="text-5xl leading-none text-zinc-100">
              +
            </Link>
          </div>

          {query.msg === "guardado" || query.msg === "actualizado" ? (
            <p className="rounded-lg border border-emerald-700/40 bg-emerald-950/30 px-3 py-2 text-sm text-emerald-300">
              Montura {query.msg === "guardado" ? "guardada" : "actualizada"} correctamente.
            </p>
          ) : null}

          <InventarioSearch initial={q} />
          <StockAlertCard items={items} />
          <InventarioSummaryCard summary={summary} q={q} />
          <MonturaList items={items} canWrite={canWrite} />
        </div>
      </div>
    </AppShell>
  );
}
