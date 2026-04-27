import { createServerClient } from "@supabase/ssr";
import { NextResponse, type NextRequest } from "next/server";
import { ACTIVE_OPTICA_COOKIE } from "@/lib/optica-context";

type CookieToSet = {
  name: string;
  value: string;
  options?: any;
};

export async function updateSession(request: NextRequest) {
  let supabaseResponse = NextResponse.next({ request });

  const supabase = createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY!,
    {
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
    }
  );

  const {
    data: { user }
  } = await supabase.auth.getUser();

  const publicPaths = ["/login", "/auth/callback"];
  const isPublic = publicPaths.some((path) =>
    request.nextUrl.pathname.startsWith(path)
  );
  const isSeleccionOptica = request.nextUrl.pathname.startsWith("/seleccion-optica");
  const activeOpticaCookie = request.cookies.get(ACTIVE_OPTICA_COOKIE)?.value;

  if (!user && !isPublic) {
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    return NextResponse.redirect(url);
  }

  if (user && request.nextUrl.pathname === "/login") {
    const url = request.nextUrl.clone();
    url.pathname = activeOpticaCookie ? "/dashboard" : "/seleccion-optica";
    return NextResponse.redirect(url);
  }

  if (user && !isPublic && !isSeleccionOptica && !activeOpticaCookie) {
    const url = request.nextUrl.clone();
    url.pathname = "/seleccion-optica";
    return NextResponse.redirect(url);
  }

  return supabaseResponse;
}
