import type { SupabaseClient } from "@supabase/supabase-js";
import { assertNoDbError } from "@/lib/supabase/db-error";
import {
  PagoRowSchema,
  SaldoPendienteRowSchema,
  CountPendientesRowSchema,
} from "@/lib/financial-queries";
import { dateOnly } from "@/lib/date-utils";
import { z } from "zod";

const EntregaRowSchema = z.object({
  estado_entrega: z.string().nullable(),
  fecha: z.string().nullable(),
});

const CitaRowSchema = z.object({
  proxima_cita: z.string().nullable(),
});

const StockRowSchema = z.object({
  stock_actual: z.number().nullable(),
});

export type DashboardKpis = {
  ventasDia: number;
  ventasMes: number;
  pacientesHoy: number;
  saldosPendientes: number;
  citasHoy: number;
  entregasPendientes: number;
  serviciosPendientes: number;
  stockCritico: number;
  cobrosDia: number;
};

export type DashboardSnapshot = {
  kpis: DashboardKpis;
  status: {
    overall: "ok" | "degraded";
    lastUpdatedIso: string;
    sources: {
      pagosDia: boolean;
      pagosMes: boolean;
      pacientesHoy: boolean;
      dispensaciones: boolean;
      serviciosExtra: boolean;
      citasHoy: boolean;
      entregasPendientes: boolean;
      serviciosPendientes: boolean;
      stockCritico: boolean;
    };
    erroresPorFuente: Partial<
      Record<
        | "pagosDia"
        | "pagosMes"
        | "pacientesHoy"
        | "dispensaciones"
        | "serviciosExtra"
        | "citasHoy"
        | "entregasPendientes"
        | "serviciosPendientes"
        | "stockCritico",
        string
      >
    >;
  };
};

export async function fetchDashboardKpis(
  supabase: SupabaseClient,
  opticaId: string
): Promise<DashboardSnapshot> {
  const { startDay, endDay, startMonth, endMonth } = getDateBounds();

  const erroresPorFuente: DashboardSnapshot["status"]["erroresPorFuente"] = {};

  const ventasDia = await sumPagos(supabase, opticaId, startDay, endDay);
  if (!ventasDia.ok && ventasDia.mensaje)
    erroresPorFuente.pagosDia = ventasDia.mensaje;

  const ventasMes = await sumPagos(supabase, opticaId, startMonth, endMonth);
  if (!ventasMes.ok && ventasMes.mensaje)
    erroresPorFuente.pagosMes = ventasMes.mensaje;

  const pacientesHoy = await countPacientesHoy(
    supabase,
    opticaId,
    startDay,
    endDay
  );
  if (!pacientesHoy.ok && pacientesHoy.mensaje)
    erroresPorFuente.pacientesHoy = pacientesHoy.mensaje;

  const saldoPendiente = await rpcSaldoPendiente(supabase, opticaId);
  if (!saldoPendiente.ok && saldoPendiente.mensaje) {
    erroresPorFuente.dispensaciones = saldoPendiente.mensaje;
    erroresPorFuente.serviciosExtra = saldoPendiente.mensaje;
  }

  const citasHoy = await countCitasHoy(supabase, opticaId, startDay, endDay);
  if (!citasHoy.ok && citasHoy.mensaje) erroresPorFuente.citasHoy = citasHoy.mensaje;

  const entregasPendientes = await countEntregasPendientes(
    supabase,
    opticaId,
    startDay
  );
  if (!entregasPendientes.ok && entregasPendientes.mensaje) {
    erroresPorFuente.entregasPendientes = entregasPendientes.mensaje;
  }

  const pendientesCount = await rpcCountPendientes(supabase, opticaId);
  if (!pendientesCount.ok && pendientesCount.mensaje) {
    erroresPorFuente.serviciosPendientes = pendientesCount.mensaje;
  }

  const stockCritico = await countStockCritico(supabase, opticaId);
  if (!stockCritico.ok && stockCritico.mensaje) {
    erroresPorFuente.stockCritico = stockCritico.mensaje;
  }

  const sources = {
    pagosDia: ventasDia.ok,
    pagosMes: ventasMes.ok,
    pacientesHoy: pacientesHoy.ok,
    dispensaciones: saldoPendiente.ok,
    serviciosExtra: saldoPendiente.ok,
    citasHoy: citasHoy.ok,
    entregasPendientes: entregasPendientes.ok,
    serviciosPendientes: pendientesCount.ok,
    stockCritico: stockCritico.ok
  };

  const overall = Object.values(sources).every(Boolean) ? "ok" : "degraded";

  return {
    kpis: {
      ventasDia: ventasDia.value,
      ventasMes: ventasMes.value,
      pacientesHoy: pacientesHoy.value,
      saldosPendientes: saldoPendiente.value,
      citasHoy: citasHoy.value,
      entregasPendientes: entregasPendientes.value,
      serviciosPendientes: pendientesCount.value,
      stockCritico: stockCritico.value,
      cobrosDia: ventasDia.value
    },
    status: {
      overall,
      lastUpdatedIso: new Date().toISOString(),
      sources,
      erroresPorFuente
    }
  };
}

