import type { SupabaseClient } from "@supabase/supabase-js";
import { assertNoDbError } from "@/lib/supabase/db-error";

type PagoRow = {
  monto: number | null;
  fecha: string | null;
};

type DispensacionRow = {
  id: string;
  monto_total: number | null;
  monto_pagado: number | null;
  fecha: string | null;
};

type ServicioRow = {
  id: string;
  monto_total: number | null;
  a_cuenta: number | null;
  fecha: string | null;
};

export type ReportePeriodo = "dia" | "semana" | "mes" | "anio";

export type ReporteFinanciero = {
  periodoLabel: string;
  ingresosCobrados: number;
  ventasEmitidas: number;
  saldoPendiente: number;
  totalMovimientos: number;
  ticketPromedio: number;
  fechaInicio: string;
  fechaFinExclusiva: string;
};

export async function fetchReporteFinanciero(
  supabase: SupabaseClient,
  opticaId: string,
  periodo: ReportePeriodo
): Promise<ReporteFinanciero> {
  const range = resolveRange(periodo);

  const [pagos, dispensaciones, servicios] = await Promise.all([
    fetchPagos(supabase, opticaId, range.start, range.endExclusive),
    fetchDispensaciones(supabase, opticaId, range.start, range.endExclusive),
    fetchServicios(supabase, opticaId, range.start, range.endExclusive)
  ]);

  const ingresosCobrados = pagos.reduce((acc, row) => acc + (row.monto ?? 0), 0);
  const ventasDisp = dispensaciones.reduce((acc, row) => acc + (row.monto_total ?? 0), 0);
  const ventasServ = servicios.reduce((acc, row) => acc + (row.monto_total ?? 0), 0);
  const ventasEmitidas = ventasDisp + ventasServ;

  const saldoDisp = dispensaciones.reduce((acc, row) => {
    const saldo = (row.monto_total ?? 0) - (row.monto_pagado ?? 0);
    return acc + Math.max(0, saldo);
  }, 0);
  const saldoServ = servicios.reduce((acc, row) => {
    const saldo = (row.monto_total ?? 0) - (row.a_cuenta ?? 0);
    return acc + Math.max(0, saldo);
  }, 0);
  const saldoPendiente = saldoDisp + saldoServ;

  const totalMovimientos = dispensaciones.length + servicios.length;
  const ticketPromedio = totalMovimientos > 0 ? ventasEmitidas / totalMovimientos : 0;

  return {
    periodoLabel: labelForPeriodo(periodo),
    ingresosCobrados,
    ventasEmitidas,
    saldoPendiente,
    totalMovimientos,
    ticketPromedio,
    fechaInicio: range.start,
    fechaFinExclusiva: range.endExclusive
  };
}

async function fetchPagos(
  supabase: SupabaseClient,
  opticaId: string,
  from: string,
  toExclusive: string
): Promise<PagoRow[]> {
  const { data, error } = await supabase
    .from("pagos")
    .select("monto,fecha")
    .eq("optica_id", opticaId)
    .gte("fecha", from)
    .lt("fecha", toExclusive)
    .abortSignal(AbortSignal.timeout(12_000));

  assertNoDbError(error, "Reporte: pagos");
  return (data ?? []) as PagoRow[];
}

async function fetchDispensaciones(
  supabase: SupabaseClient,
  opticaId: string,
  from: string,
  toExclusive: string
): Promise<DispensacionRow[]> {
  const { data, error } = await supabase
    .from("dispensaciones")
    .select("id,monto_total,monto_pagado,fecha")
    .eq("optica_id", opticaId)
    .gte("fecha", from)
    .lt("fecha", toExclusive)
    .abortSignal(AbortSignal.timeout(12_000));

  assertNoDbError(error, "Reporte: dispensaciones");
  return (data ?? []) as DispensacionRow[];
}

async function fetchServicios(
  supabase: SupabaseClient,
  opticaId: string,
  from: string,
  toExclusive: string
): Promise<ServicioRow[]> {
  const { data, error } = await supabase
    .from("servicios_extra")
    .select("id,monto_total,a_cuenta,fecha")
    .eq("optica_id", opticaId)
    .gte("fecha", from)
    .lt("fecha", toExclusive)
    .abortSignal(AbortSignal.timeout(12_000));

  assertNoDbError(error, "Reporte: servicios_extra");
  return (data ?? []) as ServicioRow[];
}

function resolveRange(periodo: ReportePeriodo): { start: string; endExclusive: string } {
  const now = new Date();
  const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  switch (periodo) {
    case "dia": {
      const end = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
      return { start: toDateOnly(startOfDay), endExclusive: toDateOnly(end) };
    }
    case "semana": {
      const day = startOfDay.getDay();
      const diffToMonday = day === 0 ? 6 : day - 1;
      const start = new Date(startOfDay);
      start.setDate(start.getDate() - diffToMonday);
      const end = new Date(start);
      end.setDate(end.getDate() + 7);
      return { start: toDateOnly(start), endExclusive: toDateOnly(end) };
    }
    case "mes": {
      const start = new Date(now.getFullYear(), now.getMonth(), 1);
      const end = new Date(now.getFullYear(), now.getMonth() + 1, 1);
      return { start: toDateOnly(start), endExclusive: toDateOnly(end) };
    }
    case "anio": {
      const start = new Date(now.getFullYear(), 0, 1);
      const end = new Date(now.getFullYear() + 1, 0, 1);
      return { start: toDateOnly(start), endExclusive: toDateOnly(end) };
    }
  }
}

function labelForPeriodo(periodo: ReportePeriodo): string {
  switch (periodo) {
    case "dia":
      return "Día";
    case "semana":
      return "Semana";
    case "mes":
      return "Mes";
    case "anio":
      return "Año";
  }
}

function toDateOnly(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
