"use server";

import { redirect } from "next/navigation";

import {
  findDuplicateHistoriaPacienteId,
  suggestNextHistoriaOptometrica
} from "@/lib/historia-optometrica";
import { getOpticaPacienteLimitInfo } from "@/lib/optica-limits";
import { fetchEliminacionesRestantesHoy } from "@/lib/paciente-delete-audit";
import { getActiveOpticaContext } from "@/lib/optica-context";
import {
  fetchHistoriaRowsForOptica,
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

export async function createPacienteAction(formData: FormData) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/pacientes");

  const p = parsePacienteForm(formData);
  if (!p.nombreCompleto || !p.telefono || Number.isNaN(p.edad) || p.edad <= 0) {
    redirect("/pacientes/nuevo?error=validacion");
  }

  const supabase = await createClient();

  const limit = await getOpticaPacienteLimitInfo(supabase, activeOptica.opticaId);
  if (!limit.puedeCrearMas) {
    redirect("/pacientes/nuevo?error=limite");
  }

  const rows = await fetchHistoriaRowsForOptica(supabase, activeOptica.opticaId);
  if (
    p.historiaOptometrica &&
    findDuplicateHistoriaPacienteId(rows, p.historiaOptometrica, null)
  ) {
    redirect("/pacientes/nuevo?error=duplicado_ho");
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
    redirect("/pacientes/nuevo?error=duplicado_ho");
  }
  if (error || !data?.id) {
    redirect("/pacientes/nuevo?error=guardar");
  }

  redirect(`/pacientes/${data.id}/evaluaciones?msg=creado`);
}

export async function updatePacienteAction(formData: FormData) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/pacientes");

  const id = String(formData.get("id") ?? "").trim();
  if (!id) redirect("/pacientes");

  const p = parsePacienteForm(formData);
  if (!p.nombreCompleto || !p.telefono || Number.isNaN(p.edad) || p.edad <= 0) {
    redirect(`/pacientes/${id}/editar?error=validacion`);
  }

  const supabase = await createClient();
  const rows = await fetchHistoriaRowsForOptica(supabase, activeOptica.opticaId);
  if (
    p.historiaOptometrica &&
    findDuplicateHistoriaPacienteId(rows, p.historiaOptometrica, id)
  ) {
    redirect(`/pacientes/${id}/editar?error=duplicado_ho`);
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
    redirect(`/pacientes/${id}/editar?error=duplicado_ho`);
  }
  if (error || !data?.id) {
    redirect(`/pacientes/${id}/editar?error=guardar`);
  }

  redirect(`/pacientes/${id}/evaluaciones?msg=guardado`);
}

export async function deletePacienteAction(formData: FormData) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canDeletePaciente(activeOptica.rol)) redirect("/pacientes");

  const id = String(formData.get("id") ?? "").trim();
  const confirm = String(formData.get("confirm") ?? "");
  if (!id || confirm !== "ELIMINAR") {
    redirect(`/pacientes/${id}/evaluaciones?error=confirm`);
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
        ? "limite_borrado"
        : "eliminar";
    redirect(`/pacientes/${id}/evaluaciones?error=${msg}`);
  }

  const restantes = await fetchEliminacionesRestantesHoy(
    supabase,
    activeOptica.opticaId
  );
  const qs = new URLSearchParams({ msg: "eliminado" });
  if (restantes != null) qs.set("restantes", String(restantes));
  redirect(`/pacientes?${qs.toString()}`);
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
  const rows = await fetchHistoriaRowsForOptica(supabase, activeOptica.opticaId);
  const historias = rows.map((r) => r.historia_optometrica);
  return { ok: true, value: suggestNextHistoriaOptometrica(historias) };
}