type MetricOk = { ok: true; value: number };
type MetricFail = { ok: false; value: number; mensaje: string };
type Metric = MetricOk | MetricFail;

function failMetric(mensaje: string): MetricFail {
  return { ok: false, value: 0, mensaje };
}

async function sumPagos(
  supabase: SupabaseClient,
  opticaId: string,
  from: string,
  toExclusive: string
): Promise<Metric> {
  try {
    const { data, error } = await supabase
      .from("pagos")
      .select("monto")
      .eq("optica_id", opticaId)
      .gte("fecha", from)
      .lt("fecha", toExclusive)
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "KPI pagos");
    const rows = z.array(PagoRowSchema).parse(data ?? []);
    const value = rows.reduce(
      (acc, row) => acc + (row.monto ?? 0),
      0
    );
    return { ok: true, value };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return failMetric(msg);
  }
}

async function countPacientesHoy(
  supabase: SupabaseClient,
  opticaId: string,
  from: string,
  toExclusive: string
): Promise<Metric> {
  try {
    const { count, error } = await supabase
      .from("pacientes")
      .select("id", { count: "exact", head: true })
      .eq("optica_id", opticaId)
      .gte("fecha_creacion", from)
      .lt("fecha_creacion", toExclusive)
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "KPI pacientes hoy");
    return { ok: true, value: count ?? 0 };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return failMetric(msg);
  }
}

async function rpcSaldoPendiente(
  supabase: SupabaseClient,
  opticaId: string
): Promise<Metric> {
  try {
    const { data, error } = await supabase
      .rpc("rpc_saldo_pendiente", { p_optica_id: opticaId })
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "KPI saldos pendientes");
    const row = SaldoPendienteRowSchema.parse(data);
    return { ok: true, value: row.saldo_total };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return failMetric(msg);
  }
}

async function countCitasHoy(
  supabase: SupabaseClient,
  opticaId: string,
  from: string,
  toExclusive: string
): Promise<Metric> {
  try {
    const { count, error } = await supabase
      .from("evaluaciones")
      .select("id", { count: "exact", head: true })
      .eq("optica_id", opticaId)
      .gte("proxima_cita", from)
      .lt("proxima_cita", toExclusive)
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "KPI citas hoy");
    return { ok: true, value: count ?? 0 };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return failMetric(msg);
  }
}

async function countEntregasPendientes(
  supabase: SupabaseClient,
  opticaId: string,
  today: string
): Promise<Metric> {
  try {
    const { data, error } = await supabase
      .from("dispensaciones")
      .select("estado_entrega,fecha")
      .eq("optica_id", opticaId)
      .eq("estado_entrega", "Pendiente")
      .lt("fecha", today)
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "KPI entregas pendientes");
    const rows = z.array(EntregaRowSchema).parse(data ?? []);
    return { ok: true, value: rows.length };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return failMetric(msg);
  }
}

async function rpcCountPendientes(
  supabase: SupabaseClient,
  opticaId: string
): Promise<Metric> {
  try {
    const { data, error } = await supabase
      .rpc("rpc_count_pendientes", { p_optica_id: opticaId })
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "KPI servicios pendientes");
    const row = CountPendientesRowSchema.parse(data);
    return { ok: true, value: row.servicios_pendientes };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return failMetric(msg);
  }
}

async function countStockCritico(
  supabase: SupabaseClient,
  opticaId: string
): Promise<Metric> {
  try {
    const { data, error } = await supabase
      .from("monturas")
      .select("stock_actual")
      .eq("optica_id", opticaId)
      .eq("activo", true)
      .lte("stock_actual", 2)
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "KPI stock crítico");
    const rows = z.array(StockRowSchema).parse(data ?? []);
    return { ok: true, value: rows.length };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return failMetric(msg);
  }
}

function getDateBounds() {
  const now = new Date();
  const startDayDate = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate(),
    0,
    0,
    0,
    0
  );
  const endDayDate = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate() + 1,
    0,
    0,
    0,
    0
  );
  const startMonthDate = new Date(now.getFullYear(), now.getMonth(), 1);
  const endMonthDate = new Date(now.getFullYear(), now.getMonth() + 1, 1);

return {
    startDay: dateOnly(startDayDate),
    endDay: dateOnly(endDayDate),
    startMonth: dateOnly(startMonthDate),
    endMonth: dateOnly(endMonthDate)
  };
}

function toDateOnly(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
