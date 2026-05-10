import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";
import { clearActiveOpticaContext } from "@/lib/optica-context";
import { deletePinVerifiedCookie } from "@/lib/pin-cookie";

export async function POST(request: Request) {
  try {
    const supabase = await createClient();
    const { error: signOutErr } = await supabase.auth.signOut();
    if (signOutErr) {
      console.error("[auth/logout] signOut:", signOutErr.message);
    }
  } catch (e) {
    console.error("[auth/logout] Error al cerrar sesión en Supabase:", e);
  }

  try {
    await clearActiveOpticaContext();
  } catch (e) {
    console.error("[auth/logout] Error al borrar cookie de óptica:", e);
  }

  const res = NextResponse.redirect(new URL("/login", request.url));
  deletePinVerifiedCookie(res);
  return res;
}
