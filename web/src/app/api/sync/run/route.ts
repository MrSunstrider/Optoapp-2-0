import { NextResponse } from "next/server";

import { canCloseCierre } from "@/lib/cierre-caja";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { runManualSync } from "@/lib/sync";
import { createClient } from "@/lib/supabase/server";

export async function POST() {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();
  if (!user) return NextResponse.json({ error: "No autorizado." }, { status: 401 });

  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return NextResponse.json({ error: "Sin óptica activa." }, { status: 400 });
  if (!canCloseCierre(activeOptica.rol)) {
    return NextResponse.json(
      { error: "Tu rol no tiene permiso para sincronizar manualmente." },
      { status: 403 }
    );
  }

  const result = await runManualSync(supabase, activeOptica.opticaId, user.id, "manual-run");
  if (!result.ok) return NextResponse.json({ error: result.message }, { status: 500 });
  return NextResponse.json({ ok: true, message: result.message });
}
