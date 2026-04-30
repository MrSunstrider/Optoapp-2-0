import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule, canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

async function createMonturaAction(formData: FormData) {
  "use server";
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManagePacientes(activeOptica.rol)) redirect("/inventario");

  const sku = String(formData.get("sku") ?? "").trim();
  const marca = String(formData.get("marca") ?? "").trim();
  const modelo = String(formData.get("modelo") ?? "").trim();
  const stock = Number(String(formData.get("stock") ?? "0"));
  if (!sku || !marca || !modelo || !Number.isFinite(stock) || stock < 0) {
    redirect("/inventario/nuevo?error=validacion");
  }

  const supabase = await createClient();
  const payload = {
    id: crypto.randomUUID(),
    optica_id: activeOptica.opticaId,
    sku,
    marca,
    modelo,
    stock_actual: Math.floor(stock),
    activo: true
  };
  const { error } = await supabase.from("monturas").insert(payload);
  if (error) redirect("/inventario/nuevo?error=guardar");
  redirect("/inventario?msg=guardado");
}

export default async function NuevaMonturaPage({
  searchParams
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canAccessModule(activeOptica.rol, "inventario")) redirect("/dashboard");
  const canWrite = canManagePacientes(activeOptica.rol);
  const query = await searchParams;

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-3xl space-y-4">
          <h1 className="text-3xl font-semibold">Nueva montura</h1>
          {query.error ? (
            <p className="text-sm text-red-300">
              {query.error === "validacion"
                ? "Completa SKU, marca, modelo y stock válido."
                : "No se pudo guardar la montura."}
            </p>
          ) : null}
          <form action={createMonturaAction} className="space-y-3 rounded-2xl bg-[#4A4856] p-4">
            <Input name="sku" label="SKU" />
            <Input name="marca" label="Marca" />
            <Input name="modelo" label="Modelo" />
            <Input name="stock" label="Stock inicial" type="number" min={0} />
            <button
              disabled={!canWrite}
              className="rounded-full bg-[#8AB4F8] px-5 py-2 text-sm font-semibold text-zinc-900 disabled:opacity-50"
            >
              Guardar
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
  type = "text",
  min
}: {
  name: string;
  label: string;
  type?: string;
  min?: number;
}) {
  return (
    <label className="block">
      <span className="text-sm text-zinc-200">{label}</span>
      <input
        name={name}
        type={type}
        min={min}
        className="mt-1 w-full rounded-xl border border-zinc-600 bg-[#121214] px-3 py-2 text-zinc-100"
      />
    </label>
  );
}
