import { AppShell } from "@/components/app-shell";

import { createClient } from "@/lib/supabase/server";

import { getActiveOpticaContext } from "@/lib/optica-context";

import { fetchDashboardKpis } from "@/lib/dashboard-kpis";

import { redirect } from "next/navigation";

export default async function DashboardPage() {
  const supabase = await createClient();

  const {
    data: { user }
  } = await supabase.auth.getUser();

  const activeOptica = await getActiveOpticaContext();

  if (!activeOptica) redirect("/seleccion-optica");

  const snapshot = await fetchDashboardKpis(supabase, activeOptica.opticaId);

  const kpis = snapshot.kpis;

  const errs = snapshot.status.erroresPorFuente;

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <h1 className="mb-2 text-2xl font-semibold tracking-tight">
        Operación de Hoy
      </h1>

      <p className="mb-4 text-sm text-muted-foreground">
        Sesion activa: {user?.email ?? "sin email"}
      </p>

      <p className="mb-4 text-sm text-muted-foreground">
        Optica activa: {activeOptica.nombre} ({activeOptica.rol})
      </p>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <KpiCard title="Ventas del dia" value={money(kpis.ventasDia)} />

        <KpiCard title="Ventas del mes" value={money(kpis.ventasMes)} />

        <KpiCard title="Pacientes hoy" value={String(kpis.pacientesHoy)} />

        <KpiCard title="Saldos pendientes" value={money(kpis.saldosPendientes)} />
      </div>

      <div className="mt-6 rounded-xl border border-border bg-card p-5 shadow-sm">
        <h2 className="mb-2 text-sm font-semibold">Estado operativo</h2>

        <p className="mb-2 text-xs">
          Salud general:{" "}
          <span
            className={
              snapshot.status.overall === "ok"
                ? "font-medium text-emerald-700"
                : "font-medium text-amber-700"
            }
          >
            {snapshot.status.overall === "ok" ? "OK" : "Degradado"}
          </span>
        </p>

        <p className="mb-2 text-xs text-muted-foreground">
          Ultima actualizacion: {formatIso(snapshot.status.lastUpdatedIso)}
        </p>

        <ul className="space-y-2 text-xs text-foreground">
          <FuenteLinea
            ok={snapshot.status.sources.pagosDia}
            titulo="Fuente pagos (dia)"
            detalle={errs.pagosDia}
          />

          <FuenteLinea
            ok={snapshot.status.sources.pagosMes}
            titulo="Fuente pagos (mes)"
            detalle={errs.pagosMes}
          />

          <FuenteLinea
            ok={snapshot.status.sources.pacientesHoy}
            titulo="Fuente pacientes (hoy)"
            detalle={errs.pacientesHoy}
          />

          <FuenteLinea
            ok={snapshot.status.sources.dispensaciones}
            titulo="Fuente dispensaciones (saldo)"
            detalle={errs.dispensaciones}
          />

          <FuenteLinea
            ok={snapshot.status.sources.serviciosExtra}
            titulo="Fuente servicios extra (saldo)"
            detalle={errs.serviciosExtra}
          />
        </ul>

        {snapshot.status.overall === "degraded" && (
          <p className="mt-3 text-xs text-amber-800 dark:text-amber-200">
            Los KPI pueden estar incompletos hasta que todas las consultas respondan.
            Si ves permisos o RLS, revisa rol en Supabase y políticas por{" "}
            <code className="text-[11px]">optica_id</code>.
          </p>
        )}
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
      <span className={ok ? "text-emerald-700" : "text-red-700"}>
        {ok ? "OK" : "Error"}
      </span>
      {!ok && detalle && (
        <span className="mt-0.5 block whitespace-pre-wrap text-[11px] text-red-600">
          {detalle}
        </span>
      )}
    </li>
  );
}

function money(value: number): string {
  return `S/ ${value.toFixed(2)}`;
}

function formatIso(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return value;

  return date.toLocaleString("es-PE");
}

function KpiCard({ title, value }: { title: string; value: string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
      <p className="text-sm font-medium text-muted-foreground">{title}</p>

      <p className="mt-2 text-3xl font-bold tracking-tight">{value}</p>
    </div>
  );
}
