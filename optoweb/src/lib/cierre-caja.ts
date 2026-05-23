import type { SupabaseClient } from "@supabase/supabase-js";
import { z } from "zod";
import { assertNoDbError } from "@/lib/supabase/db-error";
import { CierreCajaResumenRowSchema } from "@/lib/financial-queries";
import { dateOnly } from "@/lib/date-utils";

const PagoRowSchema = z.object({
  id: z.string(),
  fecha: z.string().nullable(),
  monto: z.number().nullable(),
  metodo_pago: z.string().nullable(),
  dispensacion_id: z.string().nullable().optional(),
  servicio_extra_id: z.string().nullable().optional(),
  nota: z.string().nullable().optional(),
});

export type CierrePeriodo = {
  fecha: string;
  from: string;
  toExclusive: string;
};

export type CierreTx = {
  id: string;
  fechaHora: string;
  tipoOperacion: "Dispensación" | "Servicio Extra" | "Pago";
  medioPago: "Efectivo" | "Móvil/Trans" | "Tarjeta" | "Otro";
  monto: number;
  referencia: string;
};

export type CierreSnapshotStatus = {
  overall: "ok" | "degraded";
  error?: string;
};

export type CierreSnapshot = {
  fecha: string;
  efectivo: number;
  movilTrans: number;
  tarjeta: number;
  total: number;
  ventasHoy: number;
  cobrosAtrasados: number;
  transacciones: CierreTx[];
  /** M2: status indicates whether the data is real or fallback-empty after a DB error */
  status: CierreSnapshotStatus;
};

export type CierreFormalStatus = {
  featureEnabled: boolean;
  exists: boolean;
  isClosed: boolean;
  closedAt: string | null;
  closedBy: string | null;
  closeId: string | null;
};

export function normalizeFechaCierre(value?: string, timeZone?: string | null): string {
  const raw = String(value ?? "").trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw;
  return dateOnly(new Date(), timeZone);
}

export function resolveCierrePeriodo(fecha: string): CierrePeriodo {
  const [y, m, d] = fecha.split("-").map((v) => Number(v));
  const start = new Date(y, (m ?? 1) - 1, d ?? 1);
  const end = new Date(start);
  end.setDate(end.getDate() + 1);
  return { fecha, from: dateOnly(start), toExclusive: dateOnly(end) };
}

export async function fetchCierreCaja(
  supabase: SupabaseClient,
  opticaId: string,
  fecha: string
): Promise<CierreSnapshot> {
  const period = resolveCierrePeriodo(fecha);

  const [pagosResp, dispResp] = await Promise.all([
    supabase
      .from("pagos")
      .select("id,fecha,monto,metodo_pago,dispensacion_id,servicio_extra_id,nota")
      .eq("optica_id", opticaId)
      .gte("fecha", period.from)
      .lt("fecha", period.toExclusive)
      .abortSignal(AbortSignal.timeout(12_000)),
    // Cargar dispensaciones para clasificar pagos
    supabase
      .from("dispensaciones")
      .select("id,fecha")
      .eq("optica_id", opticaId)
      .abortSignal(AbortSignal.timeout(12_000)),
  ]);

  if (pagosResp.error) {
    console.warn("[Cierre de caja] Error al consultar pagos:", pagosResp.error.message, pagosResp.error.code);
    return {
      fecha: period.fecha, efectivo: 0, movilTrans: 0, tarjeta: 0, total: 0,
      ventasHoy: 0, cobrosAtrasados: 0,
      transacciones: [],
      status: { overall: "degraded", error: pagosResp.error.message },
    };
  }

  const rows = z.array(PagoRowSchema).parse(pagosResp.data ?? []);
  const dispMap = new Map<string, string>();
  if (dispResp.data) {
    for (const d of dispResp.data as { id: string; fecha: string | null }[]) {
      if (d.fecha) dispMap.set(d.id, d.fecha);
    }
  }

  let efectivoCents = 0;
  let movilTransCents = 0;
  let tarjetaCents = 0;
  let ventasHoyCents = 0;
  let cobrosAtrasadosCents = 0;

  const transacciones = rows
    .map((row) => {
      const monto = normalizeMoney(Number(row.monto ?? 0));
      const cents = toCents(monto);
      const medio = mapMedioPago(row.metodo_pago);
      if (medio === "Efectivo") efectivoCents += cents;
      else if (medio === "Móvil/Trans") movilTransCents += cents;
      else if (medio === "Tarjeta") tarjetaCents += cents;

      // Clasificar: ¿el pago pertenece a una dispensación de hoy?
      const dispFecha = row.dispensacion_id ? dispMap.get(row.dispensacion_id) ?? null : null;
      if (dispFecha === period.from) ventasHoyCents += cents;
      else if (dispFecha !== null && dispFecha < period.from) cobrosAtrasadosCents += cents;
      else ventasHoyCents += cents; // pagos sin dispensación o con misma fecha

      const tipoOperacion = row.servicio_extra_id
        ? "Servicio Extra"
        : row.dispensacion_id
          ? "Dispensación"
          : "Pago";
      const referencia = row.nota?.trim()
        ? row.nota.trim()
        : row.servicio_extra_id
          ? `Servicio ${row.servicio_extra_id.slice(0, 8)}`
          : row.dispensacion_id
            ? `Dispensación ${row.dispensacion_id.slice(0, 8)}`
            : row.id.slice(0, 8);

      return {
        id: row.id,
        fechaHora: row.fecha ?? period.fecha,
        tipoOperacion,
        medioPago: medio,
        monto,
        referencia
      } satisfies CierreTx;
    })
    .sort((a, b) => b.fechaHora.localeCompare(a.fechaHora));

  const efectivo = fromCents(efectivoCents);
  const movilTrans = fromCents(movilTransCents);
  const tarjeta = fromCents(tarjetaCents);
  const total = fromCents(efectivoCents + movilTransCents + tarjetaCents);
  const ventasHoy = fromCents(ventasHoyCents);
  const cobrosAtrasados = fromCents(cobrosAtrasadosCents);
  return {
    fecha: period.fecha, efectivo, movilTrans, tarjeta, total,
    ventasHoy, cobrosAtrasados,
    transacciones,
    status: { overall: "ok" },
  };
}

