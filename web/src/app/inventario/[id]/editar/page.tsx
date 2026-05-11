import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule, canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

async function updateMonturaAction(formData: FormData) {
  "use server";
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/inventario");
  const id = String(formData.get("id") ?? "").trim();
  const sku = String(formData.get("sku") ?? "").trim();
  const marca = String(formData.get("marca") ?? "").trim();
  const modelo = String(formData.get("modelo") ?? "").trim();
  const stock = Number(String(formData.get("stock") ?? "0"));
  const motivo = String(formData.get("motivo") ?? "").trim();
  if (!id || !sku || !marca || !modelo || !Number.isFinite(stock) || stock < 0) {
    redirect(`/inventario/${id}/editar?error=validacion`);
  }

  const supabase = await createClient();
  const { data: prev } = await supabase
    .from("monturas")
    .select("stock_actual")
    .eq("id", id)
    .eq("optica_id", activeOptica.opticaId)
    .maybeSingle();
  const before = Number(prev?.stock_actual ?? 0);
  const after = Math.floor(stock);
  const { error } = await supabase
    .from("monturas")
    .update({ sku, marca, modelo, stock_actual: after })
    .eq("id", id)
    .eq("optica_id", activeOptica.opticaId);
  if (error) redirect(`/inventario/${id}/editar?error=guardar`);

  if (before !== after) {
    await supabase.from("montura_movimientos").insert({
      id: crypto.randomUUID(),
      montura_id: id,
      fecha: new Date().toISOString().slice(0, 10),
      tipo: "AJUSTE",
      cantidad: Math.abs(after - before),
      stock_previo: before,
      stock_nuevo: after,
      referencia_id: id,
      nota: motivo || "Ajuste manual inventario",
      optica_id: activeOptica.opticaId
    });
  }

  redirect("/inventario?msg=actualizado");
}

export default async function EditarMonturaPage({
  params,
  searchParams
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ error?: string; action?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canAccessModule(activeOptica.rol, "inventario")) redirect("/dashboard");
  const canWrite = canManagePacientes(activeOptica.rol);
  const { id } = await params;
  const query = await searchParams;

  const supabase = await createClient();
  const { data } = await supabase
    .from("monturas")
    .select("id,sku,marca,modelo,stock_actual")
    .eq("id", id)
    .eq("optica_id", activeOptica.opticaId)
    .maybeSingle();
  if (!data) redirect("/inventario");

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-3xl space-y-4">
          <h1 className="text-3xl font-semibold">
            {query.action === "stock" ? "Ajustar stock montura" : "Editar montura"}
          </h1>
          {query.error ? (
            <p className="text-sm text-red-300">
              {query.error === "validacion"
                ? "Datos inválidos para guardar."
                : "No se pudo actualizar la montura."}
            </p>
          ) : null}
          <form action={updateMonturaAction} className="space-y-3 rounded-2xl bg-[#4A4856] p-4">
            <input type="hidden" name="id" value={data.id} />
            <Input name="sku" label="SKU" defaultValue={data.sku ?? ""} />
            <Input name="marca" label="Marca" defaultValue={data.marca ?? ""} />
            <Input name="modelo" label="Modelo" defaultValue={data.modelo ?? ""} />
            <Input
              name="stock"
              label="Stock actual"
              type="number"
              min={0}
              defaultValue={String(data.stock_actual ?? 0)}
            />
            <Input
              name="motivo"
              label="Motivo ajuste stock"
              defaultValue=""
            />
            <button
              disabled={!canWrite}
              className="rounded-full bg-[#8AB4F8] px-5 py-2 text-sm font-semibold text-zinc-900 disabled:opacity-50"
            >
              Guardar cambios
            </button>
          </form>
        </div>
      </div>
    </AppShell>
  );
}

function Input({
  name,
  label,
  defaultValue,
  type = "text",
  min
}: {
  name: string;
  label: string;
  defaultValue: string;
  type?: string;
  min?: number;
}) {
  return (
    <label className="block">
      <span className="text-sm text-zinc-200">{label}</span>
      <input
        name={name}
        defaultValue={defaultValue}
        type={type}
        min={min}
        className="mt-1 w-full rounded-xl border border-zinc-600 bg-[#121214] px-3 py-2 text-zinc-100"
      />
    </label>
  );
}
