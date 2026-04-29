import { redirect } from "next/navigation";
import { AppShell } from "@/components/app-shell";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canViewBiAndReports } from "@/lib/roles";
import {
  fetchReporteFinanciero,
  type ReportePeriodo
} from "@/lib/reportes-financieros";

export default async function ReportesPage({
  searchParams
}: {
  searchParams: Promise<{ periodo?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  if (!canViewBiAndReports(activeOptica.rol)) {
    return (
      <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
        <h1 className="text-2xl font-semibold mb-2">Reportes</h1>
        <p className="text-sm text-slate-600">
          Tu rol actual no tiene permiso para ver reportes financieros.
        </p>
      </AppShell>
    );
  }

  const params = await searchParams;
  const periodo = normalizePeriodo(params.periodo);
  const supabase = await createClient();
  const data = await fetchReporteFinanciero(
    supabase,
    activeOptica.opticaId,
    periodo
  );

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-semibold">Reportes financieros</h1>
        <form method="get" className="flex items-center gap-2">
          <label className="text-sm text-slate-700">Periodo</label>
          <select
            name="periodo"
            defaultValue={periodo}
            className="rounded border px-2 py-1 text-sm"
          >
            <option value="dia">Día</option>
            <option value="semana">Semana</option>
            <option value="mes">Mes</option>
            <option value="anio">Año</option>
          </select>
          <button className="rounded border px-3 py-1 text-sm">Aplicar</button>
        </form>
      </div>

      <p className="text-sm text-slate-600 mb-4">
        Rango: {data.fechaInicio} a {data.fechaFinExclusiva} (fin exclusivo)
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-5 gap-4">
        <Kpi title={`Ingresos cobrados (${data.periodoLabel})`} value={money(data.ingresosCobrados)} />
        <Kpi title={`Ventas emitidas (${data.periodoLabel})`} value={money(data.ventasEmitidas)} />
        <Kpi title="Saldo pendiente" value={money(data.saldoPendiente)} />
        <Kpi title="Movimientos" value={String(data.totalMovimientos)} />
        <Kpi title="Ticket promedio" value={money(data.ticketPromedio)} />
      </div>
    </AppShell>
  );
}

function Kpi({ title, value }: { title: string; value: string }) {
  return (
    <div className="rounded border bg-white p-4 shadow-sm">
      <p className="text-xs text-slate-600">{title}</p>
      <p className="text-xl font-semibold mt-1">{value}</p>
    </div>
  );
}

function money(value: number): string {
  return `S/ ${value.toFixed(2)}`;
}

function normalizePeriodo(value?: string): ReportePeriodo {
  if (value === "dia" || value === "semana" || value === "mes" || value === "anio") {
    return value;
  }
  return "mes";
}
