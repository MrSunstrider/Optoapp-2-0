import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";

export async function POST(request: Request) {
  const supabase = await createClient();

  const { data: { user }, error: authError } = await supabase.auth.getUser();
  if (authError || !user) {
    return NextResponse.json({ error: "No autenticado" }, { status: 401 });
  }

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
    return NextResponse.json({ error: "Solo admin puede restaurar respaldos" }, { status: 403 });
  }

  let body: Record<string, unknown>;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "JSON inválido" }, { status: 400 });
  }

  // Validate structure
  if (!body.source_optica_id || body.source_optica_id !== optica_id) {
    return NextResponse.json({
      error: body.source_optica_id
        ? "Este respaldo pertenece a otra óptica y no puede restaurarse aquí."
        : "Respaldo sin origen de óptica."
    }, { status: 400 });
  }

  if (typeof body.version !== "number" || body.version < 3) {
    return NextResponse.json({ error: "Versión de respaldo no soportada. Exporta uno nuevo." }, { status: 400 });
  }

  const tables = [
    "pacientes", "evaluaciones", "dispensaciones",
    "servicios_extra", "pagos", "monturas", "montura_movimientos"
  ] as const;

  const errors: string[] = [];

  for (const table of tables) {
    const rows = body[table];
    if (!Array.isArray(rows) || rows.length === 0) continue;

    // Upsert each row: insert or update by primary key collision
    for (const row of rows) {
      const { error } = await supabase
        .from(table)
        .upsert({ ...(row as Record<string, unknown>), optica_id })
        .eq("optica_id", optica_id);

      if (error) errors.push(`${table}: ${error.message}`);
    }
  }

  if (errors.length > 0) {
    return NextResponse.json({
      message: "Restaurado con errores parciales.",
      errors: errors.slice(0, 10)
    });
  }

  return NextResponse.json({ message: "Base de datos restaurada correctamente." });
}
