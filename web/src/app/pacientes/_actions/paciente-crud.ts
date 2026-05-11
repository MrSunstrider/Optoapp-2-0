"use server";

import { redirect } from "next/navigation";

import {
  suggestNextHoRPC,
  checkDuplicateHo
} from "@/lib/historia-optometrica";
import { getOpticaPacienteLimitInfo } from "@/lib/optica-limits";
import { fetchEliminacionesRestantesHoy } from "@/lib/paciente-delete-audit";
import { getActiveOpticaContext } from "@/lib/optica-context";
import {
  localTodayDateOnly
} from "@/lib/pacientes";
import { canDeletePaciente, canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

function emptyToNull(s: string): string | null {
  const t = s.trim();
  return t.length > 0 ? t : null;
}

function parsePacienteForm(formData: FormData) {
  const fechaCreacion = String(formData.get("fechaCreacion") ?? "").trim();
  const historiaOptometrica = String(
    formData.get("historiaOptometrica") ?? ""
  ).trim();
  const nombreCompleto = String(formData.get("nombreCompleto") ?? "").trim();
  const edad = Number(formData.get("edad") ?? 0);
  const telefono = String(formData.get("telefono") ?? "").trim();
  const dni = String(formData.get("dni") ?? "").trim();
  const fechaNacimiento = String(formData.get("fechaNacimiento") ?? "").trim();
  const sexo = String(formData.get("sexo") ?? "").trim();
  const email = String(formData.get("email") ?? "").trim();
  const direccion = String(formData.get("direccion") ?? "").trim();
  const distrito = String(formData.get("distrito") ?? "").trim();
  const ocupacion = String(formData.get("ocupacion") ?? "").trim();
  const acompanante = String(formData.get("acompanante") ?? "").trim();
  const hobbies = String(formData.get("hobbies") ?? "").trim();

  return {
    fechaCreacion: fechaCreacion || localTodayDateOnly(),
    historiaOptometrica: historiaOptometrica || null,
    nombreCompleto,
    edad,
    telefono,
    dni: emptyToNull(dni),
    fechaNacimiento: emptyToNull(fechaNacimiento),
    sexo: emptyToNull(sexo),
    email: emptyToNull(email),
    direccion: emptyToNull(direccion),
    distrito: emptyToNull(distrito),
    ocupacion: emptyToNull(ocupacion),
    acompanante: emptyToNull(acompanante),
    hobbies: emptyToNull(hobbies)
  };
}

export async function createPacienteAction(
  prevState: { error?: string } | null,
  formData: FormData
): Promise<{ error?: string } | null> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { error: "Sin óptica activa" };
  if (!canManagePacientes(activeOptica.rol)) return { error: "Sin permiso" };

  const p = parsePacienteForm(formData);
  if (!p.nombreCompleto || !p.telefono || Number.isNaN(p.edad) || p.edad <= 0) {
    return { error: "Completa nombre, teléfono y edad." };
  }

  const supabase = await createClient();

  const limit = await getOpticaPacienteLimitInfo(supabase, activeOptica.opticaId);
  if (!limit.puedeCrearMas) {
    return { error: "Límite de pacientes alcanzado para esta óptica." };
  }

  if (
    p.historiaOptometrica &&
    (await checkDuplicateHo(supabase, activeOptica.opticaId, p.historiaOptometrica))
  ) {
    return { error: "El número de historia optométrica ya existe." };
  }

  const newId = crypto.randomUUID();
  const { data, error } = await supabase
    .from("pacientes")
    .insert({
      id: newId,
      nombre_completo: p.nombreCompleto,
      edad: p.edad,
      telefono: p.telefono,
      fecha_creacion: p.fechaCreacion,
      historia_optometrica: p.historiaOptometrica,
      dni: p.dni,
      fecha_nacimiento: p.fechaNacimiento,
      sexo: p.sexo,
      email: p.email,
      direccion: p.direccion,
      distrito: p.distrito,
      ocupacion: p.ocupacion,
      acompanante: p.acompanante,
      hobbies: p.hobbies,
      optica_id: activeOptica.opticaId
    })
    .select("id")
    .maybeSingle();

  if (error?.code === "23505") {
    return { error: "Ya existe esa historia optométrica en esta óptica." };
  }
  if (error || !data?.id) {
    return { error: "No se pudo guardar. Intenta nuevamente." };
  }

  redirect(`/pacientes/${data.id}/evaluaciones?msg=creado`);
  return null;
}

