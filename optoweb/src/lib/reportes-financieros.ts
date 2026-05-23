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
    const { data, error } = await supabase
      .rpc("rpc_resumen_financiero", {
        p_optica_id: opticaId,
        p_from: range.start,
        p_to: range.endExclusive,
      })
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(error, "Reporte financiero RPC");

    const row = ResumenFinancieroRowSchema.parse(data);

    return {
      periodoLabel: labelForPeriodo(periodo),
      ingresosCobrados: row.ingresos_cobrados,
      ventasEmitidas: row.ventas_emitidas,
      saldoPendiente: row.saldo_pendiente,
      totalMovimientos: row.total_movimientos,
      ticketPromedio: row.ticket_promedio,
      fechaInicio: row.fecha_inicio,
      fechaFinExclusiva: row.fecha_fin_exclusiva,
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

