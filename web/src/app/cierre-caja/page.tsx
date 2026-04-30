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

  const q = await searchParams;
  const fecha = normalizeFechaCierre(q.fecha);
  const supabase = await createClient();
  const [fiscal, snapshot, formal] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    fetchCierreCaja(supabase, activeOptica.opticaId, fecha),
    fetchCierreFormalStatus(supabase, activeOptica.opticaId, fecha)
  ]);
  const canClose = canCloseCierre(activeOptica.rol);
  const canOverride = canOverrideCierre(activeOptica.rol);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-4xl space-y-4">
          <p className="text-xs text-zinc-400">
            {formatOpticaActivaLine(activeOptica.nombre, fiscal)}
          </p>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h1 className="text-5xl font-semibold">Cierre de Caja</h1>
            <DateFilter value={fecha} />
          </div>
          <p className="text-3xl text-zinc-200">Reporte del {formatFechaLarga(fecha)}</p>

          <PaymentSummaryCards
            efectivo={snapshot.efectivo}
            movilTrans={snapshot.movilTrans}
            tarjeta={snapshot.tarjeta}
          />
          <TotalCard total={snapshot.total} />

          <div className="flex flex-wrap gap-2">
            <Link
              href={`/api/cierre-caja/export?fecha=${fecha}&format=pdf`}
              className="rounded-full bg-[#8AB4F8] px-5 py-2 text-sm font-semibold text-zinc-900"
            >
              Exportar PDF
            </Link>
            <Link
              href={`/api/cierre-caja/export?fecha=${fecha}&format=csv`}
              className="rounded-full border border-zinc-500 px-5 py-2 text-sm text-zinc-100"
            >
              Exportar CSV
            </Link>
            <CloseActions
              fecha={fecha}
              canClose={canClose}
              canOverride={canOverride}
              isClosed={formal.isClosed}
              featureEnabled={formal.featureEnabled}
            />
          </div>
          {formal.featureEnabled ? (
            <p className="text-xs text-zinc-400">
              Estado del cierre:{" "}
              <span className={formal.isClosed ? "text-emerald-300" : "text-amber-300"}>
                {formal.isClosed ? "Cerrado" : "Abierto"}
              </span>
              {formal.closedAt ? ` · ${formal.closedAt}` : ""}
            </p>
          ) : null}

          <section className="space-y-3">
            <h2 className="text-4xl font-semibold">Detalle de Transacciones</h2>
            <TransactionsList rows={snapshot.transacciones} />
          </section>
        </div>
      </div>
    </AppShell>
  );
}
