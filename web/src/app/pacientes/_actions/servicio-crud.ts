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

function text(fd: FormData, key: string): string {
  return String(fd.get(key) ?? "").trim();
}

function parseMonto(raw: string): number {
  const n = Number(raw.replace(",", "."));
  return Number.isFinite(n) ? n : NaN;
}

function parsePagos(raw: string): PagoInput[] {
  try {
    const arr = JSON.parse(raw) as PagoInput[];
    if (!Array.isArray(arr)) return [];
    return arr.map((p) => ({
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

function parseDeleteIds(raw: string): string[] {
  try {
    const arr = JSON.parse(raw) as string[];
    if (!Array.isArray(arr)) return [];
    return arr.map((x) => String(x).trim()).filter(Boolean);
  } catch {
    return [];
  }
}

function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate()
  ).padStart(2, "0")}`;
}

export async function saveServicioAction(formData: FormData) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/pacientes");
  const opticaId = activeOptica.opticaId;
  const supabase = await createClient();

  const servicioId = text(formData, "servicioId");
  const pacienteIdRaw = text(formData, "pacienteId");
  const pacienteId = pacienteIdRaw || null;
  const ot = text(formData, "ot");
  const descripcion = text(formData, "descripcion");
  const montoTotalRaw = text(formData, "montoTotal");
  const estado = text(formData, "estado") || "Pendiente";
  const fecha = text(formData, "fecha") || todayIso();
  const pagos = parsePagos(text(formData, "pagosJson"));
  const pagosDeleteIds = parseDeleteIds(text(formData, "pagosDeleteJson"));
  const returnTo = text(formData, "returnTo") || "/servicios-varios";
  const errorBase = text(formData, "errorBase") || "/servicios-varios/nuevo";

  if (!descripcion || !montoTotalRaw) {
    redirect(`${errorBase}?error=descripcion_monto_requeridos`);
  }
  const montoTotal = parseMonto(montoTotalRaw);
  if (!Number.isFinite(montoTotal)) redirect(`${errorBase}?error=monto_no_numerico`);
  if (montoTotal <= 0) redirect(`${errorBase}?error=total_mayor_0`);
  if (pagos.some((p) => !Number.isFinite(p.monto) || p.monto <= 0)) {
    redirect(`${errorBase}?error=pago_mayor_0`);
  }
  const aCuenta = pagos.reduce((acc, p) => acc + p.monto, 0);
  if (aCuenta > montoTotal) redirect(`${errorBase}?error=pago_mayor_total`);

  const finalId = servicioId || crypto.randomUUID();
  const payload = {
    id: finalId,
    ot,
    descripcion,
    monto_total: montoTotal,
    a_cuenta: aCuenta,
    estado,
    fecha,
    paciente_id: pacienteId,
    metodo_pago: "",
    optica_id: opticaId
  };

  const q = servicioId
    ? supabase.from("servicios_extra").update(payload).eq("id", finalId).eq("optica_id", opticaId)
    : supabase.from("servicios_extra").insert(payload);
  const { error } = await q;
  if (error?.code === "23503") redirect(`${errorBase}?error=fk_paciente`);
  if (error) redirect(`${errorBase}?error=guardar`);

  for (const p of pagos) {
    const pagoPayload = {
      id: p.id || crypto.randomUUID(),
      dispensacion_id: null,
      servicio_extra_id: finalId,
      fecha: p.fecha || todayIso(),
      tipo: "Abono",
      monto: p.monto,
      metodo_pago: p.metodoPago,
      nota: p.nota,
      optica_id: opticaId
    };
    const { error: pErr } = await supabase.from("pagos").upsert(pagoPayload, { onConflict: "id" });
    if (pErr) redirect(`${errorBase}?error=guardar`);
  }

  for (const pagoId of pagosDeleteIds) {
    const { data: existing } = await supabase
      .from("pagos")
      .select("id,dispensacion_id,servicio_extra_id,monto,metodo_pago")
      .eq("id", pagoId)
      .eq("optica_id", opticaId)
      .eq("servicio_extra_id", finalId)
      .maybeSingle();
    if (!existing) continue;
    if (Number(existing.monto ?? 0) !== 0) {
      const { error: annErr } = await supabase.from("pagos").insert({
        id: crypto.randomUUID(),
        dispensacion_id: existing.dispensacion_id,
        servicio_extra_id: existing.servicio_extra_id,
        fecha: todayIso(),
        tipo: "Anulacion",
        monto: -Math.abs(Number(existing.monto ?? 0)),
        metodo_pago: existing.metodo_pago,
        nota: `Anula abono ${String(existing.id).slice(0, 8)}`,
        optica_id: opticaId
      });
      if (annErr) redirect(`${errorBase}?error=guardar`);
    }
    const { error: delErr } = await supabase
      .from("pagos")
      .delete()
      .eq("id", pagoId)
      .eq("optica_id", opticaId);
    if (delErr) redirect(`${errorBase}?error=guardar`);
  }

  revalidatePath("/servicios-varios");
  revalidatePath("/pacientes");
  if (pacienteId) revalidatePath(`/pacientes/${pacienteId}/servicios-extra`);
  redirect(`${returnTo}?msg=${servicioId ? "actualizado" : "creado"}`);
}
