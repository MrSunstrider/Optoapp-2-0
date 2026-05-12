import type { SupabaseClient } from "@supabase/supabase-js";

export type InvitacionResult =
  | { ok: true; codigo: string }
  | { ok: false; error: string };

/** Generar un código de invitación para una óptica */
export async function generarInvitacion(
  supabase: SupabaseClient,
  opticaId: string,
  rol: string,
  creadoPor: string,
): Promise<InvitacionResult> {
  const codigo = generarCodigo();
  const { error } = await supabase.from("invitaciones").insert({
    optica_id: opticaId,
    codigo,
    rol,
    creado_por: creadoPor,
    expira_en: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
  });
  if (error) return { ok: false, error: error.message };
  return { ok: true, codigo };
}

/** Canjear un código de invitación: vincula al usuario a la óptica */
export async function canjearInvitacion(
  supabase: SupabaseClient,
  codigo: string,
  userId: string,
): Promise<
  { ok: true; opticaId: string; rol: string } | { ok: false; error: string }
> {
  const { data, error } = await supabase
    .from("invitaciones")
    .select("id,optica_id,rol,expira_en,usado_por")
    .eq("codigo", codigo.toUpperCase().trim())
    .maybeSingle();

  if (error || !data) return { ok: false, error: "Código inválido." };
  if (data.usado_por) return { ok: false, error: "Este código ya fue usado." };
  if (new Date(data.expira_en) < new Date())
    return { ok: false, error: "El código ha expirado." };
  if (!data.optica_id)
    return { ok: false, error: "Código sin óptica asignada." };

  const { error: upsertError } = await supabase
    .from("usuario_optica")
    .upsert(
      {
        user_id: userId,
        optica_id: data.optica_id,
        rol: data.rol,
      },
      { onConflict: "user_id,optica_id" },
    );
  if (upsertError) return { ok: false, error: "No se pudo vincular." };

  await supabase
    .from("invitaciones")
    .update({
      usado_por: userId,
      usado_en: new Date().toISOString(),
    })
    .eq("id", data.id);

  return { ok: true, opticaId: data.optica_id, rol: data.rol };
}

/** Crear una nueva óptica + asignar al creador como admin */
export async function crearOptica(
  supabase: SupabaseClient,
  userId: string,
  nombre: string,
): Promise<{ ok: true; opticaId: string } | { ok: false; error: string }> {
  const opticaId = crypto.randomUUID();
  const { error: opticaErr } = await supabase.from("opticas").insert({
    id: opticaId,
    nombre: nombre.trim(),
    plan_code: "free",
    max_pacientes_por_optica: null,
    max_usuarios_por_optica: null
  });
  if (opticaErr) return { ok: false, error: "No se pudo crear la óptica." };

  const { error: memberErr } = await supabase.from("usuario_optica").insert({
    user_id: userId,
    optica_id: opticaId,
    rol: "admin",
  });
  if (memberErr) {
    await supabase.from("opticas").delete().eq("id", opticaId);
    return { ok: false, error: "No se pudo asignar como admin." };
  }

  return { ok: true, opticaId };
}

function generarCodigo(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "OPT-";
  for (let i = 0; i < 6; i++)
    code += chars[Math.floor(Math.random() * chars.length)];
  return code;
}
