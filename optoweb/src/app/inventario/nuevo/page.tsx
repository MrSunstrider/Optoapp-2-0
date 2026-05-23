import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { MonturaForm } from "@/components/inventario/montura-form";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule, canManagePacientes } from "@/lib/roles";
import { createMonturaAction } from "../_actions/montura-crud";

export default async function NuevaMonturaPage() {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canAccessModule(activeOptica.rol, "inventario")) redirect("/dashboard");
  const canWrite = canManagePacientes(activeOptica.rol);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-3xl space-y-4">
          <h1 className="text-3xl font-semibold">Nueva montura</h1>
          <MonturaForm
            action={createMonturaAction}
            fields={[
              { name: "sku", label: "SKU" },
              { name: "marca", label: "Marca" },
              { name: "modelo", label: "Modelo" },
              { name: "stock", label: "Stock inicial", type: "number", min: 0 },
            ]}
            submitLabel="Guardar"
            disabled={!canWrite}
          />
        </div>
      </div>
    </AppShell>
  );
}
