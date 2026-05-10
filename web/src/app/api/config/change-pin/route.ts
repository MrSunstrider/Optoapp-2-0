import { NextResponse } from "next/server";

import { hashWebPin, verifyWebPinAgainstMetadata } from "@/lib/pin-verification";
import { createClient } from "@/lib/supabase/server";
import { checkRateLimit } from "@/lib/rate-limit";
import { ChangePinSchema } from "@/lib/api-schemas";

export async function POST(request: Request) {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();

  if (!user) {
    return NextResponse.json({ error: "No autorizado." }, { status: 401 });
  }

  const rl = checkRateLimit("pin-change:" + user.id);
  if (!rl.allowed) {
    return NextResponse.json({ error: "Demasiados intentos. Espere 1 minuto." }, { status: 429 });
  }

  let raw: unknown;
  try {
    raw = await request.json();
  } catch {
    return NextResponse.json({ error: "JSON inválido." }, { status: 400 });
  }

  const parsed = ChangePinSchema.safeParse(raw);
  if (!parsed.success) {
    return NextResponse.json(
      { error: "Todos los campos de PIN deben tener 6 dígitos." },
      { status: 400 }
    );
  }

  const { currentPin, newPin, confirmPin } = parsed.data;

  if (newPin !== confirmPin) {
    return NextResponse.json(
      { error: "La confirmación del nuevo PIN no coincide." },
      { status: 400 }
    );
  }
  if (currentPin === newPin) {
    return NextResponse.json(
      { error: "El nuevo PIN debe ser distinto al actual." },
      { status: 400 }
    );
  }

  const metadata = (user.user_metadata ?? {}) as Record<string, unknown>;
  const verify = verifyWebPinAgainstMetadata(currentPin, user.id, metadata);
  if (!verify.ok && verify.code === "unconfigured") {
    return NextResponse.json(
      { error: "No se puede cambiar el PIN en este momento." },
      { status: 503 }
    );
  }
  if (!verify.ok) {
    return NextResponse.json({ error: "PIN actual incorrecto." }, { status: 401 });
  }

  const updatedMetadata: Record<string, unknown> = {
    ...metadata,
    optoapp_web_pin_sha256: hashWebPin(newPin, user.id)
  };

  const { error } = await supabase.auth.updateUser({
    data: updatedMetadata
  });
  if (error) {
    return NextResponse.json(
      { error: "No se pudo actualizar el PIN." },
      { status: 500 }
    );
  }

  return NextResponse.json({ ok: true });
}
