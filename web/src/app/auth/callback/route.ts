import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";

/**
 * Intercambia el código OAuth por sesión (Google u otros proveedores en Supabase).
 */
export async function GET(request: Request) {
  const { searchParams, origin } = new URL(request.url);
  const code = searchParams.get("code");
  const nextParam = searchParams.get("next") ?? "/dashboard";

  const allowedPaths = new Set([
    "/dashboard",
    "/pacientes",
    "/agenda",
    "/inventario",
    "/servicios-varios",
    "/configuracion",
    "/cierre-caja",
    "/estadisticas",
    "/reportes",
    "/sincronizar",
    "/pin",
    "/seleccion-optica",
  ]);

  const next = nextParam.startsWith("/") && allowedPaths.has(nextParam.split("?")[0].split("#")[0])
    ? nextParam
    : "/dashboard";

  if (code) {
    const supabase = await createClient();
    const { error } = await supabase.auth.exchangeCodeForSession(code);
    if (!error) {
      return NextResponse.redirect(`${origin}${next}`);
    }
  }

  const login = new URL("/login", origin);
  login.searchParams.set("error", "oauth");
  return NextResponse.redirect(login);
}
