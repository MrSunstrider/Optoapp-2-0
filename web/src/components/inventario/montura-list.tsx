import Link from "next/link";
import { soles, type MonturaItem } from "@/lib/inventario";
import { redirect } from "next/navigation";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

export function MonturaList({
  items,
  canWrite
}: {
  items: MonturaItem[];
  canWrite: boolean;
}) {
  if (items.length === 0) {
    return (
      <div className="py-16 text-center text-2xl text-zinc-400">
        No hay monturas para este criterio.
      </div>
    );
  }

  return (
    <ul className="space-y-3">
      {items.map((item) => {
        const critical = item.stockActual <= item.stockMinimo;
        return (
          <li key={item.id} className="rounded-xl border border-zinc-700 bg-[#1C1C20] p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-sm text-zinc-400">SKU: {item.sku}</p>
                <p className="text-xl font-semibold text-zinc-100">
                  {item.marca} {item.modelo}
                </p>
                {item.color ? <p className="text-sm text-zinc-300">Color: {item.color}</p> : null}
                <p className={critical ? "text-red-300" : "text-zinc-200"}>
                  Stock: {item.stockActual} (mínimo {item.stockMinimo})
                </p>
                <p className="text-sm text-zinc-300">
                  Costo: {soles(item.costoUnitario)} · Venta: {soles(item.precioVenta)}
                </p>
                <p className="text-xs text-zinc-500">
                  Estado: {item.activo ? "activo" : "inactivo"}
                </p>
              </div>
              <div className="flex flex-wrap gap-2 text-xs">
                <Link href={`/inventario/${item.id}/editar`} className="rounded border border-zinc-600 px-2 py-1 text-zinc-100">
                  Editar
                </Link>
                <form action={`/inventario/${item.id}/editar`} method="get">
                  <input type="hidden" name="action" value="stock" />
                  <button className="rounded border border-zinc-600 px-2 py-1 text-zinc-100">
                    Ajustar stock
                  </button>
                </form>
                <form action={toggleMonturaAction}>
                  <input type="hidden" name="id" value={item.id} />
                  <input type="hidden" name="active" value={item.activo ? "0" : "1"} />
                  <button
                    disabled={!canWrite}
                    className="rounded border border-zinc-600 px-2 py-1 text-zinc-100 disabled:opacity-50"
                  >
                    {item.activo ? "Inactivar" : "Reactivar"}
                  </button>
                </form>
              </div>
            </div>
          </li>
        );
      })}
    </ul>
  );
}

async function toggleMonturaAction(formData: FormData) {
  "use server";
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) {
    redirect("/seleccion-optica");
  }
  const ctx = activeOptica!;
  if (!canManagePacientes(ctx.rol)) redirect("/inventario");

  const id = String(formData.get("id") ?? "").trim();
  const active = String(formData.get("active") ?? "") === "1";
  if (!id) redirect("/inventario");
  const supabase = await createClient();
  await supabase
    .from("monturas")
    .update({ activo: active })
    .eq("id", id)
    .eq("optica_id", ctx.opticaId);

  redirect("/inventario?msg=actualizado");
}
