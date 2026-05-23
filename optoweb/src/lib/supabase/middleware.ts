import { createServerClient } from "@supabase/ssr";
import type { CookieOptions } from "@supabase/ssr";
import { NextResponse, type NextRequest } from "next/server";
import { ACTIVE_OPTICA_COOKIE, OPTICA_VALIDATED_COOKIE, OPTICA_VALIDATED_TTL_SECS } from "@/lib/optica-cookie";
import { PIN_VERIFIED_COOKIE } from "@/lib/pin-cookie";
import { isPinRequiredFromUser } from "@/lib/pin-policy";
import { getPublicSupabaseEnv } from "@/lib/supabase/env-public";

type CookieToSet = {
  name: string;
  value: string;
  options: CookieOptions;
};

export async function updateSession(request: NextRequest) {
  const pathname = request.nextUrl.pathname;

  if (
    pathname.startsWith("/_next/") ||
    pathname.startsWith("/api/") ||
    pathname === "/favicon.ico"
  ) {
    return NextResponse.next({ request });
  }

  try {
    return await runSessionMiddleware(request);
  } catch (e) {
    console.error("[middleware] Error no controlado en sesión:", e);
    const res = NextResponse.next({ request });
    res.headers.set("x-optoapp-middleware-error", "1");
    return res;
  }
}

async function runSessionMiddleware(request: NextRequest) {
  const env = getPublicSupabaseEnv();
  if (!env) {
    if (request.nextUrl.pathname === "/login") {
      return NextResponse.next({ request });
    }
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    url.searchParams.set("error", "configuracion");
    return NextResponse.redirect(url);
  }

  let supabaseResponse = NextResponse.next({ request });

  const supabase = createServerClient(env.url, env.key, {
    cookies: {
      getAll() {
        return request.cookies.getAll();
      },
      setAll(cookiesToSet: CookieToSet[]) {
        cookiesToSet.forEach(({ name, value, options }) =>
          request.cookies.set(name, value)
        );
        supabaseResponse = NextResponse.next({ request });
        cookiesToSet.forEach(({ name, value, options }) =>
          supabaseResponse.cookies.set(name, value, options)
        );
      }
    }
  });

  const {
    data: { user }
  } = await supabase.auth.getUser();

  const publicPaths = ["/login", "/auth/callback"];
  const isPublic = publicPaths.some((path) =>
    request.nextUrl.pathname.startsWith(path)
  );
  const isSeleccionOptica =
    request.nextUrl.pathname.startsWith("/seleccion-optica");
  const isAuthSelectOptica = request.nextUrl.pathname.startsWith(
    "/auth/select-optica"
  );
  const activeOpticaCookie = request.cookies.get(ACTIVE_OPTICA_COOKIE)?.value;
  const parsedActiveOptica = parseActiveOpticaCookie(activeOpticaCookie);

  const pinOk = request.cookies.get(PIN_VERIFIED_COOKIE)?.value === "1";
  const pinReq = user ? isPinRequiredFromUser(user) : false;
  const isPinPage = request.nextUrl.pathname.startsWith("/pin");
  const allowsWithoutPinComplete =
    isPinPage ||
    request.nextUrl.pathname.startsWith("/auth/callback") ||
    request.nextUrl.pathname.startsWith("/auth/logout");

  if (!user && !isPublic) {
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    return NextResponse.redirect(url);
  }

  if (user && pinReq && !pinOk && !allowsWithoutPinComplete) {
    const url = request.nextUrl.clone();
    url.pathname = "/pin";
    return NextResponse.redirect(url);
  }

  if (user && isPinPage && (!pinReq || pinOk)) {
    const url = request.nextUrl.clone();
    url.pathname = parsedActiveOptica ? "/dashboard" : "/seleccion-optica";
    return NextResponse.redirect(url);
  }

  if (user && request.nextUrl.pathname === "/login") {
    const url = request.nextUrl.clone();
    if (pinReq && !pinOk) {
      url.pathname = "/pin";
    } else {
      url.pathname = parsedActiveOptica ? "/dashboard" : "/seleccion-optica";
    }
    return NextResponse.redirect(url);
  }

  if (
    user &&
    !isPublic &&
    !isSeleccionOptica &&
    !isAuthSelectOptica &&
    !parsedActiveOptica &&
    !isPinPage
  ) {
    const url = request.nextUrl.clone();
    url.pathname = "/seleccion-optica";
    return NextResponse.redirect(url);
  }

  if (user && parsedActiveOptica) {
    // M8: Saltar validación si ya se verificó hace menos de 5 minutos
    const validatedAt = request.cookies.get(OPTICA_VALIDATED_COOKIE)?.value;
    const now = Date.now();
    const isValidated =
      validatedAt &&
      now - Number(validatedAt) < OPTICA_VALIDATED_TTL_SECS * 1000;

    if (!isValidated) {
      try {
        const { data, error } = await supabase
          .from("usuario_optica")
          .select("optica_id,rol")
          .eq("user_id", user.id)
          .eq("optica_id", parsedActiveOptica.opticaId)
          .limit(1)
          .abortSignal(AbortSignal.timeout(6000));

        if (error || !data || data.length === 0) {
          const url = request.nextUrl.clone();
          url.pathname = "/seleccion-optica";
          const res = NextResponse.redirect(url);
          res.cookies.delete(ACTIVE_OPTICA_COOKIE);
          return res;
        }

        // M3: Si el rol cambió (admin lo modificó), actualizar la cookie
        const currentRol = data[0]?.rol?.trim().toLowerCase() ?? "";
        if (currentRol && currentRol !== parsedActiveOptica.rol.trim().toLowerCase()) {
          const updated = { ...parsedActiveOptica, rol: currentRol };
          supabaseResponse.cookies.set(ACTIVE_OPTICA_COOKIE, JSON.stringify(updated), {
            httpOnly: true,
            secure: process.env.NODE_ENV === "production",
            sameSite: "lax",
            path: "/",
            maxAge: 60 * 60 * 24 * 30,
          });
        }

        // Marcar como validado para los próximos 5 minutos
        supabaseResponse.cookies.set(OPTICA_VALIDATED_COOKIE, String(now), {
          maxAge: OPTICA_VALIDATED_TTL_SECS,
          path: "/",
          sameSite: "lax",
          httpOnly: true,
          secure: process.env.NODE_ENV === "production",
        });
      } catch (err) {
        console.error(
          "[middleware] No se pudo validar usuario_optica (red, timeout u otro):",
          err
        );
        return supabaseResponse;
      }
    }
  }

  return supabaseResponse;
}

function parseActiveOpticaCookie(raw?: string): { opticaId: string; rol: string } | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as { opticaId?: string; rol?: string };
    if (!parsed.opticaId || parsed.opticaId.trim().length === 0) return null;
    return { opticaId: parsed.opticaId, rol: parsed.rol?.trim().toLowerCase() ?? "" };
  } catch {
    if (process.env.NODE_ENV === "development") {
      console.warn(
        "[middleware] Cookie optoapp_active_optica no es JSON válido; se ignora."
      );
    }
    return null;
  }
}
