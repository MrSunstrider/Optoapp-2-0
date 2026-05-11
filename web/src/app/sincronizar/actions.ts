"use server";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { createClient } from "@/lib/supabase/server";
import { runManualSync } from "@/lib/sync";

export async function runSyncAction(): Promise<{ ok: boolean; message: string }> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { ok: false, message: "No hay óptica activa." };

  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();
  if (!user) return { ok: false, message: "No hay sesión activa." };

  return runManualSync(supabase, activeOptica.opticaId, user.id);
}
