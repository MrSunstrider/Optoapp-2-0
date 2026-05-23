import type { SupabaseClient } from "@supabase/supabase-js";
import { assertNoDbError } from "@/lib/supabase/db-error";
import { ResumenFinancieroRowSchema } from "@/lib/financial-queries";
import { dateOnly } from "@/lib/date-utils";

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
  /** Cobros de dispensaciones del período actual */
  ingresosPeriodoActual: number;
  /** Cobros de dispensaciones de períodos anteriores */
  ingresosPeriodosAnteriores: number;
};

/** Status indicating whether the data is real or fallback-empty after a DB error */
export type ReporteFinancieroStatus = {
  overall: "ok" | "degraded";
  error?: string;
};

export type ReporteFinancieroResult = ReporteFinanciero & {
  status: ReporteFinancieroStatus;
};

export async function fetchReporteFinanciero(
  supabase: SupabaseClient,
  opticaId: string,
  periodo: ReportePeriodo
): Promise<ReporteFinancieroResult> {
  const range = resolveRange(periodo);

  try {
    const [rpcResp, pagosResp, dispResp] = await Promise.all([
      supabase
        .rpc("rpc_resumen_financiero", {
          p_optica_id: opticaId,
          p_from: range.start,
          p_to: range.endExclusive,
        })
        .abortSignal(AbortSignal.timeout(12_000)),
      supabase
        .from("pagos")
        .select("id,monto,dispensacion_id")
        .eq("optica_id", opticaId)
        .gte("fecha", range.start)
        .lt("fecha", range.endExclusive)
        .abortSignal(AbortSignal.timeout(10_000)),
      supabase
        .from("dispensaciones")
        .select("id,fecha")
        .eq("optica_id", opticaId)
        .abortSignal(AbortSignal.timeout(10_000)),
    ]);

    assertNoDbError(rpcResp.error, "Reporte financiero RPC");

    const row = ResumenFinancieroRowSchema.parse(rpcResp.data);

    // Calcular desglose: pagos del período que pertenecen a dispensaciones del período
    const dispMap = new Map<string, string>();
    if (dispResp.data) {
      for (const d of dispResp.data as { id: string; fecha: string | null }[]) {
        if (d.fecha) dispMap.set(d.id, d.fecha);
      }
    }

    let ingresosPeriodoActual = 0;
    let ingresosPeriodosAnteriores = 0;
    if (pagosResp.data) {
      for (const p of pagosResp.data as { monto: number | null; dispensacion_id: string | null }[]) {
        const monto = Number(p.monto ?? 0);
        if (!Number.isFinite(monto) || monto <= 0) continue;
        const dispFecha = p.dispensacion_id ? dispMap.get(p.dispensacion_id) : undefined;
        if (dispFecha && dispFecha >= range.start && dispFecha < range.endExclusive) {
          ingresosPeriodoActual += monto;
        } else if (dispFecha) {
          ingresosPeriodosAnteriores += monto;
        } else {
          ingresosPeriodoActual += monto;
        }
      }
    }

    return {
      periodoLabel: labelForPeriodo(periodo),
      ingresosCobrados: row.ingresos_cobrados,
      ventasEmitidas: row.ventas_emitidas,
      saldoPendiente: row.saldo_pendiente,
      totalMovimientos: row.total_movimientos,
      ticketPromedio: row.ticket_promedio,
      fechaInicio: row.fecha_inicio,
      fechaFinExclusiva: row.fecha_fin_exclusiva,
      ingresosPeriodoActual,
      ingresosPeriodosAnteriores,
      status: { overall: "ok" },
    };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return {
      periodoLabel: labelForPeriodo(periodo),
      ingresosCobrados: 0,
      ventasEmitidas: 0,
      saldoPendiente: 0,
      totalMovimientos: 0,
      ticketPromedio: 0,
      fechaInicio: range.start,
      fechaFinExclusiva: range.endExclusive,
      ingresosPeriodoActual: 0,
      ingresosPeriodosAnteriores: 0,
      status: { overall: "degraded", error: msg },
    };
  }
}

export function resolveRange(periodo: ReportePeriodo): { start: string; endExclusive: string } {
  const now = new Date();
  const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  switch (periodo) {
    case "dia": {
      const end = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
      return { start: dateOnly(startOfDay), endExclusive: dateOnly(end) };
    }
    case "semana": {
      const day = startOfDay.getDay();
      const diffToMonday = day === 0 ? 6 : day - 1;
      const start = new Date(startOfDay);
      start.setDate(start.getDate() - diffToMonday);
      const end = new Date(start);
      end.setDate(end.getDate() + 7);
      return { start: dateOnly(start), endExclusive: dateOnly(end) };
    }
    case "mes": {
      const start = new Date(now.getFullYear(), now.getMonth(), 1);
      const end = new Date(now.getFullYear(), now.getMonth() + 1, 1);
      return { start: dateOnly(start), endExclusive: dateOnly(end) };
    }
    case "anio": {
      const start = new Date(now.getFullYear(), 0, 1);
      const end = new Date(now.getFullYear() + 1, 0, 1);
      return { start: dateOnly(start), endExclusive: dateOnly(end) };
    }
  }
}

export function labelForPeriodo(periodo: ReportePeriodo): string {
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

