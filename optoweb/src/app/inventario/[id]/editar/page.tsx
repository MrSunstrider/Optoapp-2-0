import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { MonturaForm } from "@/components/inventario/montura-form";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule, canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";
import { updateMonturaAction } from "../../_actions/montura-crud";

export default async function EditarMonturaPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ action?: string }>;
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
          <MonturaForm
            action={updateMonturaAction}
            fields={[
              { name: "sku", label: "SKU", defaultValue: data.sku ?? "" },
              { name: "marca", label: "Marca", defaultValue: data.marca ?? "" },
              { name: "modelo", label: "Modelo", defaultValue: data.modelo ?? "" },
              { name: "stock", label: "Stock actual", type: "number", min: 0, defaultValue: String(data.stock_actual ?? 0) },
              { name: "motivo", label: "Motivo ajuste stock", defaultValue: "" },
            ]}
            submitLabel="Guardar cambios"
            disabled={!canWrite}
            hiddenInputs={[{ name: "id", value: data.id }]}
          />
        </div>
      </div>
    </AppShell>
  );
}
