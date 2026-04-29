import type { SupabaseClient } from "@supabase/supabase-js";
import { assertNoDbError } from "@/lib/supabase/db-error";

export type Membership = {
  opticaId: string;
  rol: string;
  nombre: string;
};

type UsuarioOpticaRow = {
  optica_id: string;
  rol: string;
  opticas: { nombre: string | null } | null;
};

export async function fetchMembershipsForUser(
  supabase: SupabaseClient,
  userId: string
): Promise<Membership[]> {
  const { data, error } = await supabase
    .from("usuario_optica")
    .select("optica_id,rol,opticas(nombre)")
    .eq("user_id", userId);

  assertNoDbError(error, "Membresías de usuario (usuario_optica)");
  if (!data) return [];

  return (data as unknown as UsuarioOpticaRow[]).map((row) => ({
    opticaId: row.optica_id,
    rol: row.rol || "invitado",
    nombre: row.opticas?.nombre?.trim() || row.optica_id
  }));
}
