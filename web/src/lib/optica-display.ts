import type { OpticaFiscalRow } from "@/lib/optica-fiscal";

/** Línea superior tipo producto móvil: óptica + documento fiscal (RUS/RUC, etc.). */
export function formatOpticaActivaLine(
  nombreOptica: string,
  fiscal: OpticaFiscalRow | null
): string {
  const num = fiscal?.fiscal_doc_numero?.trim();
  const tipo = fiscal?.fiscal_doc_tipo?.trim() || "RUS";
  if (num) {
    return `Óptica activa: ${nombreOptica} · ${tipo} ${num}`;
  }
  return `Óptica activa: ${nombreOptica}`;
}
