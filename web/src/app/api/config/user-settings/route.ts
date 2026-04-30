import { NextResponse } from "next/server";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { upsertOpticaSettingsPatch } from "@/lib/optica-settings";
import { createClient } from "@/lib/supabase/server";

type Body = {
  pinRequired?: boolean;
  automaticReminders?: boolean;
  opticaConfigPatch?: Record<string, unknown>;
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

  const metadata = (user.user_metadata ?? {}) as Record<string, unknown>;
  const nextMetadata: Record<string, unknown> = { ...metadata };
  let changed = false;

  if (typeof body.pinRequired === "boolean") {
    nextMetadata.optoapp_pin_required = body.pinRequired;
    changed = true;
  }
  if (typeof body.automaticReminders === "boolean") {
    nextMetadata.optoapp_automatic_reminders = body.automaticReminders;
    changed = true;
  }
  if (body.opticaConfigPatch && typeof body.opticaConfigPatch === "object") {
    const activeOptica = await getActiveOpticaContext();
    if (!activeOptica) {
      return NextResponse.json(
        { error: "Sin óptica activa para guardar esta preferencia." },
        { status: 400 }
      );
    }
    const persisted = await upsertOpticaSettingsPatch(
      supabase,
      activeOptica.opticaId,
      body.opticaConfigPatch
    );
    if (!persisted.ok && !persisted.missingTable) {
      return NextResponse.json(
        { error: "No se pudo guardar configuración de óptica." },
        { status: 500 }
      );
    }
    if (persisted.missingTable) {
      // Fallback compatible hasta desplegar migración de tabla compartida.
      const prev = asRecord(nextMetadata.optoapp_optica_config);
      const current = asRecord(prev[activeOptica.opticaId]);
      prev[activeOptica.opticaId] = { ...current, ...body.opticaConfigPatch };
      nextMetadata.optoapp_optica_config = prev;
    }
    changed = true;
  }

  if (!changed) {
    return NextResponse.json(
      { error: "No hay cambios para guardar." },
      { status: 400 }
    );
  }

  const { error } = await supabase.auth.updateUser({
    data: nextMetadata
  });

  if (error) {
    return NextResponse.json(
      { error: "No se pudo guardar la configuración." },
      { status: 500 }
    );
  }

  return NextResponse.json({
    ok: true,
    pinRequired:
      typeof nextMetadata.optoapp_pin_required === "boolean"
        ? nextMetadata.optoapp_pin_required
        : true,
    automaticReminders:
      typeof nextMetadata.optoapp_automatic_reminders === "boolean"
        ? nextMetadata.optoapp_automatic_reminders
        : true,
    opticaConfig: nextMetadata.optoapp_optica_config ?? {}
  });
}

function asRecord(value: unknown): Record<string, unknown> {
  if (value && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return {};
}
