"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";
import { parsePagos, parseDeletedIds, parseMonto, todayIsoDate } from "@/lib/payment-utils";

function text(fd: FormData, key: string): string {
  return String(fd.get(key) ?? "").trim();
}

export async function saveServicioAction(
  prevState: { error?: string } | null,
  formData: FormData
): Promise<{ error?: string } | null> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { error: "Sin óptica activa" };
  if (!canManagePacientes(activeOptica.rol)) return { error: "Sin permiso" };
  const opticaId = activeOptica.opticaId;
  const supabase = await createClient();

  const servicioId = text(formData, "servicioId");
  const pacienteIdRaw = text(formData, "pacienteId");
  const pacienteId = pacienteIdRaw || null;
  const ot = text(formData, "ot");
  const descripcion = text(formData, "descripcion");
  const montoTotalRaw = text(formData, "montoTotal");
  const estado = text(formData, "estado") || "Pendiente";
  const fecha = text(formData, "fecha") || todayIsoDate();
  const pagos = parsePagos(text(formData, "pagosJson"));
  const pagosDeleteIds = parseDeletedIds(text(formData, "pagosDeleteJson"));
  const returnTo = text(formData, "returnTo") || "/servicios-varios";
  const errorBase = text(formData, "errorBase") || "/servicios-varios/nuevo";

  if (!descripcion || !montoTotalRaw) {
    return { error: "descripcion_monto_requeridos" };
  }
  const montoTotal = parseMonto(montoTotalRaw);
  if (!Number.isFinite(montoTotal)) return { error: "monto_no_numerico" };
  if (montoTotal <= 0) return { error: "total_mayor_0" };
  if (pagos.some((p) => !Number.isFinite(p.monto) || p.monto <= 0)) {
    return { error: "pago_mayor_0" };
  }
  const aCuenta = pagos.reduce((acc, p) => acc + p.monto, 0);
  if (aCuenta > montoTotal) return { error: "pago_mayor_total" };

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
      fecha: p.fecha || todayIsoDate(),
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
        fecha: todayIsoDate(),
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
  return null;
}

export async function deleteServicioExtraAction(servicioId: string) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/servicios-varios");
  const opticaId = activeOptica.opticaId;
  const supabase = await createClient();

  const id = String(servicioId ?? "").trim();
  if (!id) redirect("/servicios-varios?error=eliminar");

  const { data: row, error: fetchErr } = await supabase
    .from("servicios_extra")
    .select("id,paciente_id")
    .eq("id", id)
    .eq("optica_id", opticaId)
    .maybeSingle();

  if (fetchErr || !row) redirect("/servicios-varios?error=eliminar");

  const { error: delPagos } = await supabase
    .from("pagos")
    .delete()
    .eq("servicio_extra_id", id)
    .eq("optica_id", opticaId);

  if (delPagos) redirect("/servicios-varios?error=eliminar");

  const { error: delServ } = await supabase
    .from("servicios_extra")
    .delete()
    .eq("id", id)
    .eq("optica_id", opticaId);

  if (delServ) redirect("/servicios-varios?error=eliminar");

  revalidatePath("/servicios-varios");
  revalidatePath("/pacientes");
  if (row.paciente_id) {
    revalidatePath(`/pacientes/${row.paciente_id}/servicios-extra`);
  }

  redirect("/servicios-varios?msg=eliminado");
}
