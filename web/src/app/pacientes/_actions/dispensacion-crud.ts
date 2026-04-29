"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

type PagoInput = {
  id: string;
  fecha: string;
  monto: number;
  metodoPago: string;
  nota: string;
};

function text(formData: FormData, key: string): string {
  return String(formData.get(key) ?? "").trim();
}

function parseMonto(raw: string): number {
  const n = Number(raw.replace(",", "."));
  return Number.isFinite(n) ? n : NaN;
}

function parsePagos(raw: string): PagoInput[] {
  try {
    const arr = JSON.parse(raw) as PagoInput[];
    if (!Array.isArray(arr)) return [];
    return arr
      .map((p) => ({
        id: String(p.id ?? "").trim(),
        fecha: String(p.fecha ?? "").trim(),
        monto: Number(p.monto ?? 0),
        metodoPago: String(p.metodoPago ?? "").trim(),
        nota: String(p.nota ?? "").trim()
      }));
  } catch {
    return [];
  }
}

function parseDeletedIds(raw: string): string[] {
  try {
    const arr = JSON.parse(raw) as string[];
    if (!Array.isArray(arr)) return [];
    return arr.map((x) => String(x).trim()).filter(Boolean);
  } catch {
    return [];
  }
}

function todayIsoDate(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate()
  ).padStart(2, "0")}`;
}

export async function saveDispensacionAction(formData: FormData) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/pacientes");
  const opticaId = activeOptica.opticaId;

  const supabase = await createClient();
  const pacienteId = text(formData, "pacienteId");
  const dispensacionId = text(formData, "dispensacionId");
  const fecha = text(formData, "fecha");
  const ot = text(formData, "ot");
  const tipoLente = text(formData, "tipoLente");
  const altura = text(formData, "altura");
  const materialLente = text(formData, "materialLente");
  const tratamientosRaw = text(formData, "tratamientosJson");
  const colorLente = text(formData, "colorLente");
  const notasDiseno = text(formData, "notasDiseno");
  const origenMontura = text(formData, "origenMontura");
  const monturaId = text(formData, "monturaId");
  const previousMonturaId = text(formData, "previousMonturaId");
  const tipoAro = text(formData, "tipoAro");
  const materialMontura = text(formData, "materialMontura");
  const descripcionMontura = text(formData, "descripcionMontura");
  const distanciaLente = text(formData, "distanciaLente");
  const subTipoBifocal = text(formData, "subTipoBifocal");
  const estadoEntrega = text(formData, "estadoEntrega") || "Pendiente";
  const montoTotalRaw = text(formData, "montoTotal");
  const pagos = parsePagos(text(formData, "pagosJson"));
  const pagosDeleteIds = parseDeletedIds(text(formData, "pagosDeleteJson"));
  const errorBase = dispensacionId
    ? `/pacientes/${pacienteId}/dispensaciones/${dispensacionId}/editar`
    : `/pacientes/${pacienteId}/dispensaciones/nueva`;

  const requiresAltura =
    tipoLente === "Bifocal" || tipoLente === "Progresivo" || tipoLente === "Ocupacional";
  if (requiresAltura && !altura) {
    redirect(`${errorBase}?error=altura`);
  }
  if (!ot) {
    redirect(`${errorBase}?error=ot`);
  }

  const montoTotal = parseMonto(montoTotalRaw);
  if (!Number.isFinite(montoTotal) || montoTotal <= 0) {
    redirect(`${errorBase}?error=total_mayor_0`);
  }
  if (pagos.some((p) => !Number.isFinite(p.monto) || p.monto <= 0)) {
    redirect(`${errorBase}?error=pago_mayor_0`);
  }
  const montoPagado = pagos.reduce((acc, p) => acc + p.monto, 0);
  if (montoPagado > montoTotal) {
    redirect(`${errorBase}?error=pago_mayor_total`);
  }

  const normalizedOt = ot.trim().toUpperCase();
  const dupQuery = supabase
    .from("dispensaciones")
    .select("id")
    .eq("optica_id", opticaId)
    .ilike("ot", normalizedOt);
  if (dispensacionId) dupQuery.neq("id", dispensacionId);
  const { data: dupRows, error: dupErr } = await dupQuery.limit(1);
  if (dupErr) {
    redirect(`${errorBase}?error=guardar`);
  }
  if ((dupRows ?? []).length > 0) {
    redirect(`${errorBase}?error=ot_duplicada`);
  }

  const finalId = dispensacionId || crypto.randomUUID();
  const finalMonturaId = origenMontura === "Tienda" ? monturaId : "";

  async function adjustStockAndLog(id: string, delta: number, note: string, tipo: string) {
    const { data: mRow, error: mErr } = await supabase
      .from("monturas")
      .select("stock_actual")
      .eq("id", id)
      .eq("optica_id", opticaId)
      .maybeSingle();
    if (mErr || !mRow) return { ok: false as const, insufficient: false };
    const before = Number(mRow.stock_actual ?? 0);
    if (delta < 0 && before < Math.abs(delta)) return { ok: false as const, insufficient: true };
    const after = before + delta;
    const { error: upErr } = await supabase
      .from("monturas")
      .update({ stock_actual: after })
      .eq("id", id)
      .eq("optica_id", opticaId);
    if (upErr) return { ok: false as const, insufficient: false };
    const mov = {
      id: crypto.randomUUID(),
      montura_id: id,
      fecha: fecha,
      tipo,
      cantidad: Math.abs(delta),
      stock_previo: before,
      stock_nuevo: after,
      referencia_id: finalId,
      nota: note,
      optica_id: opticaId
    };
    await supabase.from("montura_movimientos").insert(mov);
    return { ok: true as const, insufficient: false };
  }

  if (previousMonturaId && previousMonturaId !== finalMonturaId) {
    const r = await adjustStockAndLog(
      previousMonturaId,
      +1,
      "Reversion por edicion de dispensacion",
      "AJUSTE"
    );
    if (!r.ok) {
      redirect(`${errorBase}?error=guardar`);
    }
  }
  if (finalMonturaId && finalMonturaId !== previousMonturaId) {
    const r = await adjustStockAndLog(
      finalMonturaId,
      -1,
      "Salida por venta en dispensacion",
      "SALIDA_VENTA"
    );
    if (!r.ok && r.insufficient) {
      redirect(`${errorBase}?error=stock`);
    }
    if (!r.ok) {
      redirect(`${errorBase}?error=guardar`);
    }
  }

  const tratamientos = (() => {
    try {
      const arr = JSON.parse(tratamientosRaw) as string[];
      if (!Array.isArray(arr)) return [];
      return Array.from(new Set(arr.map((x) => String(x).trim()).filter((x) => x && x !== "Ninguno")));
    } catch {
      return [];
    }
  })();

  const payload = {
    id: finalId,
    paciente_id: pacienteId,
    optica_id: opticaId,
    ot: normalizedOt,
    fecha,
    tipo_lente: tipoLente,
    sub_tipo_bifocal: tipoLente === "Bifocal" ? subTipoBifocal : "",
    distancia_lente: tipoLente === "Monofocal" ? distanciaLente : "",
    altura: requiresAltura ? altura : "",
    material_lente: materialLente,
    tratamientos,
    color_lente: colorLente,
    notas_diseno: notasDiseno,
    origen_montura: origenMontura,
    montura_id: finalMonturaId,
    tipo_aro: tipoAro,
    material_montura: materialMontura,
    descripcion_montura: descripcionMontura,
    monto_total: montoTotal,
    monto_pagado: montoPagado,
    estado_entrega: estadoEntrega
  };

  const saveDisp = dispensacionId
    ? supabase
        .from("dispensaciones")
        .update(payload)
        .eq("id", finalId)
        .eq("optica_id", opticaId)
    : supabase.from("dispensaciones").insert(payload);
  const { error: saveErr } = await saveDisp;
  if (saveErr) {
    redirect(`${errorBase}?error=guardar`);
  }

  for (const p of pagos) {
    const pId = p.id || crypto.randomUUID();
    const pagoPayload = {
      id: pId,
      dispensacion_id: finalId,
      servicio_extra_id: null,
      fecha: p.fecha || todayIsoDate(),
      tipo: "Abono",
      monto: p.monto,
      metodo_pago: p.metodoPago,
      nota: p.nota,
      optica_id: activeOptica.opticaId
    };
    const { error } = await supabase.from("pagos").upsert(pagoPayload, { onConflict: "id" });
    if (error) {
      redirect(`${errorBase}?error=guardar`);
    }
  }

  for (const pagoId of pagosDeleteIds) {
    const { data: existing } = await supabase
      .from("pagos")
      .select("id,dispensacion_id,servicio_extra_id,monto,metodo_pago,fecha")
      .eq("id", pagoId)
      .eq("optica_id", opticaId)
      .eq("dispensacion_id", finalId)
      .maybeSingle();
    if (!existing) continue;
    if (Number(existing.monto ?? 0) !== 0) {
      const { error: annErr } = await supabase.from("pagos").insert({
        id: crypto.randomUUID(),
        dispensacion_id: existing.dispensacion_id,
        servicio_extra_id: existing.servicio_extra_id,
        fecha: todayIsoDate(),
        tipo: "Anulacion",
        monto: -Math.abs(Number(existing.monto ?? 0)),
        metodo_pago: existing.metodo_pago,
        nota: `Anula abono ${String(existing.id).slice(0, 8)}`,
        optica_id: opticaId
      });
      if (annErr) {
        redirect(`${errorBase}?error=guardar`);
      }
    }
    const { error: delErr } = await supabase
      .from("pagos")
      .delete()
      .eq("id", pagoId)
      .eq("optica_id", opticaId);
    if (delErr) {
      redirect(`${errorBase}?error=guardar`);
    }
  }

  revalidatePath(`/pacientes/${pacienteId}/dispensaciones`);
  revalidatePath("/pacientes");
  revalidatePath("/dashboard");
  revalidatePath("/reportes");
  redirect(`/pacientes/${pacienteId}/dispensaciones?msg=${dispensacionId ? "actualizada" : "creada"}`);
}
