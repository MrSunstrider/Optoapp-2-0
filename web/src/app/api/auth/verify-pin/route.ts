import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";
import { PIN_VERIFIED_COOKIE } from "@/lib/pin-cookie";
import { isPinRequiredFromUser } from "@/lib/pin-policy";
import { verifyWebPinAgainstMetadata } from "@/lib/pin-verification";

const PIN_LENGTH = 6;

export async function POST(request: Request) {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();

  if (!user) {
    return NextResponse.json({ error: "Sesión no válida." }, { status: 401 });
  }

  if (!isPinRequiredFromUser(user)) {
    const res = NextResponse.json({ ok: true, skipped: true });
    setPinCookie(res);
    return res;
  }

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Solicitud inválida." }, { status: 400 });
  }

  const pin =
    typeof body === "object" && body !== null && "pin" in body
      ? String((body as { pin?: unknown }).pin ?? "").replace(/\D/g, "")
      : "";

  if (pin.length !== PIN_LENGTH) {
    return NextResponse.json({ error: "PIN inválido." }, { status: 400 });
  }

  const meta = (user.user_metadata ?? {}) as Record<string, unknown>;
  const result = verifyWebPinAgainstMetadata(pin, user.id, meta);

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
  setPinCookie(res);
  return res;
}

function setPinCookie(res: NextResponse) {
  res.cookies.set(PIN_VERIFIED_COOKIE, "1", {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 60 * 8
  });
}