export function money(v: number): string {
  return `S/ ${v.toFixed(2)}`;
}

export function formatFechaLarga(fecha: string): string {
  const d = new Date(`${fecha}T12:00:00`);
  if (Number.isNaN(d.getTime())) return fecha;
  return d.toLocaleDateString("es-PE", { day: "2-digit", month: "short", year: "numeric" });
}

export function canReadCierre(role: string): boolean {
  const r = role.trim().toLowerCase();
  return r !== "invitado" && r !== "lectura";
}

export function canCloseCierre(role: string): boolean {
  const r = role.trim().toLowerCase();
  return r === "admin" || r === "gerente" || r === "cajero";
}

export function canOverrideCierre(role: string): boolean {
  const r = role.trim().toLowerCase();
  return r === "admin" || r === "gerente";
}

export async function fetchCierreFormalStatus(
  supabase: SupabaseClient,
  opticaId: string,
  fecha: string
): Promise<CierreFormalStatus> {
  const { data, error } = await supabase
    .from("cierres_caja")
    .select("id,estado,cerrado_at,cerrado_por")
    .eq("optica_id", opticaId)
    .eq("fecha", fecha)
    .limit(1)
    .maybeSingle();

  if (error) {
    console.warn("[Cierre de caja] Error al consultar cierres_caja:", error.message, error.code);
    return {
      featureEnabled: false,
      exists: false,
      isClosed: false,
      closedAt: null,
      closedBy: null,
      closeId: null
    };
  }

  if (!data) {
    return {
      featureEnabled: true,
      exists: false,
      isClosed: false,
      closedAt: null,
      closedBy: null,
      closeId: null
    };
  }

  const CierreCajaRowSchema = z.object({
    id: z.string(),
    estado: z.string().nullable().optional(),
    cerrado_at: z.string().nullable().optional(),
    cerrado_por: z.string().nullable().optional(),
  });
  const row = CierreCajaRowSchema.parse(data);
  return {
    featureEnabled: true,
    exists: true,
    isClosed: (row.estado ?? "cerrado") !== "abierto",
    closedAt: row.cerrado_at ?? null,
    closedBy: row.cerrado_por ?? null,
    closeId: row.id ?? null
  };
}

import { mapMedioPago } from "@/lib/payment-methods";
export { mapMedioPago };

export function normalizeMoney(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.round(value * 100) / 100;
}

export function toCents(value: number): number {
  return Math.round(normalizeMoney(value) * 100);
}

export function fromCents(cents: number): number {
  return Math.round(cents) / 100;
}