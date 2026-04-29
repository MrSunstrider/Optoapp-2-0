import type { SupabaseClient } from "@supabase/supabase-js";

/**
 * Eliminaciones de paciente restantes hoy para el usuario en la óptica (máx. 10/día en DB).
 * Requiere migración `paciente_eliminaciones_restantes_hoy`.
 */
export async function fetchEliminacionesRestantesHoy(
  supabase: SupabaseClient,
  opticaId: string
): Promise<number | null> {
  const { data, error } = await supabase.rpc("paciente_eliminaciones_restantes_hoy", {
    p_optica_id: opticaId
  });
  if (error) return null;
  return typeof data === "number" ? data : null;
}
