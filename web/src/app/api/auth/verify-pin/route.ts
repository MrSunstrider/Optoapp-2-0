import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";
import { setPinVerifiedCookie } from "@/lib/pin-cookie";
import { isPinRequiredFromUser } from "@/lib/pin-policy";
import { verifyWebPinAgainstMetadata } from "@/lib/pin-verification";
import { checkRateLimit } from "@/lib/rate-limit";
import { VerifyPinSchema } from "@/lib/api-schemas";

export async function POST(request: Request) {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();

  if (!user) {
    return NextResponse.json({ error: "Sesión no válida." }, { status: 401 });
  }

  const rl = await checkRateLimit("pin-verify:" + user.id);
  if (!rl.allowed) {
    return NextResponse.json({ error: "Demasiados intentos. Espere 1 minuto." }, { status: 429 });
  }

  if (!isPinRequiredFromUser(user)) {
    const res = NextResponse.json({ ok: true, skipped: true });
    setPinVerifiedCookie(res);
    return res;
  }

  let raw: unknown;
  try {
    raw = await request.json();
  } catch {
    return NextResponse.json({ error: "Solicitud inválida." }, { status: 400 });
  }

  const parsed = VerifyPinSchema.safeParse(raw);
  if (!parsed.success) {
    return NextResponse.json({ error: "PIN inválido." }, { status: 400 });
  }

  const meta = (user.user_metadata ?? {}) as Record<string, unknown>;
  const result = verifyWebPinAgainstMetadata(parsed.data.pin, user.id, meta);

  if (!result.ok) {
    if (result.code === "unconfigured") {
      return NextResponse.json(
        {
          error:
            "El acceso web con PIN no está configurado para esta cuenta. Contacte al administrador."
        },
        { status: 503 }
      );
    }
    return NextResponse.json(
      { error: "No se pudo verificar el PIN. Intente de nuevo." },
      { status: 401 }
    );
  }

  const res = NextResponse.json({ ok: true });
  setPinVerifiedCookie(res);
  return res;
}
