export function suggestNextHistoriaOptometrica(
  historias: (string | null | undefined)[]
): string {
  const year = new Date().getFullYear().toString();
  const regex = new RegExp(`^HO-${year}-(\\d+)$`, "i");
  let max = 0;
  for (const h of historias) {
    if (!h) continue;
    const m = h.trim().match(regex);
    if (m) {
      const n = parseInt(m[1], 10);
      if (!Number.isNaN(n)) max = Math.max(max, n);
    }
  }
  return `HO-${year}-${String(max + 1).padStart(4, "0")}`;
}

export function normalizedHistoriaKey(
  h: string | null | undefined
): string | null {
  const n = h?.trim().toUpperCase() ?? "";
  return n.length > 0 ? n : null;
}

export function findDuplicateHistoriaPacienteId(
  rows: { id: string; historia_optometrica: string | null }[],
  ho: string,
  excludePacienteId?: string | null
): string | null {
  const key = normalizedHistoriaKey(ho);
  if (!key) return null;
  for (const r of rows) {
    if (excludePacienteId && r.id === excludePacienteId) continue;
    if (normalizedHistoriaKey(r.historia_optometrica) === key) return r.id;
  }
  return null;
}
