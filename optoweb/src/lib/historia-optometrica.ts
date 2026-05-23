import type { SupabaseClient } from "@supabase/supabase-js";

/**
 * Suggest the next available HO number via RPC (server-side, no full table scan).
 * M9: Replaces client-side fetchHistoriaRowsForOptica + suggestNextHistoriaOptometrica.
 */
export async function suggestNextHoRPC(
  supabase: SupabaseClient,
  opticaId: string
): Promise<{ ok: true; value: string } | { ok: false; error: string }> {
  const { data, error } = await supabase
    .rpc("suggest_next_ho", { p_optica_id: opticaId });

  if (error) return { ok: false, error: error.message };
  const result = data as { next_ho?: string } | null;
  if (!result?.next_ho) return { ok: false, error: "No se pudo generar HO" };
  return { ok: true, value: result.next_ho };
}

/**
 * Check if a given HO number is already used by another patient.
 * Uses a targeted DB query instead of fetching all rows (M9).
 */
export async function checkDuplicateHo(
  supabase: SupabaseClient,
  opticaId: string,
  ho: string,
  excludePacienteId?: string | null
): Promise<string | null> {
  const key = normalizedHistoriaKey(ho);
  if (!key) return null;

  let q = supabase
    .from("pacientes")
    .select("id")
    .eq("optica_id", opticaId)
    .ilike("historia_optometrica", key);

  if (excludePacienteId) {
    q = q.neq("id", excludePacienteId);
  }

  const { data, error } = await q.limit(1).maybeSingle();
  if (error || !data) return null;
  return data.id;
}

export function normalizedHistoriaKey(
  h: string | null | undefined
): string | null {
  const n = h?.trim().toUpperCase() ?? "";
  return n.length > 0 ? n : null;
}
