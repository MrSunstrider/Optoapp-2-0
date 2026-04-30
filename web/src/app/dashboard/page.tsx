import { AppShell } from "@/components/app-shell";
import { DashboardExportActions } from "@/components/dashboard/dashboard-export-actions";
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
  const errs = snapshot.status.erroresPorFuente;
  const today = dateOnly(new Date());
  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);
  const alertas = buildAlertas(kpis.entregasPendientes, kpis.serviciosPendientes);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-5xl space-y-5">
          <header className="space-y-1">
            <h1 className="text-2xl font-semibold tracking-tight text-white">
              Operación de Hoy
            </h1>
            <p className="text-xs text-zinc-400">{opticaLine}</p>
            <p className="text-xs text-zinc-500">
              Fecha operativa: {today}
              {user?.email ? ` · ${user.email}` : ""}
            </p>
          </header>

          <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5 shadow-sm">
            <h2 className="mb-3 text-lg font-semibold text-white">Tablero operativo</h2>
            <div className="space-y-2 text-sm">
              <LineaKpi etiqueta="Fecha" valor={today} />
              <LineaKpi etiqueta="Citas hoy" valor={String(kpis.citasHoy)} />
              <LineaKpi
                etiqueta="Entregas pendientes"
                valor={String(kpis.entregasPendientes)}
              />
              <LineaKpi etiqueta="Cobros del día" valor={money(kpis.cobrosDia)} />
              <LineaKpi etiqueta="Stock crítico" valor={String(kpis.stockCritico)} />
            </div>
          </section>

          <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5 shadow-sm">
            <h2 className="mb-3 text-lg font-semibold text-white">Alertas internas</h2>
            {alertas.length === 0 ? (
              <p className="text-sm text-zinc-300">Sin alertas internas por ahora.</p>
            ) : (
              <ul className="space-y-2 text-sm">
                {alertas.map((alerta, idx) => (
                  <li
                    key={idx}
                    className={
                      alerta.tipo === "critica"
                        ? "text-amber-300"
                        : "text-zinc-200"
                    }
                  >
                    - {alerta.texto}
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5 shadow-sm">
            <h2 className="mb-2 text-lg font-semibold text-white">Exportaciones rápidas</h2>
            <p className="mb-4 text-sm text-zinc-300">
              Descarga reportes operativos del día e inventario, o actualiza el snapshot
              en pantalla.
            </p>
            <DashboardExportActions />
          </section>

          <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5 shadow-sm">
            <h2 className="mb-2 text-lg font-semibold text-white">Integración de módulos</h2>
            <p className="mb-4 text-sm text-zinc-300">
              Acceso rápido y estado operativo de los apartados principales.
            </p>
            <div className="grid grid-cols-1 gap-2 md:grid-cols-2">
              <ModuleLink
                href="/agenda"
                label="Agenda"
                detail={`Citas hoy: ${kpis.citasHoy}`}
                enabled={canAccessModule(activeOptica.rol, "agenda")}
              />
              <ModuleLink
                href="/inventario"
                label="Inventario de Monturas"
                detail={`Stock crítico: ${kpis.stockCritico}`}
                enabled={canAccessModule(activeOptica.rol, "inventario")}
              />
              <ModuleLink
                href="/cierre-caja"
                label="Cierre de Caja"
                detail={`Cobros del día: ${money(kpis.cobrosDia)}`}
                enabled={canAccessModule(activeOptica.rol, "cierre-caja")}
              />
              <ModuleLink
                href="/sincronizar"
                label="Sincronizar Cloud"
                detail={`Estado: ${syncSnapshot.globalStatus}`}
                enabled={canAccessModule(activeOptica.rol, "sincronizar")}
              />
              <ModuleLink
                href="/servicios-varios"
                label="Servicios Varios"
                detail={`Pendientes: ${kpis.serviciosPendientes}`}
                enabled={canAccessModule(activeOptica.rol, "servicios-varios")}
              />
              <ModuleLink
                href="/configuracion"
                label="Configuración"
                detail="Seguridad, fiscal, roles y operación"
                enabled={canAccessModule(activeOptica.rol, "configuracion")}
              />
              <ModuleLink
                href="/estadisticas"
                label="Estadisticas"
                detail="BI operativo y salud de sincronizacion"
                enabled={canAccessModule(activeOptica.rol, "estadisticas")}
              />
            </div>
          </section>

          <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/60 p-4">
            <p className="mb-2 text-xs text-zinc-300">
              Estado del snapshot:{" "}
              <span
                className={
                  snapshot.status.overall === "ok"
                    ? "font-medium text-emerald-300"
                    : "font-medium text-amber-300"
                }
              >
                {snapshot.status.overall === "ok" ? "OK" : "Degradado"}
              </span>{" "}
              · Actualizado: {formatIso(snapshot.status.lastUpdatedIso)}
            </p>
            {snapshot.status.overall === "degraded" && (
              <ul className="space-y-1 text-xs text-zinc-400">
                <FuenteLinea ok={snapshot.status.sources.pagosDia} titulo="Pagos día" detalle={errs.pagosDia} />
                <FuenteLinea ok={snapshot.status.sources.pagosMes} titulo="Pagos mes" detalle={errs.pagosMes} />
                <FuenteLinea ok={snapshot.status.sources.pacientesHoy} titulo="Pacientes hoy" detalle={errs.pacientesHoy} />
                <FuenteLinea ok={snapshot.status.sources.dispensaciones} titulo="Saldo dispensaciones" detalle={errs.dispensaciones} />
                <FuenteLinea ok={snapshot.status.sources.serviciosExtra} titulo="Saldo servicios extra" detalle={errs.serviciosExtra} />
                <FuenteLinea ok={snapshot.status.sources.citasHoy} titulo="Citas hoy" detalle={errs.citasHoy} />
                <FuenteLinea ok={snapshot.status.sources.entregasPendientes} titulo="Entregas pendientes" detalle={errs.entregasPendientes} />
                <FuenteLinea ok={snapshot.status.sources.serviciosPendientes} titulo="Servicios pendientes" detalle={errs.serviciosPendientes} />
                <FuenteLinea ok={snapshot.status.sources.stockCritico} titulo="Stock crítico" detalle={errs.stockCritico} />
              </ul>
            )}
          </section>
        </div>
      </div>
    </AppShell>
  );
}

function ModuleLink({
  href,
  label,
  detail,
  enabled
}: {
  href: string;
  label: string;
  detail: string;
  enabled: boolean;
}) {
  if (!enabled) {
    return (
      <div className="rounded-xl border border-zinc-700 bg-black/15 p-3 opacity-60">
        <p className="font-medium text-zinc-200">{label}</p>
        <p className="text-xs text-zinc-400">{detail}</p>
        <p className="mt-1 text-xs text-amber-300">Sin permiso para este rol.</p>
      </div>
    );
  }
  return (
    <Link href={href} className="rounded-xl border border-zinc-700 bg-black/15 p-3 hover:bg-black/25">
      <p className="font-medium text-zinc-100">{label}</p>
      <p className="text-xs text-zinc-400">{detail}</p>
    </Link>
  );
}

function LineaKpi({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <p className="flex items-center justify-between gap-3 rounded-lg bg-black/15 px-3 py-2 text-zinc-100">
      <span className="text-zinc-300">{etiqueta}</span>
      <span className="font-semibold tabular-nums">{valor}</span>
    </p>
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
    <li className="border-l-2 border-zinc-600 pl-2">
      <span className="font-medium">{titulo}:</span>{" "}
      <span className={ok ? "text-emerald-300" : "text-red-300"}>
        {ok ? "OK" : "Error"}
      </span>
      {!ok && detalle && (
        <span className="mt-0.5 block whitespace-pre-wrap text-[11px] text-red-300/90">
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

function dateOnly(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function buildAlertas(entregasPendientes: number, serviciosPendientes: number) {
  const alertas: Array<{ texto: string; tipo: "critica" | "info" }> = [];
  if (entregasPendientes > 0) {
    alertas.push({
      tipo: entregasPendientes >= 5 ? "critica" : "info",
      texto: `Hay ${entregasPendientes} entregas pendientes con fecha anterior a hoy.`
    });
  }
  if (serviciosPendientes > 0) {
    alertas.push({
      tipo: serviciosPendientes >= 5 ? "critica" : "info",
      texto: `Servicios extra pendientes: ${serviciosPendientes}.`
    });
  }
  return alertas;
}
