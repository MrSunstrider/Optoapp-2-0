import { 
  TrendingUp, 
  Users, 
  Calendar, 
  Package, 
  AlertCircle, 
  Wallet,
  Clock,
  ShieldCheck,
  ShieldAlert,
  ArrowRight
} from "lucide-react";
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
        <div className="flex h-[60vh] flex-col items-center justify-center text-center space-y-4">
           <div className="h-16 w-16 rounded-full bg-destructive/10 flex items-center justify-center text-destructive">
             <ShieldAlert className="h-8 w-8" />
           </div>
           <h1 className="text-2xl font-black text-foreground uppercase tracking-tight">Acceso Restringido</h1>
           <p className="text-sm font-medium text-muted-foreground max-w-xs">
             Tu rol actual no tiene permisos para visualizar estadísticas avanzadas de BI.
           </p>
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
      <div className="mx-auto w-full max-w-7xl space-y-6 py-6">
        <header className="flex flex-col gap-3 border-b border-border/50 pb-6">
          <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary w-fit">
            <TrendingUp className="h-3 w-3" />
            BUSINESS INTELLIGENCE
          </div>
          <h1 className="font-heading text-4xl font-black tracking-tight text-foreground sm:text-5xl">
            Estadísticas <span className="text-primary">Operativas</span>
          </h1>
          <p className="text-sm font-medium text-muted-foreground">
            Snapshot integrado de rendimiento clínico y sincronización de datos.
          </p>
        </header>

        <section className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <StatCard label="Ventas Hoy" value={money(k.ventasDia)} icon={<Wallet className="text-emerald-500" />} />
          <StatCard label="Ventas Mes" value={money(k.ventasMes)} icon={<TrendingUp className="text-primary" />} />
          <StatCard label="Cobros Hoy" value={money(k.cobrosDia)} icon={<ShieldCheck className="text-emerald-500" />} />
          <StatCard label="Pacientes Nuevos" value={String(k.pacientesHoy)} icon={<Users className="text-blue-500" />} />
          <StatCard label="Citas Agendadas" value={String(k.citasHoy)} icon={<Calendar className="text-purple-500" />} />
          <StatCard label="Entregas Pendientes" value={String(k.entregasPendientes)} icon={<Package className="text-amber-500" />} />
          <StatCard label="Servicios Pendientes" value={String(k.serviciosPendientes)} icon={<Clock className="text-indigo-500" />} />
          <StatCard label="Stock Crítico" value={String(k.stockCritico)} icon={<AlertCircle className="text-destructive" />} highlight={k.stockCritico > 0} />
          <StatCard label="Saldos Pendientes" value={money(k.saldosPendientes)} icon={<ArrowRight className="text-muted-foreground/30" />} />
        </section>

        <section className="rounded-[2.5rem] border border-border bg-card p-6 shadow-sm">
          <div className="flex items-center gap-4 mb-6">
            <div className={`h-12 w-12 rounded-2xl flex items-center justify-center ${sync.globalStatus === "ok" ? "bg-emerald-500/10 text-emerald-500" : "bg-amber-500/10 text-amber-500"}`}>
              {sync.globalStatus === "ok" ? <ShieldCheck className="h-6 w-6" /> : <ShieldAlert className="h-6 w-6" />}
            </div>
            <div>
              <h2 className="font-heading text-xl font-bold text-foreground">Estado de Sincronización Cloud</h2>
              <p className="text-xs font-medium text-muted-foreground">Monitoreo de integridad de datos en tiempo real.</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="rounded-2xl bg-foreground/[0.02] p-6 border border-border/50">
               <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/50 mb-2">Estado Global</p>
               <span className={`text-xl font-black uppercase tracking-tighter ${sync.globalStatus === "ok" ? "text-emerald-500" : "text-amber-500"}`}>
                 {sync.globalStatus === "ok" ? "Sistema Sincronizado" : "Sincronización Pendiente"}
               </span>
            </div>
            <div className="rounded-2xl bg-foreground/[0.02] p-6 border border-border/50">
               <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/50 mb-2">Última Actualización</p>
               <p className="text-sm font-bold text-foreground">
                 Éxito: <span className="text-muted-foreground">{sync.lastSuccessAt ?? "—"}</span>
               </p>
               <p className="text-[10px] text-muted-foreground/40 mt-1">Snapshot ID: {sync.updatedAt ?? "—"}</p>
            </div>
          </div>
        </section>
      </div>
    </AppShell>
  );
}

function StatCard({ label, value, icon, highlight = false }: { label: string; value: string; icon: React.ReactNode; highlight?: boolean }) {
  return (
    <div className="group rounded-3xl border border-border bg-card p-6 shadow-sm transition-all hover:shadow-xl hover:border-primary/20">
      <div className="flex items-center justify-between mb-4">
        <div className="rounded-xl bg-foreground/[0.03] p-2.5 transition-colors group-hover:bg-primary/10">
          <div className="h-5 w-5">{icon}</div>
        </div>
        {highlight && (
          <div className="h-2 w-2 rounded-full bg-destructive animate-pulse shadow-[0_0_8px_rgba(239,68,68,0.5)]" />
        )}
      </div>
      <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">{label}</p>
      <p className="font-heading text-3xl font-black text-foreground mt-1 tabular-nums tracking-tighter">{value}</p>
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
