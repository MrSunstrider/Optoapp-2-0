/**
 * Variables públicas de Supabase (NEXT_PUBLIC_*).
 * Deben estar en `web/.env.local`; ver `web/.env.example`.
 */

export type PublicSupabaseEnv = {
  url: string;
  key: string;
};

export function getPublicSupabaseEnv(): PublicSupabaseEnv | null {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL?.trim() ?? "";
  const key = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY?.trim() ?? "";
  if (!url || !key) return null;
  return { url, key };
}

/** Usar en servidor/cliente cuando la app no puede funcionar sin estas variables. */
export function requirePublicSupabaseEnv(): PublicSupabaseEnv {
  const env = getPublicSupabaseEnv();
  if (!env) {
    throw new Error(
      "Falta NEXT_PUBLIC_SUPABASE_URL o NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY. " +
        "En la carpeta web/, copia .env.example a .env.local y completa los valores del proyecto Supabase."
    );
  }
  return env;
}
