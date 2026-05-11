"use server";

import { redirect } from "next/navigation";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { canDeletePaciente } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

export async function deleteEvaluacionAction(
  prevState: { error?: string } | null,
  formData: FormData
): Promise<{ error?: string } | null> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { error: "Sin óptica activa" };
  if (!canDeletePaciente(activeOptica.rol)) return { error: "Solo admin o gerente pueden eliminar evaluaciones." };

  const pacienteId = String(formData.get("pacienteId") ?? "").trim();
  const evalId = String(formData.get("evaluacionId") ?? "").trim();
  if (!pacienteId || !evalId) return { error: "Faltan datos de la evaluación" };

  const supabase = await createClient();
  const { error } = await supabase
    .from("evaluaciones")
    .delete()
    .eq("id", evalId)
    .eq("optica_id", activeOptica.opticaId)
    .eq("paciente_id", pacienteId);

  if (error) {
    return { error: "No se pudo eliminar la evaluación. Intenta de nuevo." };
  }

  redirect(`/pacientes/${pacienteId}/evaluaciones?msg=eval_eliminada`);
  return null; // unreachable, but satisfies TS
}
