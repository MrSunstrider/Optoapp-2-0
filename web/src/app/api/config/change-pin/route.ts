import { NextResponse } from "next/server";

import { hashWebPin, verifyWebPinAgainstMetadata } from "@/lib/pin-verification";
import { createClient } from "@/lib/supabase/server";

type Body = {
  currentPin?: string;
  newPin?: string;
  confirmPin?: string;
};

export async function POST(request: Request) {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();

  if (!user) {
    return NextResponse.json({ error: "No autorizado." }, { status: 401 });
  }

  let body: Body;
  try {
    body = (await request.json()) as Body;
  } catch {
    return NextResponse.json({ error: "JSON inválido." }, { status: 400 });
  }

  const currentPin = sanitizePin(body.currentPin);
  const newPin = sanitizePin(body.newPin);
  const confirmPin = sanitizePin(body.confirmPin);

  if (currentPin.length !== 6 || newPin.length !== 6 || confirmPin.length !== 6) {
    return NextResponse.json(
      { error: "Todos los campos de PIN deben tener 6 dígitos." },
      { status: 400 }
    );
  }
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
      { error: "PIN no configurado para esta cuenta." },
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

function sanitizePin(value: unknown): string {
  return String(value ?? "").replace(/\D/g, "").slice(0, 6);
}
