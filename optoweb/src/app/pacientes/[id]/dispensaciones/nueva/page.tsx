import { redirect } from "next/navigation";

import { saveDispensacionAction } from "@/app/pacientes/_actions/dispensacion-crud";
import { DispensacionForm } from "@/components/pacientes/dispensacion-form";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";
import { today } from "@/lib/date-utils";

function todayDateOnly(): string { return today(); }

export default async function NuevaDispensacionPage({
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
  const [fiscal, monturasResp] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    supabase
      .from("monturas")
      .select("id,sku,marca,modelo,stock_actual,activo")
      .eq("optica_id", activeOptica.opticaId)
      .order("marca", { ascending: true })
  ]);
  const monturas = (monturasResp.data ?? []) as {
    id: string;
    sku: string | null;
    marca: string | null;
    modelo: string | null;
    stock_actual: number | null;
    activo: boolean | null;
  }[];
  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);
  const today = todayDateOnly();

  const errorMap: Record<string, string> = {
    altura: "La altura es obligatoria para el tipo de lente seleccionado.",
    ot: "La OT es obligatoria para guardar la dispensación.",
    total_mayor_0: "El monto total debe ser mayor a 0.",
    pago_mayor_0: "El pago debe ser mayor a 0.",
    pago_mayor_total: "El pago no puede ser mayor al total.",
    ot_duplicada:
      'Ya existe una dispensación con esta OT en esta óptica. Usa otra OT o "Sugerir OT".',
    stock: "Stock insuficiente para la montura seleccionada.",
    guardar: "No se pudo guardar la dispensación."
  };

  return (
    <div className="max-w-3xl">
      {query.error && errorMap[query.error] && (
        <p className="mb-3 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {errorMap[query.error]}
        </p>
      )}
      <DispensacionForm
        pacienteId={id}
        mode="create"
        backHref={`/pacientes/${id}/dispensaciones`}
        opticaLine={opticaLine}
        todayDate={today}
        monturas={monturas}
        saveAction={saveDispensacionAction}
        initial={{
          fecha: today,
          ot: "",
          tipoLente: "",
          subTipoBifocal: "",
          distanciaLente: "",
          altura: "",
          materialLente: "",
          tratamientos: [],
          colorLente: "",
          notasDiseno: "",
          origenMontura: "Tienda",
          monturaId: "",
          previousMonturaId: "",
          tipoAro: "",
          materialMontura: "",
          descripcionMontura: "",
          montoTotal: "",
          estadoEntrega: "Pendiente",
          pagos: []
        }}
      />
    </div>
  );
}
