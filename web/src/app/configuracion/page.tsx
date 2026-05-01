import { redirect } from "next/navigation";
import { AppShell } from "@/components/app-shell";
import { ModuleCard } from "@/components/config/module-card";
import { CONFIG_MODULES } from "@/lib/config/modules";
import { canAccessConfigModule } from "@/lib/config/permissions";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchDashboardKpis } from "@/lib/dashboard-kpis";
import { createClient } from "@/lib/supabase/server";

export default async function ConfiguracionPage() {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  const supabase = await createClient();
  const snapshot = await fetchDashboardKpis(supabase, activeOptica.opticaId);
  const errs = snapshot.status.erroresPorFuente;

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="min-h-screen bg-background p-4 sm:p-8 text-foreground transition-colors duration-300">
        <div className="mx-auto w-full max-w-5xl space-y-8">
          <div className="flex flex-col gap-1">
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-primary/70">Panel Administrativo</p>
            <h1 className="font-heading text-4xl font-black tracking-tight text-foreground sm:text-5xl">Configuración</h1>
            <p className="mt-2 text-sm font-medium text-muted-foreground">
              Administra la seguridad, operaciones y datos maestros de tu óptica.
            </p>
          </div>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {CONFIG_MODULES.map((item) => (
              <ModuleCard
                key={item.key}
                href={`/configuracion/${item.key}`}
                title={item.title}
                description={item.description}
                blocked={!canAccessConfigModule(activeOptica.rol, item.key)}
              />
            ))}
          </div>

          <section className="rounded-3xl border border-border bg-card p-6 shadow-xl shadow-foreground/[0.02]">
            <h2 className="font-heading text-lg font-bold text-foreground mb-4">Estado del Sistema</h2>
            <div className="flex items-center gap-3 mb-6 p-4 rounded-2xl bg-foreground/[0.02]">
              <div className={`h-3 w-3 rounded-full animate-pulse ${snapshot.status.overall === "ok" ? "bg-emerald-500 shadow-[0_0_12px_rgba(16,185,129,0.5)]" : "bg-amber-500 shadow-[0_0_12px_rgba(245,158,11,0.5)]"}`} />
              <p className="text-sm font-bold text-foreground">
                Estado:{" "}
                <span className={snapshot.status.overall === "ok" ? "text-emerald-500" : "text-amber-500"}>
                  {snapshot.status.overall === "ok" ? "Sincronizado" : "Sincronización parcial"}
                </span>
                <span className="mx-2 text-muted-foreground/30 font-light">|</span>
                <span className="text-xs text-muted-foreground font-medium">Último sync: {formatIso(snapshot.status.lastUpdatedIso)}</span>
              </p>
            </div>
            
            {snapshot.status.overall === "degraded" && (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                <FuenteLinea ok={snapshot.status.sources.pagosDia} titulo="Pagos día" detalle={errs.pagosDia} />
                <FuenteLinea ok={snapshot.status.sources.pagosMes} titulo="Pagos mes" detalle={errs.pagosMes} />
                <FuenteLinea ok={snapshot.status.sources.pacientesHoy} titulo="Pacientes hoy" detalle={errs.pacientesHoy} />
                <FuenteLinea ok={snapshot.status.sources.dispensaciones} titulo="Saldo dispensaciones" detalle={errs.dispensaciones} />
                <FuenteLinea ok={snapshot.status.sources.serviciosExtra} titulo="Saldo servicios extra" detalle={errs.serviciosExtra} />
                <FuenteLinea ok={snapshot.status.sources.citasHoy} titulo="Citas hoy" detalle={errs.citasHoy} />
                <FuenteLinea ok={snapshot.status.sources.entregasPendientes} titulo="Entregas pendientes" detalle={errs.entregasPendientes} />
                <FuenteLinea ok={snapshot.status.sources.serviciosPendientes} titulo="Servicios pendientes" detalle={errs.serviciosPendientes} />
                <FuenteLinea ok={snapshot.status.sources.stockCritico} titulo="Stock crítico" detalle={errs.stockCritico} />
              </div>
            )}
          </section>
        </div>
      </div>
    </AppShell>
  );
}

function FuenteLinea({
  ok,
  titulo,
  detalle
}: {
  ok: boolean;
  titulo: string;
  detalle?: string;
}) {
  return (
    <li className="border-l-2 border-border pl-2">
      <span className="font-medium">{titulo}:</span>{" "}
      <span className={ok ? "text-emerald-500" : "text-amber-500"}>
        {ok ? "OK" : "Error"}
      </span>
      {!ok && detalle && (
        <span className="mt-0.5 block whitespace-pre-wrap text-[11px] text-muted-foreground">
          {detalle}
        </span>
      )}
    </li>
  );
}

function formatIso(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("es-PE");
}
