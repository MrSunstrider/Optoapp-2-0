import { 
  Calendar, 
  Package, 
  DollarSign, 
  AlertTriangle, 
  CheckCircle2, 
  Activity,
  TrendingUp,
  ArrowRight,
  ClipboardList
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { fetchDashboardKpis } from "@/lib/dashboard-kpis";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule } from "@/lib/roles";
import { fetchSyncSnapshot } from "@/lib/sync";
import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { redirect } from "next/navigation";

export default async function DashboardPage() {
  const supabase = await createClient();

  const {
    data: { user }
  } = await supabase.auth.getUser();

  const activeOptica = await getActiveOpticaContext();

  if (!activeOptica) redirect("/seleccion-optica");

  const [snapshot, fiscal, syncSnapshot] = await Promise.all([
    fetchDashboardKpis(supabase, activeOptica.opticaId),
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    fetchSyncSnapshot(supabase, activeOptica.opticaId)
  ]);

  const kpis = snapshot.kpis;
  const today = dateOnly(new Date());
  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);
  const alertas = buildAlertas(kpis.entregasPendientes, kpis.serviciosPendientes);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="mx-auto w-full max-w-7xl space-y-10 py-6">
        <header className="flex flex-col justify-between gap-6 md:flex-row md:items-end border-b border-border/50 pb-10">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary">
              <Activity className="h-3 w-3" />
              SISTEMA OPERATIVO
            </div>
            <h1 className="font-heading text-4xl font-black tracking-tight text-foreground sm:text-5xl">
              Dashboard <span className="text-primary">Clínico</span>
            </h1>
            <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm font-medium text-muted-foreground/80">
              <span className="flex items-center gap-2">
                {opticaLine}
              </span>
              <span className="hidden md:inline text-border/40">•</span>
              <span className="flex items-center gap-2">
                <Calendar className="h-4 w-4 opacity-40" />
                {today}
              </span>
            </div>
          </div>
          
          <div className="flex items-center gap-4">
            {user?.email && (
              <div className="flex items-center gap-3 rounded-2xl bg-foreground/[0.03] px-5 py-3 border border-border/40">
                <div className="h-8 w-8 rounded-full bg-primary/20 flex items-center justify-center text-primary font-black text-xs">
                  {user.email.charAt(0).toUpperCase()}
                </div>
                <div className="flex flex-col">
                  <span className="text-xs font-black uppercase tracking-tighter text-foreground">{user.email.split('@')[0]}</span>
                  <span className="text-[10px] font-medium text-muted-foreground">{user.email}</span>
                </div>
              </div>
            )}
          </div>
        </header>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-4">
          {/* Main KPI Cards */}
          <div className="lg:col-span-3 grid grid-cols-1 md:grid-cols-2 gap-6">
            <BentoKpi 
              etiqueta="Citas del Día" 
              valor={String(kpis.citasHoy)} 
              subtexto="Pacientes agendados"
              icon={<Calendar className="h-6 w-6 text-primary" />} 
              href="/agenda"
            />
            <BentoKpi 
              etiqueta="Entregas OT" 
              valor={String(kpis.entregasPendientes)} 
              subtexto="Pendientes de entrega"
              icon={<Package className="h-6 w-6 text-amber-500" />} 
              highlight={kpis.entregasPendientes > 0}
              href="/pacientes?filtro=pendientes"
            />
            <BentoKpi 
              etiqueta="Cobros Hoy" 
              valor={money(kpis.cobrosDia)} 
              subtexto="Ingresos liquidados"
              icon={<DollarSign className="h-6 w-6 text-emerald-500" />} 
              href="/reportes"
            />
            <BentoKpi 
              etiqueta="Alertas Stock" 
              valor={String(kpis.stockCritico)} 
              subtexto="Insumos bajo límite"
              icon={<AlertTriangle className="h-6 w-6 text-destructive" />} 
              highlight={kpis.stockCritico > 0}
              href="/configuracion/inventario"
            />
          </div>

          {/* Right Section - Alerts & Health */}
          <div className="space-y-6">
            <section className="rounded-3xl border border-border/40 bg-gradient-to-br from-card to-card/80 p-6 shadow-sm">
              <div className="flex items-center justify-between mb-5">
                <h2 className="font-heading text-lg font-bold text-foreground">Alertas</h2>
                <div className="rounded-full bg-foreground/[0.03] p-2">
                  <TrendingUp className="h-4 w-4 text-primary/60" />
                </div>
              </div>
              
              <div className="space-y-3">
                {alertas.length === 0 ? (
                  <div className="flex flex-col items-center justify-center py-10 text-center space-y-4">
                    <div className="h-14 w-14 rounded-2xl bg-gradient-to-br from-emerald-500/15 to-emerald-500/5 flex items-center justify-center shadow-inner">
                      <CheckCircle2 className="h-7 w-7 text-emerald-500" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-foreground uppercase tracking-widest leading-relaxed">
                        Todo en orden
                      </p>
                      <p className="text-[10px] font-medium text-muted-foreground/60 mt-1">
                        Sin incidencias pendientes
                      </p>
                    </div>
                  </div>
                ) : (
                  alertas.map((alerta, idx) => (
                    <div
                      key={idx}
                      className={`flex gap-3 rounded-2xl p-4 text-xs font-medium transition-all ${
                        alerta.tipo === "critica"
                          ? "bg-destructive/5 text-destructive border border-destructive/10"
                          : "bg-primary/5 text-primary border border-primary/10"
                      }`}
                    >
                      <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
                      <p className="leading-relaxed">{alerta.texto}</p>
                    </div>
                  ))
                )}
              </div>
            </section>

            {/* Quick Actions / Status */}
            <div className="rounded-3xl bg-gradient-to-br from-foreground/[0.02] to-foreground/[0.01] border border-border/40 p-6">
              <h3 className="text-[10px] font-black uppercase tracking-[0.15em] text-muted-foreground/60 mb-4">Acceso Rápido</h3>
              <div className="space-y-1">
                <Link href="/reportes" className="flex items-center justify-between p-3 rounded-xl hover:bg-card hover:shadow-sm transition-all group">
                  <span className="text-xs font-bold text-foreground">Ver Reportes</span>
                  <ArrowRight className="h-4 w-4 text-muted-foreground/40 group-hover:text-primary transition-colors" />
                </Link>
                <div className="h-px bg-border/30 mx-3" />
                <Link href="/pacientes" className="flex items-center justify-between p-3 rounded-xl hover:bg-card hover:shadow-sm transition-all group">
                  <span className="text-xs font-bold text-foreground">Lista de Pacientes</span>
                  <ArrowRight className="h-4 w-4 text-muted-foreground/40 group-hover:text-primary transition-colors" />
                </Link>
                <div className="h-px bg-border/30 mx-3" />
                <Link href="/sincronizar" className="flex items-center justify-between p-3 rounded-xl hover:bg-card hover:shadow-sm transition-all group">
                  <span className="text-xs font-bold text-foreground">Sincronizar Cloud</span>
                  <ArrowRight className="h-4 w-4 text-muted-foreground/40 group-hover:text-primary transition-colors" />
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}

