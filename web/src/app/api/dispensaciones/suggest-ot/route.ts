import { NextResponse } from "next/server";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { createClient } from "@/lib/supabase/server";

export async function GET(request: Request) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return NextResponse.json({ error: "Sin óptica activa" }, { status: 400 });
  const url = new URL(request.url);
  const fecha = url.searchParams.get("fecha")?.trim() || "";
  const year = /^\d{4}-\d{2}-\d{2}$/.test(fecha)
    ? fecha.slice(0, 4)
    : String(new Date().getFullYear());

  const supabase = await createClient();
  const { data, error } = await supabase
    .from("dispensaciones")
    .select("ot")
    .eq("optica_id", activeOptica.opticaId)
    .ilike("ot", `OT-${year}-%`);
  if (error) return NextResponse.json({ error: "No se pudo sugerir OT" }, { status: 500 });

  const re = new RegExp(`^OT-${year}-(\\d+)$`, "i");
  let max = 0;
  for (const row of data ?? []) {
    const ot = String(row.ot ?? "").trim();
    const m = ot.match(re);
    if (!m) continue;
    const v = Number(m[1]);
    if (Number.isFinite(v) && v > max) max = v;
  }
  const ot = `OT-${year}-${String(max + 1).padStart(4, "0")}`;
  return NextResponse.json({ ot });
}
