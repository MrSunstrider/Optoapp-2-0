import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";
import { ACTIVE_OPTICA_COOKIE } from "@/lib/optica-cookie";
import { fetchMembershipsForUser } from "@/lib/memberships";

export async function GET(request: Request) {
  const url = new URL(request.url);
  const opticaId = url.searchParams.get("opticaId")?.trim() ?? "";
  const rol = url.searchParams.get("rol")?.trim() ?? "";
  const nombre = url.searchParams.get("nombre")?.trim() ?? "";

  if (!opticaId || !rol || !nombre) {
    return NextResponse.redirect(new URL("/seleccion-optica", request.url));
  }

  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();

  if (!user) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  const memberships = await fetchMembershipsForUser(supabase, user.id);
  const allowed = memberships.some(
    (m) => m.opticaId === opticaId && m.rol === rol && m.nombre === nombre
  );

  if (!allowed) {
    return NextResponse.redirect(new URL("/seleccion-optica", request.url));
  }

  const response = NextResponse.redirect(new URL("/dashboard", request.url));
  response.cookies.set(
    ACTIVE_OPTICA_COOKIE,
    JSON.stringify({ opticaId, rol, nombre }),
    {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/",
      maxAge: 60 * 60 * 24 * 30
    }
  );
  return response;
}
