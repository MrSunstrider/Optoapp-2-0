import type { SupabaseClient } from "@supabase/supabase-js";
import { assertNoDbError } from "@/lib/supabase/db-error";

export type OpticaPacienteLimitInfo = {
  maxPacientes: number | null;
  pacientesActuales: number;
  puedeCrearMas: boolean;
};

/** NULL max_pacientes_por_optica = ilimitado (Supabase opticas). */
export async function getOpticaPacienteLimitInfo(
  supabase: SupabaseClient,
  opticaId: string
): Promise<OpticaPacienteLimitInfo> {
  const { data: o, error: e1 } = await supabase
    .from("opticas")
    .select("max_pacientes_por_optica")
    .eq("id", opticaId)
    .maybeSingle();
  assertNoDbError(e1, "Límite de plan (óptica)");

  const { count, error: e2 } = await supabase
    .from("pacientes")
    .select("id", { count: "exact", head: true })
    .eq("optica_id", opticaId)
    .abortSignal(AbortSignal.timeout(12_000));
  assertNoDbError(e2, "Conteo pacientes");

  const max =
    o?.max_pacientes_por_optica != null && !Number.isNaN(o.max_pacientes_por_optica)
      ? Number(o.max_pacientes_por_optica)
      : null;
  const n = count ?? 0;
  const puedeCrearMas = max === null || n < max;
  return { maxPacientes: max, pacientesActuales: n, puedeCrearMas };
}
