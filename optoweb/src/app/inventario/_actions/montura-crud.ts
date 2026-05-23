"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { z } from "zod";

import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

const monturaSchema = z.object({
  sku: z.string().min(1, "SKU requerido"),
  marca: z.string().min(1, "Marca requerida"),
  modelo: z.string().min(1, "Modelo requerido"),
  stock: z.coerce.number().int().min(0, "Stock debe ser >= 0"),
});

export async function createMonturaAction(
  prevState: { error?: string } | null,
  formData: FormData
): Promise<{ error?: string } | null> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { error: "Sin óptica activa" };
  if (!canManagePacientes(activeOptica.rol)) return { error: "Sin permiso" };

  const raw = {
    sku: String(formData.get("sku") ?? "").trim(),
    marca: String(formData.get("marca") ?? "").trim(),
    modelo: String(formData.get("modelo") ?? "").trim(),
    stock: formData.get("stock"),
  };

  const parsed = monturaSchema.safeParse(raw);
  if (!parsed.success) {
    const first = parsed.error.issues[0]?.message ?? "Completa SKU, marca, modelo y stock válido.";
    return { error: first };
  }

  const { sku, marca, modelo, stock } = parsed.data;

  const supabase = await createClient();
  const payload = {
    id: crypto.randomUUID(),
    optica_id: activeOptica.opticaId,
    sku,
    marca,
    modelo,
    stock_actual: stock,
    activo: true,
  };

  const { error } = await supabase.from("monturas").insert(payload);
  if (error) return { error: "No se pudo guardar la montura." };

  revalidatePath("/inventario");
  revalidatePath("/dashboard");
  redirect("/inventario?msg=guardado");
}

export async function updateMonturaAction(
  prevState: { error?: string } | null,
  formData: FormData
): Promise<{ error?: string } | null> {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) return { error: "Sin óptica activa" };
  if (!canManagePacientes(activeOptica.rol)) return { error: "Sin permiso" };

  const id = String(formData.get("id") ?? "").trim();
  const motivo = String(formData.get("motivo") ?? "").trim();

  if (!id) return { error: "Datos inválidos para guardar." };

  const raw = {
    sku: String(formData.get("sku") ?? "").trim(),
    marca: String(formData.get("marca") ?? "").trim(),
    modelo: String(formData.get("modelo") ?? "").trim(),
    stock: formData.get("stock"),
  };

  const parsed = monturaSchema.safeParse(raw);
  if (!parsed.success) {
    const first = parsed.error.issues[0]?.message ?? "Datos inválidos para guardar.";
    return { error: first };
  }

  const { sku, marca, modelo, stock } = parsed.data;

  const supabase = await createClient();

  const { data: prev } = await supabase
    .from("monturas")
    .select("stock_actual")
    .eq("id", id)
    .eq("optica_id", activeOptica.opticaId)
    .maybeSingle();

  const before = Number(prev?.stock_actual ?? 0);
  const after = stock;

  const { error } = await supabase
    .from("monturas")
    .update({ sku, marca, modelo, stock_actual: after })
    .eq("id", id)
    .eq("optica_id", activeOptica.opticaId);

  if (error) return { error: "No se pudo actualizar la montura." };

  if (before !== after) {
    const { error: movErr } = await supabase.from("montura_movimientos").insert({
      id: crypto.randomUUID(),
      montura_id: id,
      fecha: new Date().toISOString().slice(0, 10),
      tipo: "AJUSTE",
      cantidad: Math.abs(after - before),
      stock_previo: before,
      stock_nuevo: after,
      referencia_id: id,
      nota: motivo || "Ajuste manual inventario",
      optica_id: activeOptica.opticaId,
    });
    if (movErr) return { error: "No se pudo actualizar la montura." };
  }

  revalidatePath("/inventario");
  revalidatePath("/dashboard");
  redirect("/inventario?msg=actualizado");
}