function BentoKpi({
  etiqueta,
  valor,
  subtexto,
  icon,
  highlight = false,
  href
}: {
  etiqueta: string;
  valor: string;
  subtexto: string;
  icon: React.ReactNode;
  highlight?: boolean;
  href: string;
}) {
  return (
    <Link href={href} className="group relative flex flex-col justify-between overflow-hidden rounded-[2.5rem] border border-border/50 bg-card p-8 shadow-sm transition-all duration-300 hover:shadow-lg hover:-translate-y-1 hover:border-primary/20">
      <div className="flex items-center justify-between">
        <div className="rounded-2xl bg-gradient-to-br from-foreground/[0.03] to-foreground/[0.06] p-3.5 group-hover:from-primary/10 group-hover:to-primary/5 transition-all duration-300 shadow-inner">
          {icon}
        </div>
        {highlight && (
          <div className="flex h-2.5 w-2.5">
            <div className="absolute inline-flex h-2.5 w-2.5 animate-ping rounded-full bg-destructive opacity-75"></div>
            <div className="relative inline-flex h-2.5 w-2.5 rounded-full bg-destructive"></div>
          </div>
        )}
      </div>
      
      <div className="mt-8">
        <p className="text-[10px] font-black uppercase tracking-[0.15em] text-muted-foreground/60">
          {etiqueta}
        </p>
        <div className="flex items-baseline gap-2 mt-1.5">
          <p className="font-heading text-4xl font-black text-foreground tabular-nums tracking-tighter">
            {valor}
          </p>
        </div>
        <p className="mt-2 text-xs font-medium text-muted-foreground/60">{subtexto}</p>
      </div>

      <div className="absolute bottom-5 right-8 opacity-0 transition-all duration-300 group-hover:opacity-100 group-hover:translate-x-0 translate-x-3">
        <div className="flex items-center gap-1.5 text-[10px] font-black uppercase tracking-widest text-primary">
          <span>Ver más</span>
          <ArrowRight className="h-3.5 w-3.5" />
        </div>
      </div>
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

function dateOnly(date: Date): string {
  return date.toLocaleDateString("es-PE", {
    year: "numeric",
    month: "long",
    day: "numeric"
  });
}

function buildAlertas(entregasPendientes: number, serviciosPendientes: number) {
  const alertas: Array<{ texto: string; tipo: "critica" | "info" }> = [];
  if (entregasPendientes > 0) {
    alertas.push({
      tipo: entregasPendientes >= 5 ? "critica" : "info",
      texto: `Hay ${entregasPendientes} entregas pendientes con fecha vencida.`
    });
  }
  if (serviciosPendientes > 0) {
    alertas.push({
      tipo: serviciosPendientes >= 5 ? "critica" : "info",
      texto: `Tienes ${serviciosPendientes} servicios extra por cobrar.`
    });
  }
  return alertas;
}
