import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { fetchDashboardKpis } from "@/lib/dashboard-kpis";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canViewBiAndReports } from "@/lib/roles";
import { fetchSyncSnapshot } from "@/lib/sync";
import { createClient } from "@/lib/supabase/server";

export default async function EstadisticasPage() {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  if (!canViewBiAndReports(activeOptica.rol)) {
    return (
      <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
        <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
          <section className="mx-auto w-full max-w-5xl rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
            <h1 className="text-2xl font-semibold tracking-tight text-white">Estadisticas</h1>
            <p className="mt-2 text-sm text-amber-300">
              Tu rol actual no tiene permiso para ver BI y reportes.
            </p>
          </section>
        </div>
      </AppShell>
    );
  }

  const supabase = await createClient();
  const [dashboard, sync] = await Promise.all([
    fetchDashboardKpis(supabase, activeOptica.opticaId),
    fetchSyncSnapshot(supabase, activeOptica.opticaId)
  ]);

  const k = dashboard.kpis;
  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-5xl space-y-5">
          <header>
            <h1 className="text-2xl font-semibold tracking-tight text-white">
              Estadisticas operativas
            </h1>
            <p className="mt-1 text-xs text-zinc-400">
              Snapshot integrado de ventas, cartera, agenda, inventario y sincronizacion.
            </p>
          </header>

          <section className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
            <KpiCard label="Ventas del dia" value={money(k.ventasDia)} />
            <KpiCard label="Ventas del mes" value={money(k.ventasMes)} />
            <KpiCard label="Cobros del dia" value={money(k.cobrosDia)} />
            <KpiCard label="Pacientes hoy" value={String(k.pacientesHoy)} />
            <KpiCard label="Citas hoy" value={String(k.citasHoy)} />
            <KpiCard label="Entregas pendientes" value={String(k.entregasPendientes)} />
            <KpiCard label="Servicios pendientes" value={String(k.serviciosPendientes)} />
            <KpiCard label="Stock critico" value={String(k.stockCritico)} />
            <KpiCard label="Saldos pendientes" value={money(k.saldosPendientes)} />
          </section>

          <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
            <h2 className="text-lg font-semibold text-white">Estado de sincronizacion</h2>
            <p className="mt-2 text-sm text-zinc-200">
              Estado global:{" "}
              <span
                className={
                  sync.globalStatus === "ok" ? "font-semibold text-emerald-300" : "font-semibold text-amber-300"
                }
              >
                {sync.globalStatus.toUpperCase()}
              </span>
            </p>
            <p className="mt-1 text-xs text-zinc-400">
              Ultimo exito: {sync.lastSuccessAt ?? "Sin registros"} · Ultima actualizacion:{" "}
              {sync.updatedAt ?? "Sin registros"}
            </p>
          </section>
        </div>
      </div>
    </AppShell>
  );
}

function KpiCard({ label, value }: { label: string; value: string }) {
  return (
    <article className="rounded-xl border border-zinc-700 bg-black/15 p-4">
      <p className="text-xs text-zinc-400">{label}</p>
      <p className="mt-1 text-xl font-semibold text-zinc-100">{value}</p>
    </article>
  );
}

function money(value: number): string {
  return `S/ ${value.toFixed(2)}`;
}
