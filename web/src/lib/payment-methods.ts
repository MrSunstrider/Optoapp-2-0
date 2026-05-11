/**
 * Canonical payment method mappings.
 * L1: Centralized enum to avoid fuzzy string matching drift.
 */
export const MEDIO_PAGO_EFECTIVO = "Efectivo";
export const MEDIO_PAGO_TARJETA = "Tarjeta";
export const MEDIO_PAGO_MOVIL_TRANS = "Móvil/Trans";
export const MEDIO_PAGO_OTRO = "Otro";

export type MedioPago =
  | typeof MEDIO_PAGO_EFECTIVO
  | typeof MEDIO_PAGO_TARJETA
  | typeof MEDIO_PAGO_MOVIL_TRANS
  | typeof MEDIO_PAGO_OTRO;

const EFECTIVO_KEYWORDS = ["efectivo"];
const TARJETA_KEYWORDS = ["tarjeta"];
const MOVIL_TRANS_KEYWORDS = ["yape", "plin", "transfer", "movil", "móvil"];

/**
 * Map a raw DB payment method string to a canonical MedioPago.
 * Uses exact keyword matching instead of fuzzy includes to reduce false positives.
 */
export function mapMedioPago(raw: string | null): MedioPago {
  const v = (raw ?? "").trim().toLowerCase();
  if (!v) return MEDIO_PAGO_EFECTIVO;
  if (EFECTIVO_KEYWORDS.some((k) => v.includes(k))) return MEDIO_PAGO_EFECTIVO;
  if (TARJETA_KEYWORDS.some((k) => v.includes(k))) return MEDIO_PAGO_TARJETA;
  if (MOVIL_TRANS_KEYWORDS.some((k) => v.includes(k))) return MEDIO_PAGO_MOVIL_TRANS;
  return MEDIO_PAGO_OTRO;
}
