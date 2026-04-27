import { cookies } from "next/headers";

export const ACTIVE_OPTICA_COOKIE = "optoapp_active_optica";

export type ActiveOpticaContext = {
  opticaId: string;
  rol: string;
  nombre: string;
};

export async function getActiveOpticaContext(): Promise<ActiveOpticaContext | null> {
  const cookieStore = await cookies();
  const raw = cookieStore.get(ACTIVE_OPTICA_COOKIE)?.value;
  if (!raw) return null;
  const parsed = safeParse(raw);
  if (!parsed) return null;
  if (!parsed.opticaId || !parsed.rol || !parsed.nombre) return null;
  return parsed;
}

export async function setActiveOpticaContext(ctx: ActiveOpticaContext): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.set(
    ACTIVE_OPTICA_COOKIE,
    JSON.stringify({
      opticaId: ctx.opticaId,
      rol: ctx.rol,
      nombre: ctx.nombre
    }),
    {
      httpOnly: true,
      secure: true,
      sameSite: "lax",
      path: "/",
      maxAge: 60 * 60 * 24 * 30
    }
  );
}

function safeParse(value: string): ActiveOpticaContext | null {
  try {
    return JSON.parse(value) as ActiveOpticaContext;
  } catch {
    return null;
  }
}
