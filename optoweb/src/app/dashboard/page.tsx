import { Activity, TrendingUp, TrendingDown, Package, Eye, Calendar, ArrowRight } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { fetchDashboardKpis } from "@/lib/dashboard-kpis";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchSyncSnapshot } from "@/lib/sync";
import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { redirect } from "next/navigation";

export default async function DashboardPage() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  const [snapshot, fiscal, syncSnapshot] = await Promise.all([
    fetchDashboardKpis(supabase, activeOptica.opticaId),
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    fetchSyncSnapshot(supabase, activeOptica.opticaId)
  ]);

  const kpis = snapshot.kpis;
  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="mx-auto w-full max-w-7xl space-y-6 py-6">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border/50 pb-6">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary">
              <Activity className="h-3 w-3" />
              PANEL DE ESTADÍSTICAS
            </div>
            <h1 className="mt-2 font-heading text-3xl font-black tracking-tight text-foreground">
              Dashboard <span className="text-primary">Clínico</span>
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">{opticaLine}</p>
          </div>
          {user?.email && (
            <div className="hidden sm:flex items-center gap-3 rounded-2xl bg-foreground/[0.03] px-5 py-3 border border-border/40">
              <div className="h-8 w-8 rounded-full bg-primary/20 flex items-center justify-center text-primary font-black text-xs">
                {user.email.charAt(0).toUpperCase()}
              </div>
              <span className="text-xs font-medium text-muted-foreground">{user.email}</span>
            </div>
          )}
        </div>

        {/* Mini KPIs */}
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <MiniKpi label="Exámenes hoy" value={String(kpis.citasHoy)} icon={<Activity className="h-5 w-5" />} />
          <MiniKpi label="Entregas pend." value={String(kpis.entregasPendientes)} icon={<Package className="h-5 w-5" />} highlight={kpis.entregasPendientes > 0} />
          <MiniKpi label="Cobros hoy" value={money(kpis.cobrosDia)} icon={<TrendingUp className="h-5 w-5" />} />
          <MiniKpi label="Stock crítico" value={String(kpis.stockCritico)} icon={<Activity className="h-5 w-5" />} highlight={kpis.stockCritico > 0} />
        </div>

        {/* Main grid */}
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">

          {/* Exams card (span 2) */}
          <div className="lg:col-span-2 space-y-6">
            <StatCard title="Exámenes Realizados" current={String(kpis.citasHoy)} previous={`Mes anterior: 0`}>
              <div className="mt-4 flex items-end gap-2">
                <Bar value={kpis.citasHoy} label="Actual" color="primary" />
                <Bar value={0} label="Anterior" color="muted" />
              </div>
            </StatCard>

            {/* Operations card */}
            <section className="rounded-3xl border border-border bg-card p-6 shadow-sm">
              <h2 className="font-heading text-lg font-bold text-foreground">Operación e inventario</h2>
              <div className="mt-4 space-y-3">
                <MetricRow label="Entregas pendientes (período)" value={String(kpis.entregasPendientes)} />
                <MetricRow label="Servicios pendientes" value={String(kpis.serviciosPendientes)} />
                <MetricRow label="Stock crítico" value={String(kpis.stockCritico)} highlight={kpis.stockCritico > 0} />
              </div>
              <p className="mt-4 text-xs text-muted-foreground/60">Datos del período activo.</p>
            </section>
          </div>

          {/* Right column */}
          <div className="space-y-6">
            {/* Sync status */}
            <section className="rounded-3xl border border-border bg-card p-6 shadow-sm">
              <h3 className="font-heading text-sm font-bold text-foreground">Sincronización</h3>
              <div className="mt-4 space-y-3">
                {syncSnapshot.entities.map((e) => (
                  <div key={e.key} className="flex items-center justify-between">
                    <span className="text-xs text-muted-foreground">{e.label}</span>
                    <span className="text-sm font-bold">{e.successCount}</span>
                  </div>
                ))}
              </div>
            </section>

            {/* Quick links */}
            <div className="rounded-3xl border border-border bg-card p-6 shadow-sm">
              <h3 className="font-heading text-sm font-bold text-foreground">Acceso rápido</h3>
              <div className="mt-4 space-y-1">
                <QuickLink href="/estadisticas" label="Estadísticas completas" />
                <QuickLink href="/reportes" label="Reportes financieros" />
                <QuickLink href="/sincronizar" label="Sincronizar Cloud" />
                <QuickLink href="/inventario" label="Inventario" />
              </div>
            </div>
          </div>

        </div>
      </div>
    </AppShell>
  );
}

function MiniKpi({ label, value, icon, highlight = false }: { label: string; value: string; icon: React.ReactNode; highlight?: boolean }) {
  return (
    <div className="relative rounded-2xl border border-border bg-card p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground/60">{label}</span>
        <div className={`rounded-lg p-1.5 ${highlight ? "bg-destructive/10 text-destructive" : "bg-primary/10 text-primary"}`}>
          {icon}
        </div>
      </div>
      <p className={`mt-2 font-heading text-2xl font-black tabular-nums tracking-tighter ${highlight ? "text-destructive" : "text-foreground"}`}>
        {value}
      </p>
    </div>
  );
}

function StatCard({ title, current, previous, children }: { title: string; current: string; previous: string; children: React.ReactNode }) {
  return (
    <section className="rounded-3xl border border-border bg-card p-6 shadow-sm">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs text-muted-foreground">{title}</p>
          <p className="font-heading text-3xl font-black text-foreground tabular-nums mt-1">{current}</p>
          <p className="text-xs text-muted-foreground/60 mt-0.5">{previous}</p>
        </div>
      </div>
      {children}
    </section>
  );
}

function Bar({ value, label, color }: { value: number; label: string; color: string }) {
  const maxH = 80;
  const h = Math.min(value * 8, maxH);
  return (
    <div className="flex flex-col items-center gap-1">
      <div className={`w-12 rounded-lg bg-${color} transition-all`} style={{ height: `${Math.max(h, 4)}px` }} />
      <span className="text-[10px] font-medium text-muted-foreground/60">{label}</span>
    </div>
  );
}

function MetricRow({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="flex items-center justify-between border-b border-border/30 pb-2 last:border-0">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className={`text-sm font-bold ${highlight ? "text-destructive" : "text-foreground"}`}>{value}</span>
    </div>
  );
}

function QuickLink({ href, label }: { href: string; label: string }) {
  return (
    <Link href={href} className="flex items-center justify-between rounded-xl px-3 py-2.5 text-sm font-medium text-muted-foreground hover:bg-foreground/5 hover:text-foreground transition-all group">
      {label}
      <ArrowRight className="h-3.5 w-3.5 text-muted-foreground/30 group-hover:text-primary transition-colors" />
    </Link>
  );
}

function money(value: number): string {
  return new Intl.NumberFormat("es-PE", {
    style: "currency",
    currency: "PEN",
    maximumFractionDigits: 0
  }).format(value);
}
