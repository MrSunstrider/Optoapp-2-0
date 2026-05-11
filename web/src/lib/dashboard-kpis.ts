import type { SupabaseClient } from "@supabase/supabase-js";
import { assertNoDbError } from "@/lib/supabase/db-error";

type PagoRow = {
  monto: number | null;
};

type DispensacionRow = {
  monto_total: number | null;
  monto_pagado: number | null;
};

type ServicioRow = {
  monto_total: number | null;
  a_cuenta: number | null;
  fecha: string | null;
  estado: string | null;
};

type EntregaRow = {
  estado_entrega: string | null;
  fecha: string | null;
};

type CitaRow = {
  proxima_cita: string | null;
};

type StockRow = {
  stock_actual: number | null;
};

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
    /** Texto exacto devuelto por Supabase cuando una fuente falla. */
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

  const saldoDisp = await sumSaldoDispensaciones(supabase, opticaId);
  if (!saldoDisp.ok && saldoDisp.mensaje)
    erroresPorFuente.dispensaciones = saldoDisp.mensaje;

  const saldoServ = await sumSaldoServicios(supabase, opticaId);
  if (!saldoServ.ok && saldoServ.mensaje)
    erroresPorFuente.serviciosExtra = saldoServ.mensaje;

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

  const serviciosPendientes = await countServiciosPendientes(supabase, opticaId);
  if (!serviciosPendientes.ok && serviciosPendientes.mensaje) {
    erroresPorFuente.serviciosPendientes = serviciosPendientes.mensaje;
  }

  const stockCritico = await countStockCritico(supabase, opticaId);
  if (!stockCritico.ok && stockCritico.mensaje) {
    erroresPorFuente.stockCritico = stockCritico.mensaje;
  }

  const sources = {
    pagosDia: ventasDia.ok,
    pagosMes: ventasMes.ok,
    pacientesHoy: pacientesHoy.ok,
    dispensaciones: saldoDisp.ok,
    serviciosExtra: saldoServ.ok,
    citasHoy: citasHoy.ok,
    entregasPendientes: entregasPendientes.ok,
    serviciosPendientes: serviciosPendientes.ok,
    stockCritico: stockCritico.ok
  };

  const overall = Object.values(sources).every(Boolean) ? "ok" : "degraded";

  return {
    kpis: {
      ventasDia: ventasDia.value,
      ventasMes: ventasMes.value,
      pacientesHoy: pacientesHoy.value,
      saldosPendientes: saldoDisp.value + saldoServ.value,
      citasHoy: citasHoy.value,
      entregasPendientes: entregasPendientes.value,
      serviciosPendientes: serviciosPendientes.value,
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
    const value = ((data ?? []) as PagoRow[]).reduce(
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

async function sumSaldoDispensaciones(
  supabase: SupabaseClient,
  opticaId: string
): Promise<Metric> {
  try {
    const { data, error } = await supabase
      .from("dispensaciones")
      .select("monto_total,monto_pagado")
      .eq("optica_id", opticaId)
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "KPI saldo dispensaciones");
    const value = ((data ?? []) as DispensacionRow[]).reduce((acc, row) => {
      const saldo = (row.monto_total ?? 0) - (row.monto_pagado ?? 0);
      return acc + Math.max(0, saldo);
    }, 0);
    return { ok: true, value };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return failMetric(msg);
  }
}

async function sumSaldoServicios(
  supabase: SupabaseClient,
  opticaId: string
): Promise<Metric> {
  try {
    const { data, error } = await supabase
      .from("servicios_extra")
      .select("monto_total,a_cuenta")
      .eq("optica_id", opticaId)
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "KPI saldo servicios extra");
    const value = ((data ?? []) as ServicioRow[]).reduce((acc, row) => {
      const saldo = (row.monto_total ?? 0) - (row.a_cuenta ?? 0);
      return acc + Math.max(0, saldo);
    }, 0);
    return { ok: true, value };
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
    return { ok: true, value: ((data ?? []) as EntregaRow[]).length };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return failMetric(msg);
  }
}

async function countServiciosPendientes(
  supabase: SupabaseClient,
  opticaId: string
): Promise<Metric> {
  try {
    const { data, error } = await supabase
      .from("servicios_extra")
      .select("estado,monto_total,a_cuenta")
      .eq("optica_id", opticaId)
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "KPI servicios pendientes");
    const rows = (data ?? []) as ServicioRow[];
    const value = rows.reduce((acc, row) => {
      const saldo = (row.monto_total ?? 0) - (row.a_cuenta ?? 0);
      if ((row.estado ?? "").toLowerCase() === "pendiente" || saldo > 0.005) {
        return acc + 1;
      }
      return acc;
    }, 0);
    return { ok: true, value };
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
    return { ok: true, value: ((data ?? []) as StockRow[]).length };
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
    startDay: toDateOnly(startDayDate),
    endDay: toDateOnly(endDayDate),
    startMonth: toDateOnly(startMonthDate),
    endMonth: toDateOnly(endMonthDate)
  };
}

function toDateOnly(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
