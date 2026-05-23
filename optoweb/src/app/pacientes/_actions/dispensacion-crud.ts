"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { z } from "zod";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";
import { parsePagos, parseDeletedIds, parseMonto, todayIsoDate, type PagoInput } from "@/lib/payment-utils";

const itemSchema = z.object({
  id: z.string(),
  tipoLente: z.string(),
  subTipoBifocal: z.string(),
  distanciaLente: z.string(),
  altura: z.string(),
  materialLente: z.string(),
  tratamientos: z.array(z.string()),
  colorLente: z.string(),
  notasDiseno: z.string(),
  origenMontura: z.string(),
  monturaId: z.string(),
  tipoAro: z.string(),
  materialMontura: z.string(),
  descripcionMontura: z.string(),
});

function text(formData: FormData, key: string): string {
  return String(formData.get(key) ?? "").trim();
}

interface StockResult {
  ok: boolean;
  error?: string;
}

function isStockResult(v: unknown): v is StockResult {
  return typeof v === "object" && v !== null && "ok" in v;
}

export async function saveDispensacionAction(
  prevState: { error?: string } | null,
  formData: FormData,
): Promise<{ error?: string } | null> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { error: "Sin óptica activa" };
  if (!canManagePacientes(activeOptica.rol)) return { error: "Sin permiso" };
  const opticaId = activeOptica.opticaId;

  const supabase = await createClient();
  const pacienteId = text(formData, "pacienteId");
  const dispensacionId = text(formData, "dispensacionId");
  const fecha = text(formData, "fecha");
  const ot = text(formData, "ot");
  const montoTotalRaw = text(formData, "montoTotal");
  const estadoEntrega = text(formData, "estadoEntrega") || "Pendiente";
  const pagos = parsePagos(text(formData, "pagosJson"));
  const pagosDeleteIds = parseDeletedIds(text(formData, "pagosDeleteJson"));

  const itemsRaw = text(formData, "itemsJson");
  const itemsDeleteRaw = text(formData, "itemsDeleteJson");
  const previousItemIdsRaw = text(formData, "previousItemIdsJson");

  const errorBase = dispensacionId
    ? `/pacientes/${pacienteId}/dispensaciones/${dispensacionId}/editar`
    : `/pacientes/${pacienteId}/dispensaciones/nueva`;

  if (!pacienteId) return { error: "guardar" };
  if (!ot) return { error: "ot" };

  const montoTotal = parseMonto(montoTotalRaw);
  if (!Number.isFinite(montoTotal) || montoTotal <= 0) {
    return { error: "total_mayor_0" };
  }
  if (pagos.some((p) => !Number.isFinite(p.monto) || p.monto <= 0)) {
    return { error: "pago_mayor_0" };
  }
  const montoPagado = pagos.reduce((acc, p) => acc + p.monto, 0);
  if (montoPagado > montoTotal) {
    return { error: "pago_mayor_total" };
  }

  // Parse items
  let items: z.infer<typeof itemSchema>[];
  try {
    items = z.array(itemSchema).parse(JSON.parse(itemsRaw));
  } catch {
    return { error: "guardar" };
  }
  if (items.length === 0) return { error: "guardar" };

  const previousItemIds: string[] = (() => {
    try {
      return z.array(z.string()).parse(JSON.parse(previousItemIdsRaw));
    } catch {
      return [];
    }
  })();

  const itemsDeleteIds: string[] = (() => {
    try {
      return z.array(z.string()).parse(JSON.parse(itemsDeleteRaw));
    } catch {
      return [];
    }
  })();

  const normalizedOt = ot.trim().toUpperCase();
  const finalId = dispensacionId || crypto.randomUUID();

  // Primer item → datos principales del entity (backward compat)
  const first = items[0];
  const requiresAltura =
    first.tipoLente === "Bifocal" ||
    first.tipoLente === "Progresivo" ||
    first.tipoLente === "Ocupacional";
  const finalMonturaId = first.origenMontura === "Tienda" ? first.monturaId : "";
  const tratamientos = Array.from(
    new Set(first.tratamientos.map((x) => x.trim()).filter((x) => x && x !== "Ninguno")),
  );

  // Stock adjustments: compare previous item monturas vs new item monturas
  async function getItemMonturas(itemIds: string[]): Promise<string[]> {
    if (itemIds.length === 0) return [];
    const { data } = await supabase
      .from("dispensacion_items")
      .select("montura_id, origen_montura")
      .in("id", itemIds)
      .eq("optica_id", opticaId);
    return (data ?? [])
      .filter((r) => r.origen_montura === "Tienda" && r.montura_id)
      .map((r) => r.montura_id!);
  }

  const oldTiendaMonturas = dispensacionId
    ? await getItemMonturas(previousItemIds)
    : [];

  const newTiendaMonturas = items
    .filter((i) => i.origenMontura === "Tienda" && i.monturaId)
    .map((i) => i.monturaId);

  const toAddStock = oldTiendaMonturas.filter((id) => !newTiendaMonturas.includes(id));
  const toRemoveStock = newTiendaMonturas.filter((id) => !oldTiendaMonturas.includes(id));

  for (const mid of toAddStock) {
    const { data: r, error: rpcErr } = await supabase.rpc("rpc_adjust_montura_stock", {
      p_montura_id: mid,
      p_optica_id: opticaId,
      p_delta: 1,
      p_reference_id: finalId,
      p_note: "Reversión por edición de dispensación",
      p_tipo: "AJUSTE",
      p_fecha: fecha,
    });
    const result = isStockResult(r) ? r : null;
    if (rpcErr || !result?.ok) {
      redirect(`${errorBase}?error=guardar`);
    }
  }

  for (const mid of toRemoveStock) {
    const { data: r, error: rpcErr } = await supabase.rpc("rpc_adjust_montura_stock", {
      p_montura_id: mid,
      p_optica_id: opticaId,
      p_delta: -1,
      p_reference_id: finalId,
      p_note: "Salida por venta en dispensación",
      p_tipo: "SALIDA_VENTA",
      p_fecha: fecha,
    });
    const result = isStockResult(r) ? r : null;
    if (rpcErr || !result?.ok) {
      if (result?.error === "insufficient") redirect(`${errorBase}?error=stock`);
      redirect(`${errorBase}?error=guardar`);
    }
  }

  // ── Save dispensacion header ──
  const payload = {
    id: finalId,
    paciente_id: pacienteId,
    optica_id: opticaId,
    ot: normalizedOt,
    fecha,
    tipo_lente: first.tipoLente,
    sub_tipo_bifocal: first.tipoLente === "Bifocal" ? first.subTipoBifocal : "",
    distancia_lente: first.tipoLente === "Monofocal" ? first.distanciaLente : "",
    altura: requiresAltura ? first.altura : "",
    material_lente: first.materialLente,
    tratamientos,
    color_lente: first.colorLente,
    notas_diseno: first.notasDiseno,
    origen_montura: first.origenMontura,
    montura_id: finalMonturaId,
    tipo_aro: first.tipoAro,
    material_montura: first.materialMontura,
    descripcion_montura: first.descripcionMontura,
    monto_total: montoTotal,
    monto_pagado: montoPagado,
    estado_entrega: estadoEntrega,
  };

  const saveDisp = dispensacionId
    ? supabase.from("dispensaciones").update(payload).eq("id", finalId).eq("optica_id", opticaId)
    : supabase.from("dispensaciones").insert(payload);
  const { error: saveErr } = await saveDisp;
  if (saveErr) redirect(`${errorBase}?error=guardar`);

  // ── Save items ──
  // Delete old items (re-insert)
  await supabase
    .from("dispensacion_items")
    .delete()
    .eq("dispensacion_id", finalId)
    .eq("optica_id", opticaId);

  for (const item of items) {
    const itemRequiresAltura =
      item.tipoLente === "Bifocal" ||
      item.tipoLente === "Progresivo" ||
      item.tipoLente === "Ocupacional";
    const { error: itemErr } = await supabase.from("dispensacion_items").insert({
      id: item.id,
      dispensacion_id: finalId,
      tipo_lente: item.tipoLente,
      sub_tipo_bifocal: item.tipoLente === "Bifocal" ? item.subTipoBifocal : "",
      distancia_lente: item.tipoLente === "Monofocal" ? item.distanciaLente : "",
      altura: itemRequiresAltura ? item.altura : "",
      material_lente: item.materialLente,
      tratamientos: Array.from(new Set(item.tratamientos.filter((x) => x && x !== "Ninguno"))),
      color_lente: item.colorLente,
      notas_diseno: item.notasDiseno,
      montura_id: item.origenMontura === "Tienda" ? item.monturaId : "",
      origen_montura: item.origenMontura,
      tipo_aro: item.tipoAro,
      material_montura: item.materialMontura,
      descripcion_montura: item.descripcionMontura,
      optica_id: opticaId,
    });
    if (itemErr) redirect(`${errorBase}?error=guardar`);
  }

  // Delete removed items
  for (const itemId of itemsDeleteIds) {
    await supabase
      .from("dispensacion_items")
      .delete()
      .eq("id", itemId)
      .eq("optica_id", opticaId);
  }

  // ── Save pagos (same as before) ──
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
      optica_id: activeOptica.opticaId,
    };
    const { error } = await supabase.from("pagos").upsert(pagoPayload, { onConflict: "id" });
    if (error) redirect(`${errorBase}?error=guardar`);
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
      await supabase.from("pagos").insert({
        id: crypto.randomUUID(),
        dispensacion_id: existing.dispensacion_id,
        servicio_extra_id: existing.servicio_extra_id,
        fecha: todayIsoDate(),
        tipo: "Anulacion",
        monto: -Math.abs(Number(existing.monto ?? 0)),
        metodo_pago: existing.metodo_pago,
        nota: `Anula abono ${String(existing.id).slice(0, 8)}`,
        optica_id: opticaId,
      });
    }
    await supabase.from("pagos").delete().eq("id", pagoId).eq("optica_id", opticaId);
  }

  revalidatePath(`/pacientes/${pacienteId}/dispensaciones`);
  revalidatePath("/pacientes");
  revalidatePath("/dashboard");
  revalidatePath("/reportes");
  redirect(
    `/pacientes/${pacienteId}/dispensaciones?msg=${dispensacionId ? "actualizada" : "creada"}`,
  );
  return null;
}
