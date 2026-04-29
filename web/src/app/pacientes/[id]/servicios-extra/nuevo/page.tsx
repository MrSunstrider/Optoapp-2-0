import { redirect } from "next/navigation";

import { saveServicioAction } from "@/app/pacientes/_actions/servicio-crud";
import { ServicioForm } from "@/components/pacientes/servicio-form";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

function todayDateOnly(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate()
  ).padStart(2, "0")}`;
}

export default async function NuevoServicioExtraPlaceholder({
  params,
  searchParams
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ error?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/pacientes");

  const { id } = await params;
  const query = await searchParams;
  const supabase = await createClient();
  const [fiscal, pacientesResp] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    supabase
      .from("pacientes")
      .select("id,nombre_completo")
      .eq("optica_id", activeOptica.opticaId)
      .order("nombre_completo", { ascending: true })
  ]);
  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);
  const today = todayDateOnly();
  const errorMap: Record<string, string> = {
    descripcion_monto_requeridos: "Completa la descripción y el monto total para guardar.",
    monto_no_numerico: "El monto total no es un número válido.",
    total_mayor_0: "El monto total debe ser mayor a 0.",
    pago_mayor_0: "El pago debe ser mayor a 0.",
    pago_mayor_total: "El pago no puede ser mayor al total.",
    fk_paciente: "No se pudo guardar: revisa el paciente asociado o deja el servicio sin paciente.",
    guardar: "No se pudo guardar el servicio."
  };

  return (
    <div className="max-w-3xl">
      {query.error && errorMap[query.error] && (
        <p className="mb-3 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {errorMap[query.error]}
        </p>
      )}
      <ServicioForm
        mode="create"
        backHref={`/pacientes/${id}/servicios-extra`}
        returnTo={`/pacientes/${id}/servicios-extra`}
        errorBase={`/pacientes/${id}/servicios-extra/nuevo`}
        opticaLine={opticaLine}
        todayDate={today}
        pacientes={(pacientesResp.data ?? []) as { id: string; nombre_completo: string | null }[]}
        saveAction={saveServicioAction}
        initial={{
          ot: "",
          descripcion: "",
          montoTotal: "",
          estado: "Pendiente",
          fecha: today,
          pacienteId: id,
          pagos: []
        }}
      />
    </div>
  );
}
