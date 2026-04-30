import { NextResponse } from "next/server";

import {
  canCloseCierre,
  canOverrideCierre,
  fetchCierreCaja,
  normalizeFechaCierre
} from "@/lib/cierre-caja";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { createClient } from "@/lib/supabase/server";

type Body = { fecha?: string; action?: "close" | "reopen" };

export async function POST(request: Request) {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();
  if (!user) return NextResponse.json({ error: "No autorizado." }, { status: 401 });

  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) {
    return NextResponse.json({ error: "Sin óptica activa." }, { status: 400 });
  }

  let body: Body;
  try {
    body = (await request.json()) as Body;
  } catch {
    return NextResponse.json({ error: "Solicitud inválida." }, { status: 400 });
  }

  const fecha = normalizeFechaCierre(body.fecha);
  const action = body.action === "reopen" ? "reopen" : "close";
  if (action === "close" && !canCloseCierre(activeOptica.rol)) {
    return NextResponse.json({ error: "Sin permiso para cerrar caja." }, { status: 403 });
  }
  if (action === "reopen" && !canOverrideCierre(activeOptica.rol)) {
    return NextResponse.json({ error: "Sin permiso para reabrir caja." }, { status: 403 });
  }

  const snapshot = await fetchCierreCaja(supabase, activeOptica.opticaId, fecha);
  const payload = {
    optica_id: activeOptica.opticaId,
    fecha,
    estado: action === "close" ? "cerrado" : "abierto",
    total: snapshot.total,
    totales_json: {
      efectivo: snapshot.efectivo,
      movilTrans: snapshot.movilTrans,
      tarjeta: snapshot.tarjeta
    },
    detalle_json: snapshot.transacciones,
    cerrado_por: action === "close" ? user.id : null,
    cerrado_at: action === "close" ? new Date().toISOString() : null,
    updated_at: new Date().toISOString()
  };

  const { error } = await supabase.from("cierres_caja").upsert(payload, {
    onConflict: "optica_id,fecha"
  });

  if (error?.code === "42P01") {
    return NextResponse.json(
      { error: "Cierre formal no habilitado: falta tabla cierres_caja." },
      { status: 501 }
    );
  }
  if (error) {
    return NextResponse.json({ error: "No se pudo persistir el cierre." }, { status: 500 });
  }

  return NextResponse.json({
    ok: true,
    message:
      action === "close"
        ? "Caja cerrada correctamente."
        : "Caja reabierta correctamente."
  });
}
