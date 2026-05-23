import { NextResponse } from "next/server";
import type { SupabaseClient } from "@supabase/supabase-js";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { createClient } from "@/lib/supabase/server";

type RouteHandler = (
  request: Request,
  context?: { params: Promise<Record<string, string>> }
) => Promise<NextResponse>;

/**
 * Wraps an API route handler with error handling.
 * Catches unexpected errors and returns a 500 JSON response.
 */
export function withErrorHandler(handler: RouteHandler): RouteHandler {
  return async (request, context) => {
    try {
      return await handler(request, context);
    } catch (e) {
      console.error("[api] Error no controlado:", e);
      return NextResponse.json(
        { error: "Error interno del servidor." },
        { status: 500 }
      );
    }
  };
}

export type UserSession = {
  supabase: SupabaseClient;
  user: NonNullable<Awaited<ReturnType<SupabaseClient["auth"]["getUser"]>>["data"]["user"]>;
};

export type FullSession = UserSession & {
  optica: NonNullable<Awaited<ReturnType<typeof getActiveOpticaContext>>>;
};

/**
 * Require a logged-in user.
 * Returns the session on success, or a JSON error NextResponse on failure.
 */
export async function requireUser(): Promise<UserSession | NextResponse> {
  try {
    const supabase = await createClient();
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) return NextResponse.json({ error: "No autorizado." }, { status: 401 });
    return { supabase, user };
  } catch (e) {
    console.error("[api-auth] Error al autenticar:", e);
    return NextResponse.json({ error: "Error de autenticación." }, { status: 500 });
  }
}

/**
 * Require a logged-in user AND an active óptica context.
 */
export async function requireUserAndOptica(): Promise<FullSession | NextResponse> {
  const session = await requireUser();
  if (session instanceof NextResponse) return session;

  try {
    const optica = await getActiveOpticaContext();
    if (!optica) return NextResponse.json({ error: "Sin óptica activa." }, { status: 400 });
    return { ...session, optica };
  } catch (e) {
    console.error("[api-auth] Error al obtener contexto de óptica:", e);
    return NextResponse.json({ error: "Error interno." }, { status: 500 });
  }
}
