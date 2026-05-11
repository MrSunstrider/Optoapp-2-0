import { 
  FileText, 
  FileSpreadsheet, 
  Wallet, 
  Calendar as CalendarIcon, 
  ShieldCheck, 
  ShieldAlert
} from "lucide-react";
import Link from "next/link";
import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { CloseActions } from "@/components/cierre-caja/close-actions";
import { DateFilter } from "@/components/cierre-caja/date-filter";
import { PaymentSummaryCards } from "@/components/cierre-caja/payment-summary-cards";
import { TotalCard } from "@/components/cierre-caja/total-card";
import { TransactionsList } from "@/components/cierre-caja/transactions-list";
import {
  canCloseCierre,
  canOverrideCierre,
  canReadCierre,
  fetchCierreCaja,
  fetchCierreFormalStatus,
  formatFechaLarga,
  normalizeFechaCierre
} from "@/lib/cierre-caja";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

export default async function CierreCajaPage({
  searchParams
}: {
  searchParams: Promise<{ fecha?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canAccessModule(activeOptica.rol, "cierre-caja")) redirect("/dashboard");
  if (!canReadCierre(activeOptica.rol)) redirect("/dashboard");

  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  const timezone = user?.user_metadata?.optoapp_timezone as string | undefined;

  const q = await searchParams;
  const fecha = normalizeFechaCierre(q.fecha, timezone);
  const [fiscal, snapshot, formal] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    fetchCierreCaja(supabase, activeOptica.opticaId, fecha),
    fetchCierreFormalStatus(supabase, activeOptica.opticaId, fecha)
  ]);
  const canClose = canCloseCierre(activeOptica.rol);
  const canOverride = canOverrideCierre(activeOptica.rol);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="mx-auto w-full max-w-7xl space-y-10 py-6">
        <header className="flex flex-col justify-between gap-6 md:flex-row md:items-end border-b border-border/50 pb-10">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary">
              <Wallet className="h-3 w-3" />
              GESTIÓN FINANCIERA
            </div>
            <h1 className="font-heading text-4xl font-black tracking-tight text-foreground sm:text-5xl">
              Cierre de <span className="text-primary">Caja</span>
            </h1>
            <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm font-medium text-muted-foreground/80">
              <span className="flex items-center gap-2">
                {formatOpticaActivaLine(activeOptica.nombre, fiscal)}
              </span>
              <span className="hidden md:inline text-border/40">•</span>
              <span className="flex items-center gap-2">
                <CalendarIcon className="h-4 w-4 opacity-40" />
                {formatFechaLarga(fecha)}
              </span>
            </div>
          </div>
          
          <div className="rounded-2xl border border-border/50 bg-card p-1.5 shadow-sm">
            <DateFilter value={fecha} />
          </div>
        </header>

        {snapshot.status.overall === "degraded" && (
          <div className="rounded-2xl border border-amber-500/30 bg-amber-50 px-6 py-4 dark:bg-amber-950/30">
            <div className="flex items-start gap-3">
              <ShieldAlert className="mt-0.5 h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400" />
              <div className="text-sm text-amber-800 dark:text-amber-200">
                <p className="font-bold">Cierre con datos degradados</p>
                <p className="mt-1 text-amber-700 dark:text-amber-300">
                  {snapshot.status.error
                    ? `Error: ${snapshot.status.error}`
                    : "No se pudieron cargar todos los datos. Los montos mostrados pueden estar incompletos."}
                </p>
              </div>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <PaymentSummaryCards
              efectivo={snapshot.efectivo}
              movilTrans={snapshot.movilTrans}
              tarjeta={snapshot.tarjeta}
            />
          </div>
          <div>
            <TotalCard total={snapshot.total} />
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-4 rounded-[2rem] border border-border/50 bg-foreground/[0.02] p-6 shadow-inner">
          <div className="flex gap-2">
            <Link
              href={`/api/cierre-caja/export?fecha=${fecha}&format=pdf`}
              className="flex items-center gap-2 rounded-2xl bg-primary px-6 py-3.5 text-xs font-black uppercase tracking-widest text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
            >
              <FileText className="h-4 w-4" />
              Exportar PDF
            </Link>
            <Link
              href={`/api/cierre-caja/export?fecha=${fecha}&format=csv`}
              className="flex items-center gap-2 rounded-2xl border border-border bg-card px-6 py-3.5 text-xs font-black uppercase tracking-widest text-foreground transition-all hover:bg-foreground/5"
            >
              <FileSpreadsheet className="h-4 w-4" />
              Exportar CSV
            </Link>
          </div>
          
          <div className="ml-auto">
            <CloseActions
              fecha={fecha}
              canClose={canClose}
              canOverride={canOverride}
              isClosed={formal.isClosed}
              featureEnabled={formal.featureEnabled}
            />
          </div>
        </div>

        {formal.featureEnabled && (
          <div className="inline-flex items-center gap-4 rounded-2xl border border-border/40 bg-foreground/[0.03] px-6 py-3">
            <div className={`flex items-center gap-2 text-[10px] font-black uppercase tracking-widest ${formal.isClosed ? "text-emerald-500" : "text-amber-500"}`}>
              {formal.isClosed ? <ShieldCheck className="h-4 w-4" /> : <ShieldAlert className="h-4 w-4" />}
              ESTADO: {formal.isClosed ? "CERRADO" : "ABIERTO"}
            </div>
            {formal.closedAt && (
              <>
                <div className="h-4 w-px bg-border/40" />
                <span className="text-[10px] font-bold text-muted-foreground uppercase">{formal.closedAt}</span>
              </>
            )}
          </div>
        )}

        <section className="overflow-hidden rounded-[2.5rem] border border-border/50 bg-card shadow-sm">
          <div className="border-b border-border/50 bg-foreground/[0.02] px-8 py-6">
            <h2 className="font-heading text-2xl font-bold text-foreground">
              Detalle de Transacciones
            </h2>
          </div>
          <div className="p-2">
            <TransactionsList rows={snapshot.transacciones} />
          </div>
        </section>
      </div>
    </AppShell>
  );
}
