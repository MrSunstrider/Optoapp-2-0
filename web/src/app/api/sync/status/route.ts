import { NextResponse } from "next/server";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchSyncSnapshot } from "@/lib/sync";
import { createClient } from "@/lib/supabase/server";

export async function GET() {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();
  if (!user) return NextResponse.json({ error: "No autorizado." }, { status: 401 });

  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return NextResponse.json({ error: "Sin óptica activa." }, { status: 400 });

  const snapshot = await fetchSyncSnapshot(supabase, activeOptica.opticaId);
  return NextResponse.json({ ok: true, snapshot });
}
