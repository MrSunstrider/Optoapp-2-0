import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";

export async function GET() {
  const supabase = await createClient();

  const { data: { user }, error: authError } = await supabase.auth.getUser();
  if (authError || !user) {
    return NextResponse.json({ error: "No autenticado" }, { status: 401 });
  }

  // Get user's optica memberships to find opticaId and role
  const { data: memberships } = await supabase
    .from("usuario_optica")
    .select("optica_id, rol")
    .eq("user_id", user.id)
    .single();

  if (!memberships) {
    return NextResponse.json({ error: "Sin membresía de óptica" }, { status: 403 });
  }

  const { optica_id, rol } = memberships;
  if (rol !== "admin") {
    return NextResponse.json({ error: "Solo admin puede descargar respaldo total" }, { status: 403 });
  }

  // Fetch all entity types for this optica
  const tables = [
    "pacientes", "evaluaciones", "dispensaciones",
    "servicios_extra", "pagos", "monturas", "montura_movimientos"
  ] as const;

  const backup: Record<string, unknown> = {
    version: 3,
    dateExported: Date.now(),
    appIdentifier: "OptoApp-Web",
    source_optica_id: optica_id
  };

  for (const table of tables) {
    const { data } = await supabase
      .from(table)
      .select("*")
      .eq("optica_id", optica_id);
    backup[table] = data ?? [];
  }

  return NextResponse.json(backup);
}