export async function updatePacienteAction(
  prevState: { error?: string } | null,
  formData: FormData
): Promise<{ error?: string } | null> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { error: "Sin óptica activa" };
  if (!canManagePacientes(activeOptica.rol)) return { error: "Sin permiso" };

  const id = String(formData.get("id") ?? "").trim();
  if (!id) return { error: "Falta ID del paciente" };

  const p = parsePacienteForm(formData);
  if (!p.nombreCompleto || !p.telefono || Number.isNaN(p.edad) || p.edad <= 0) {
    return { error: "Revisa los campos obligatorios antes de guardar." };
  }

  const supabase = await createClient();
  if (
    p.historiaOptometrica &&
    (await checkDuplicateHo(supabase, activeOptica.opticaId, p.historiaOptometrica, id))
  ) {
    return { error: "Ya existe otra ficha con esa HO en esta óptica." };
  }

  const { data, error } = await supabase
    .from("pacientes")
    .update({
      nombre_completo: p.nombreCompleto,
      edad: p.edad,
      telefono: p.telefono,
      fecha_creacion: p.fechaCreacion,
      historia_optometrica: p.historiaOptometrica,
      dni: p.dni,
      fecha_nacimiento: p.fechaNacimiento,
      sexo: p.sexo,
      email: p.email,
      direccion: p.direccion,
      distrito: p.distrito,
      ocupacion: p.ocupacion,
      acompanante: p.acompanante,
      hobbies: p.hobbies
    })
    .eq("id", id)
    .eq("optica_id", activeOptica.opticaId)
    .select("id")
    .maybeSingle();

  if (error?.code === "23505") {
    return { error: "Ya existe otra ficha con esa HO en esta óptica." };
  }
  if (error || !data?.id) {
    return { error: "No se pudo guardar los cambios." };
  }

  redirect(`/pacientes/${id}/evaluaciones?msg=guardado`);
  return null;
}

export async function deletePacienteAction(
  prevState: { error?: string } | null,
  formData: FormData
): Promise<{ error?: string } | null> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { error: "Sin óptica activa" };
  if (!canDeletePaciente(activeOptica.rol)) return { error: "Sin permiso" };

  const id = String(formData.get("id") ?? "").trim();
  const confirm = String(formData.get("confirm") ?? "");
  if (!id || confirm !== "ELIMINAR") {
    return { error: "Debes escribir ELIMINAR para confirmar." };
  }

  const supabase = await createClient();
  const { data, error } = await supabase
    .from("pacientes")
    .delete()
    .eq("id", id)
    .eq("optica_id", activeOptica.opticaId)
    .select("id")
    .maybeSingle();

  if (error || !data?.id) {
    const msg =
      error?.message?.includes("Límite diario") ||
      error?.message?.includes("límite diario")
        ? "Límite diario de eliminaciones alcanzado."
        : "No se pudo eliminar el paciente.";
    return { error: msg };
  }

  const restantes = await fetchEliminacionesRestantesHoy(
    supabase,
    activeOptica.opticaId
  );
  const qs = new URLSearchParams({ msg: "eliminado" });
  if (restantes != null) qs.set("restantes", String(restantes));
  redirect(`/pacientes?${qs.toString()}`);
  return null;
}

export async function suggestHistoriaOptometricaAction(): Promise<{
  ok: boolean;
  value?: string;
  error?: string;
}> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { ok: false, error: "Sin óptica activa" };
  if (!canManagePacientes(activeOptica.rol))
    return { ok: false, error: "Sin permiso" };

  const supabase = await createClient();
  const result = await suggestNextHoRPC(supabase, activeOptica.opticaId);
  if (!result.ok) return { ok: false, error: result.error };
  return { ok: true, value: result.value };
}
