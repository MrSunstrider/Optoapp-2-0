import type { SupabaseClient } from "@supabase/supabase-js";
import { z } from "zod";

export const PagoRowSchema = z.object({
  monto: z.number().nullable(),
});
export type PagoRow = z.infer<typeof PagoRowSchema>;

export const DispensacionRowSchema = z.object({
  monto_total: z.number().nullable(),
  monto_pagado: z.number().nullable(),
});
export type DispensacionRow = z.infer<typeof DispensacionRowSchema>;

export const ServicioExtraRowSchema = z.object({
  monto_total: z.number().nullable(),
  a_cuenta: z.number().nullable(),
});
export type ServicioExtraRow = z.infer<typeof ServicioExtraRowSchema>;

export const SaldoPendienteRowSchema = z.object({
  saldo_dispensaciones: z.number(),
  saldo_servicios: z.number(),
  saldo_total: z.number(),
});

export const CountPendientesRowSchema = z.object({
  entregas_pendientes: z.number(),
  servicios_pendientes: z.number(),
});

/** Zod schema for rpc_resumen_financiero response */
export const ResumenFinancieroRowSchema = z.object({
  ingresos_cobrados: z.number(),
  ventas_emitidas: z.number(),
  saldo_pendiente: z.number(),
  total_movimientos: z.number(),
  ticket_promedio: z.number(),
  fecha_inicio: z.string(),
  fecha_fin_exclusiva: z.string(),
});
export type ResumenFinancieroRow = z.infer<typeof ResumenFinancieroRowSchema>;

/** Zod schema for rpc_cierre_caja_resumen response */
export const CierreCajaResumenRowSchema = z.object({
  efectivo: z.number(),
  movil_trans: z.number(),
  tarjeta: z.number(),
  total: z.number(),
});
export type CierreCajaResumenRow = z.infer<typeof CierreCajaResumenRowSchema>;

export type FinancialQueryOpts = {
  dateFrom?: string;
  dateTo?: string;
  signalTimeoutMs?: number;
};

export function queryPagos(
  supabase: SupabaseClient,
  opticaId: string,
  dateFrom: string,
  dateTo: string,
  opts?: Pick<FinancialQueryOpts, "signalTimeoutMs">
) {
  return supabase
    .from("pagos")
    .select("monto")
    .eq("optica_id", opticaId)
    .gte("fecha", dateFrom)
    .lt("fecha", dateTo)
    .abortSignal(AbortSignal.timeout(opts?.signalTimeoutMs ?? 12_000));
}

export function queryDispensaciones(
  supabase: SupabaseClient,
  opticaId: string,
  selectFields: string,
  opts?: FinancialQueryOpts
) {
  let query = supabase
    .from("dispensaciones")
    .select(selectFields)
    .eq("optica_id", opticaId);

  if (opts?.dateFrom) query = query.gte("fecha", opts.dateFrom);
  if (opts?.dateTo) query = query.lt("fecha", opts.dateTo);

  return query.abortSignal(AbortSignal.timeout(opts?.signalTimeoutMs ?? 12_000));
}

export function queryServiciosExtra(
  supabase: SupabaseClient,
  opticaId: string,
  selectFields: string,
  opts?: FinancialQueryOpts
) {
  let query = supabase
    .from("servicios_extra")
    .select(selectFields)
    .eq("optica_id", opticaId);

  if (opts?.dateFrom) query = query.gte("fecha", opts.dateFrom);
  if (opts?.dateTo) query = query.lt("fecha", opts.dateTo);

  return query.abortSignal(AbortSignal.timeout(opts?.signalTimeoutMs ?? 12_000));
}
