import { 
  BarChart3, 
  TrendingUp, 
  Wallet, 
  History, 
  CircleDollarSign,
  Filter,
  Calendar,
  ArrowUpRight
} from "lucide-react";
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
        <div className="flex h-[60vh] flex-col items-center justify-center text-center space-y-4">
           <div className="h-16 w-16 rounded-full bg-destructive/10 flex items-center justify-center text-destructive">
             <Filter className="h-8 w-8" />
           </div>
           <h1 className="text-2xl font-black text-foreground">Acceso Restringido</h1>
           <p className="text-sm font-medium text-muted-foreground max-w-xs">
             Tu rol actual no tiene permisos para visualizar reportes financieros detallados.
           </p>
        </div>
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
      <div className="mx-auto w-full max-w-7xl space-y-10 py-6">
        <header className="flex flex-col justify-between gap-6 md:flex-row md:items-end border-b border-border/50 pb-10">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary">
              <BarChart3 className="h-3 w-3" />
              ANÁLISIS DE NEGOCIO
            </div>
            <h1 className="font-heading text-4xl font-black tracking-tight text-foreground sm:text-5xl">
              Reportes <span className="text-primary">Financieros</span>
            </h1>
            <p className="text-sm font-medium text-muted-foreground">
              Periodo consultado: <span className="text-foreground font-bold">{data.fechaInicio}</span> al <span className="text-foreground font-bold">{data.fechaFinExclusiva}</span>
            </p>
          </div>

          <form method="get" className="flex flex-wrap items-center gap-3">
            <div className="relative group">
              <Calendar className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground/50 transition-colors group-focus-within:text-primary" />
              <select
                name="periodo"
                defaultValue={periodo}
                className="h-12 w-full appearance-none rounded-2xl border border-border bg-card pl-11 pr-10 text-xs font-black uppercase tracking-widest text-foreground outline-none ring-primary/20 transition-all focus:ring-4"
              >
                <option value="dia">Día Actual</option>
                <option value="semana">Última Semana</option>
                <option value="mes">Mes en Curso</option>
                <option value="anio">Año Fiscal</option>
              </select>
            </div>
            <button className="h-12 rounded-2xl bg-primary px-8 text-xs font-black uppercase tracking-widest text-primary-foreground shadow-xl shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-all">
              Filtrar Datos
            </button>
          </form>
        </header>

        <div className="flex justify-end">
          <a
            href={`/api/reportes/pdf?periodo=${periodo}`}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 rounded-2xl bg-primary px-6 py-3.5 text-xs font-black uppercase tracking-widest text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
          >
            <svg viewBox="0 0 24 24" width={16} height={16} fill="none" stroke="currentColor" strokeWidth={2.5}>
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            Exportar PDF
          </a>
        </div>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          <KpiCard 
            title="Ingresos Cobrados" 
            value={money(data.ingresosCobrados)} 
            label={data.periodoLabel}
            icon={<TrendingUp className="h-5 w-5 text-emerald-500" />}
            variant="success"
          />
          <KpiCard 
            title="Ventas Emitidas" 
            value={money(data.ventasEmitidas)} 
            label={data.periodoLabel}
            icon={<Wallet className="h-5 w-5 text-primary" />}
            variant="default"
          />
          <KpiCard 
            title="Saldo Pendiente" 
            value={money(data.saldoPendiente)} 
            label="Cuentas por cobrar"
            icon={<CircleDollarSign className="h-5 w-5 text-destructive" />}
            variant="destructive"
          />
          <KpiCard 
            title="Movimientos" 
            value={String(data.totalMovimientos)} 
            label="Total transacciones"
            icon={<History className="h-5 w-5 text-amber-500" />}
            variant="default"
          />
          <KpiCard 
            title="Ticket Promedio" 
            value={money(data.ticketPromedio)} 
            label="Valor por venta"
            icon={<BarChart3 className="h-5 w-5 text-blue-500" />}
            variant="default"
          />
        </div>

        {/* Desglose de cobros del período */}
        <div className="rounded-2xl border border-border/50 bg-foreground/[0.02] p-6 space-y-3">
          <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">
            Cobros del período
          </p>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Ventas del período</span>
            <span className="font-bold text-emerald-500">{money(data.ingresosPeriodoActual)}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Cobros de períodos anteriores</span>
            <span className="font-bold text-amber-500">{money(data.ingresosPeriodosAnteriores)}</span>
          </div>
          <hr className="border-border/50" />
          <div className="flex justify-between text-sm font-bold">
            <span>Total cobrado</span>
            <span className="text-primary">{money(data.ingresosPeriodoActual + data.ingresosPeriodosAnteriores)}</span>
          </div>
        </div>
      </div>
    </AppShell>
  );
}

function KpiCard({ 
  title, 
  value, 
  label, 
  icon, 
  variant = "default" 
}: { 
  title: string; 
  value: string; 
  label: string;
  icon: React.ReactNode;
  variant?: "default" | "success" | "destructive";
}) {
  return (
    <div className="group rounded-3xl border border-border/50 bg-card p-8 shadow-sm transition-all hover:shadow-xl hover:border-primary/20">
      <div className="flex items-center justify-between mb-6">
        <div className={`rounded-2xl p-3 bg-foreground/[0.03] group-hover:bg-primary/10 transition-colors`}>
          {icon}
        </div>
        <ArrowUpRight className="h-4 w-4 text-muted-foreground/30 group-hover:text-primary transition-colors" />
      </div>
      <div className="space-y-1">
        <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">{title}</p>
        <p className={`font-heading text-3xl font-black text-foreground tabular-nums tracking-tighter`}>
          {value}
        </p>
        <p className="text-[10px] font-bold text-muted-foreground/40 uppercase tracking-tighter">{label}</p>
      </div>
    </div>
  );
}

function money(value: number): string {
  return new Intl.NumberFormat("es-PE", {
    style: "currency",
    currency: "PEN",
    minimumFractionDigits: 2
  }).format(value);
}

function normalizePeriodo(value?: string): ReportePeriodo {
  if (value === "dia" || value === "semana" || value === "mes" || value === "anio") {
    return value;
  }
  return "mes";
}
