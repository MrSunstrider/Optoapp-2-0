import { createServerClient } from "@supabase/ssr";
import type { CookieOptions } from "@supabase/ssr";
import { cookies } from "next/headers";
import { requirePublicSupabaseEnv } from "@/lib/supabase/env-public";

type CookieToSet = {
  name: string;
  value: string;
  options: CookieOptions;
};

/** Mensaje típico de Next.js al mutar cookies fuera de Server Action / Route Handler. */
const RSC_COOKIE_RESTRICTION =
  "Cookies can only be modified in a Server Action or Route Handler";

export async function createClient() {
  const { url, key } = requirePublicSupabaseEnv();
  const cookieStore = await cookies();

  return createServerClient(url, key, {
    cookies: {
      getAll() {
        return cookieStore.getAll();
      },
      setAll(cookiesToSet: CookieToSet[]) {
        try {
          cookiesToSet.forEach(({ name, value, options }) =>
            cookieStore.set(name, value, options)
          );
        } catch (err) {
          const msg = err instanceof Error ? err.message : String(err);
          if (msg.includes(RSC_COOKIE_RESTRICTION)) {
            if (process.env.NODE_ENV === "development") {
              console.debug(
                "[supabase/ssr] No se pudieron escribir cookies en este Server Component; el middleware debe mantener la sesión."
              );
            }
            return;
          }
          console.error("[supabase/ssr] Error inesperado al escribir cookies:", err);
          throw err;
        }
      }
    }
  });
}
