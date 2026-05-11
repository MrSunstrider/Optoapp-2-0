/**
 * Misma lógica que reportes-financieros / KPIs: saldo = total − a cuenta.
 */
export function computeSaldoServicioExtra(
  montoTotal: number | null,
  aCuenta: number | null
): number {
  const t = Number(montoTotal ?? 0);
  const a = Number(aCuenta ?? 0);
  return t - a;
}

export function roundMoney2(n: number): number {
  return Math.round(n * 100) / 100;
}

/** Saldo “pagado” si, redondeado a céntimos, no queda deuda. */
export function isSaldoPagado(saldo: number): boolean {
  return roundMoney2(saldo) <= 0;
}

export function formatSoles(n: number | null): string {
  if (n === null || Number.isNaN(n)) return "—";
  return `S/ ${n.toFixed(2)}`;
}

export function formatFechaServicioListado(fecha: string | null): string {
  if (!fecha?.trim()) return "—";
  try {
    return new Date(fecha + "T12:00:00").toLocaleDateString("es-PE", {
      day: "numeric",
      month: "short",
      year: "numeric"
    });
  } catch {
    return fecha;
  }
}

export type ServicioVariosListRow = {
  id: string;
  fecha: string | null;
  ot: string | null;
  descripcion: string | null;
  monto_total: number | null;
  a_cuenta: number | null;
  estado: string | null;
  paciente_id: string | null;
};
