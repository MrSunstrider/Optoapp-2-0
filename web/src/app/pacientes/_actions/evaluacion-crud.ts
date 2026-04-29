"use server";

import { redirect } from "next/navigation";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

export async function deleteEvaluacionAction(formData: FormData) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/pacientes");

  const pacienteId = String(formData.get("pacienteId") ?? "").trim();
  const evalId = String(formData.get("evaluacionId") ?? "").trim();
  if (!pacienteId || !evalId) {
    redirect("/pacientes");
  }

  const supabase = await createClient();
  const { error } = await supabase
    .from("evaluaciones")
    .delete()
    .eq("id", evalId)
    .eq("optica_id", activeOptica.opticaId)
    .eq("paciente_id", pacienteId);

  if (error) {
    redirect(`/pacientes/${pacienteId}/evaluaciones?error=eliminar_eval`);
  }

  redirect(`/pacientes/${pacienteId}/evaluaciones?msg=eval_eliminada`);
}
