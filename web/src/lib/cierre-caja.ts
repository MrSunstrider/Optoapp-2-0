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
  const { data, error } = await supabase
    .from("pagos")
    .select("id,fecha,monto,metodo_pago,dispensacion_id,servicio_extra_id,nota")
    .eq("optica_id", opticaId)
    .gte("fecha", period.from)
    .lt("fecha", period.toExclusive)
    .abortSignal(AbortSignal.timeout(12_000));

  if (error) {
    // M2: signal to the UI that data is incomplete rather than silently returning $0.00
    console.warn("[Cierre de caja] Error al consultar pagos:", error.message, error.code);
    return {
      fecha: period.fecha,
      efectivo: 0,
      movilTrans: 0,
      tarjeta: 0,
      total: 0,
      transacciones: [],
      status: { overall: "degraded", error: error.message },
    };
  }

  const rows = z.array(PagoRowSchema).parse(data ?? []);

  let efectivoCents = 0;
  let movilTransCents = 0;
  let tarjetaCents = 0;
  const transacciones = rows
    .map((row) => {
      const monto = normalizeMoney(Number(row.monto ?? 0));
      const cents = toCents(monto);
      const medio = mapMedioPago(row.metodo_pago);
      if (medio === "Efectivo") efectivoCents += cents;
      else if (medio === "Móvil/Trans") movilTransCents += cents;
      else if (medio === "Tarjeta") tarjetaCents += cents;

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
  return {
    fecha: period.fecha,
    efectivo,
    movilTrans,
    tarjeta,
    total,
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

export function mapMedioPago(raw: string | null): CierreTx["medioPago"] {
  const v = (raw ?? "").trim().toLowerCase();
  if (!v || v.includes("efectivo")) return "Efectivo";
  if (v.includes("tarjeta")) return "Tarjeta";
  if (
    v.includes("yape") ||
    v.includes("plin") ||
    v.includes("transfer") ||
    v.includes("movil") ||
    v.includes("móvil")
  ) {
    return "Móvil/Trans";
  }
  return "Otro";
}

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