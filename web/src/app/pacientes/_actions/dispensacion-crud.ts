"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { z } from "zod";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

const PagoInputSchema = z.object({
  id: z.string(),
  fecha: z.string(),
  monto: z.number(),
  metodoPago: z.string(),
  nota: z.string(),
});

type PagoInput = z.infer<typeof PagoInputSchema>;

function text(formData: FormData, key: string): string {
  return String(formData.get(key) ?? "").trim();
}

function parseMonto(raw: string): number {
  const n = Number(raw.replace(",", "."));
  return Number.isFinite(n) ? n : NaN;
}

function parsePagos(raw: string): PagoInput[] {
  const arr = JSON.parse(raw);
  const parsed = z.array(PagoInputSchema).safeParse(arr);
  if (!parsed.success) throw new Error("Invalid pagos data");
  return parsed.data;
}

function parseDeletedIds(raw: string): string[] {
  const arr = JSON.parse(raw);
  const parsed = z.array(z.string()).safeParse(arr);
  if (!parsed.success) throw new Error("Invalid deleted-ids data");
  return parsed.data.map((x) => x.trim()).filter(Boolean);
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
  if (!pacienteId) redirect(`${errorBase}?error=guardar`);

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

  function isStockResult(v: unknown): v is { ok: boolean; error?: string } {
    return typeof v === "object" && v !== null && "ok" in v;
  }

  if (previousMonturaId && previousMonturaId !== finalMonturaId) {
    const { data: r, error: rpcErr } = await supabase.rpc("rpc_adjust_montura_stock", {
      p_montura_id: previousMonturaId,
      p_optica_id: opticaId,
      p_delta: 1,
      p_reference_id: finalId,
      p_note: "Reversion por edicion de dispensacion",
      p_tipo: "AJUSTE",
      p_fecha: fecha,
    });
    const result = isStockResult(r) ? r : null;
    if (rpcErr || !result?.ok) {
      redirect(`${errorBase}?error=guardar`);
    }
  }
  if (finalMonturaId && finalMonturaId !== previousMonturaId) {
    const { data: r, error: rpcErr } = await supabase.rpc("rpc_adjust_montura_stock", {
      p_montura_id: finalMonturaId,
      p_optica_id: opticaId,
      p_delta: -1,
      p_reference_id: finalId,
      p_note: "Salida por venta en dispensacion",
      p_tipo: "SALIDA_VENTA",
      p_fecha: fecha,
    });
    const result = isStockResult(r) ? r : null;
    if (rpcErr || !result?.ok) {
      if (result?.error === "insufficient") {
        redirect(`${errorBase}?error=stock`);
      }
      redirect(`${errorBase}?error=guardar`);
    }
  }

  const tratamientos = (() => {
    const arr = JSON.parse(tratamientosRaw);
    const parsed = z.array(z.string()).safeParse(arr);
    if (!parsed.success) return [];
    return Array.from(new Set(parsed.data.map((x) => x.trim()).filter((x) => x && x !== "Ninguno")));
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
