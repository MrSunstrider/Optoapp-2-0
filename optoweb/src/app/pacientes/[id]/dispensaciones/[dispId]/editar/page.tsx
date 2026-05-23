import { notFound, redirect } from "next/navigation";

import { saveDispensacionAction } from "@/app/pacientes/_actions/dispensacion-crud";
import { DispensacionForm } from "@/components/pacientes/dispensacion-form";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";
import { today } from "@/lib/date-utils";
import type { ItemDraft } from "@/lib/dispensacion-types";

function todayDateOnly(): string { return today(); }

export default async function EditarDispensacionPage({
  params,
  searchParams
}: {
  params: Promise<{ id: string; dispId: string }>;
  searchParams: Promise<{ error?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/pacientes");

  const { id: pacienteId, dispId } = await params;
  const query = await searchParams;
  const supabase = await createClient();
  const [fiscal, monturasResp, dispResp, pagosResp, itemsResp] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    supabase
      .from("monturas")
      .select("id,sku,marca,modelo,stock_actual,activo")
      .eq("optica_id", activeOptica.opticaId)
      .order("marca", { ascending: true }),
    supabase
      .from("dispensaciones")
      .select("*")
      .eq("id", dispId)
      .eq("optica_id", activeOptica.opticaId)
      .eq("paciente_id", pacienteId)
      .maybeSingle(),
    supabase
      .from("pagos")
      .select("id,fecha,monto,metodo_pago,nota")
      .eq("dispensacion_id", dispId)
      .eq("optica_id", activeOptica.opticaId)
      .order("fecha", { ascending: false }),
    supabase
      .from("dispensacion_items")
      .select("*")
      .eq("dispensacion_id", dispId)
      .eq("optica_id", activeOptica.opticaId)
  ]);
  if (!dispResp.data) notFound();

  const d = dispResp.data;
  const monturas = (monturasResp.data ?? []) as {
    id: string;
    sku: string | null;
    marca: string | null;
    modelo: string | null;
    stock_actual: number | null;
    activo: boolean | null;
  }[];
  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);

  // Build items from dispensacion_items (with fallback to legacy entity fields)
  const rawItems = (itemsResp.data ?? []) as Record<string, unknown>[];
  const items: ItemDraft[] = rawItems.length > 0
    ? rawItems.map((r) => ({
        id: String(r.id),
        tipoLente: String(r.tipo_lente ?? ""),
        subTipoBifocal: String(r.sub_tipo_bifocal ?? ""),
        distanciaLente: String(r.distancia_lente ?? ""),
        altura: String(r.altura ?? ""),
        materialLente: String(r.material_lente ?? ""),
        tratamientos: Array.isArray(r.tratamientos)
          ? (r.tratamientos as string[])
          : typeof r.tratamientos === "string" && r.tratamientos
            ? String(r.tratamientos).split(",").filter(Boolean)
            : [],
        colorLente: String(r.color_lente ?? ""),
        notasDiseno: String(r.notas_diseno ?? ""),
        origenMontura: String(r.origen_montura ?? ""),
        monturaId: String(r.montura_id ?? ""),
        tipoAro: String(r.tipo_aro ?? ""),
        materialMontura: String(r.material_montura ?? ""),
        descripcionMontura: String(r.descripcion_montura ?? ""),
      }))
    : [{
        id: crypto.randomUUID(),
        tipoLente: String(d.tipo_lente ?? ""),
        subTipoBifocal: String(d.sub_tipo_bifocal ?? ""),
        distanciaLente: String(d.distancia_lente ?? ""),
        altura: String(d.altura ?? ""),
        materialLente: String(d.material_lente ?? ""),
        tratamientos: Array.isArray(d.tratamientos) ? (d.tratamientos as string[]) : [],
        colorLente: String(d.color_lente ?? ""),
        notasDiseno: String(d.notas_diseno ?? ""),
        origenMontura: String(d.origen_montura ?? "Tienda"),
        monturaId: String(d.montura_id ?? ""),
        tipoAro: String(d.tipo_aro ?? ""),
        materialMontura: String(d.material_montura ?? ""),
        descripcionMontura: String(d.descripcion_montura ?? ""),
      }];

  const previousItemIds = items.map((i) => i.id);

  const errorMap: Record<string, string> = {
    altura: "La altura es obligatoria para el tipo de lente seleccionado.",
    ot: "La OT es obligatoria para guardar la dispensación.",
    total_mayor_0: "El monto total debe ser mayor a 0.",
    pago_mayor_0: "El pago debe ser mayor a 0.",
    pago_mayor_total: "El pago no puede ser mayor al total.",
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
        pacienteId={pacienteId}
        dispensacionId={dispId}
        mode="edit"
        backHref={`/pacientes/${pacienteId}/dispensaciones`}
        opticaLine={opticaLine}
        todayDate={todayDateOnly()}
        monturas={monturas}
        saveAction={saveDispensacionAction}
        initial={{
          fecha: String(d.fecha ?? todayDateOnly()),
          ot: String(d.ot ?? ""),
          items,
          previousItemIds,
          montoTotal: String(d.monto_total ?? ""),
          estadoEntrega: String(d.estado_entrega ?? "Pendiente"),
          pagos: (pagosResp.data ?? []).map((p) => ({
            id: String(p.id),
            fecha: String(p.fecha ?? todayDateOnly()),
            monto: Number(p.monto ?? 0).toFixed(2),
            metodoPago: String(p.metodo_pago ?? "Efectivo"),
            nota: String(p.nota ?? "")
          }))
        }}
      />
    </div>
  );
}
