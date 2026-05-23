import { notFound, redirect } from "next/navigation";

import { saveServicioAction } from "@/app/pacientes/_actions/servicio-crud";
import { AppShell } from "@/components/app-shell";
import { ServicioForm } from "@/components/pacientes/servicio-form";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { canAccessModule, canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";
import { today } from "@/lib/date-utils";

function todayDateOnly(): string { return today(); }

export default async function EditarServicioGeneralPage({
  params,
  searchParams
}: {
  params: Promise<{ servicioId: string }>;
  searchParams: Promise<{ error?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canAccessModule(activeOptica.rol, "servicios-varios")) redirect("/dashboard");
  if (!canManagePacientes(activeOptica.rol)) redirect("/dashboard");

  const { servicioId } = await params;
  const query = await searchParams;
  const supabase = await createClient();
  const [fiscal, pacientesResp, servicioResp, pagosResp] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    supabase
      .from("pacientes")
      .select("id,nombre_completo")
      .eq("optica_id", activeOptica.opticaId)
      .order("nombre_completo", { ascending: true }),
    supabase
      .from("servicios_extra")
      .select("id,ot,descripcion,monto_total,estado,fecha,paciente_id")
      .eq("id", servicioId)
      .eq("optica_id", activeOptica.opticaId)
      .maybeSingle(),
    supabase
      .from("pagos")
      .select("id,fecha,monto,metodo_pago,nota")
      .eq("servicio_extra_id", servicioId)
      .eq("optica_id", activeOptica.opticaId)
      .order("fecha", { ascending: false })
  ]);
  if (!servicioResp.data) notFound();
  const s = servicioResp.data;

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
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="max-w-3xl">
        {query.error && errorMap[query.error] && (
          <p className="mb-3 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
            {errorMap[query.error]}
          </p>
        )}
        <ServicioForm
          mode="edit"
          servicioId={servicioId}
          backHref="/servicios-varios"
          returnTo="/servicios-varios"
          errorBase={`/servicios-varios/${servicioId}/editar`}
          opticaLine={opticaLine}
          todayDate={today}
          pacientes={(pacientesResp.data ?? []) as { id: string; nombre_completo: string | null }[]}
          saveAction={saveServicioAction}
          initial={{
            ot: String(s.ot ?? ""),
            descripcion: String(s.descripcion ?? ""),
            montoTotal: String(s.monto_total ?? ""),
            estado: String(s.estado ?? "Pendiente"),
            fecha: String(s.fecha ?? today),
            pacienteId: String(s.paciente_id ?? ""),
            pagos: (pagosResp.data ?? []).map((p) => ({
              id: String(p.id),
              fecha: String(p.fecha ?? today),
              monto: Number(p.monto ?? 0).toFixed(2),
              metodoPago: String(p.metodo_pago ?? "Efectivo"),
              nota: String(p.nota ?? "")
            }))
          }}
        />
      </div>
    </AppShell>
  );
}
