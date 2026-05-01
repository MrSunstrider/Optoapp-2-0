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
    <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-1">
      {items.map((item) => {
        const critical = item.stockActual <= item.stockMinimo;
        return (
          <li key={item.id} className="group rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:shadow-md">
            <div className="flex flex-wrap items-start justify-between gap-6">
              <div className="flex-1 space-y-1">
                <div className="flex items-center gap-2">
                  <span className="rounded-lg bg-foreground/[0.05] px-2 py-1 text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">
                    SKU: {item.sku}
                  </span>
                  {critical && (
                    <span className="rounded-lg bg-destructive/10 px-2 py-1 text-[10px] font-black uppercase tracking-widest text-destructive animate-pulse">
                      ⚠️ Stock Crítico
                    </span>
                  )}
                </div>
                <p className="font-heading text-xl font-bold text-foreground">
                  {item.marca} {item.modelo}
                </p>
                {item.color ? <p className="text-sm font-medium text-muted-foreground/80">🎨 Color: {item.color}</p> : null}
                
                <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
                  <div className="rounded-xl bg-foreground/[0.03] p-3 text-center">
                    <p className="text-[10px] font-bold uppercase tracking-tight text-muted-foreground/60">Stock Actual</p>
                    <p className={`font-heading text-lg font-black ${critical ? "text-destructive" : "text-foreground"}`}>
                      {item.stockActual}
                    </p>
                  </div>
                  <div className="rounded-xl bg-foreground/[0.03] p-3 text-center">
                    <p className="text-[10px] font-bold uppercase tracking-tight text-muted-foreground/60">Precio Venta</p>
                    <p className="font-heading text-lg font-black text-primary">
                      {soles(item.precioVenta)}
                    </p>
                  </div>
                  <div className="hidden rounded-xl bg-foreground/[0.03] p-3 text-center sm:block">
                    <p className="text-[10px] font-bold uppercase tracking-tight text-muted-foreground/60">Estado</p>
                    <p className={`text-xs font-bold uppercase ${item.activo ? "text-emerald-500" : "text-muted-foreground/40"}`}>
                      {item.activo ? "Activo" : "Inactivo"}
                    </p>
                  </div>
                </div>
              </div>

              <div className="flex shrink-0 flex-wrap gap-2">
                <Link 
                  href={`/inventario/${item.id}/editar`} 
                  className="flex h-10 w-10 items-center justify-center rounded-xl border border-border bg-card text-foreground transition-all hover:bg-foreground/5"
                  title="Editar montura"
                >
                  ✏️
                </Link>
                <form action={`/inventario/${item.id}/editar`} method="get">
                  <input type="hidden" name="action" value="stock" />
                  <button 
                    className="flex h-10 w-10 items-center justify-center rounded-xl border border-border bg-card text-foreground transition-all hover:bg-foreground/5"
                    title="Ajustar stock"
                  >
                    📦
                  </button>
                </form>
                <form action={toggleMonturaAction}>
                  <input type="hidden" name="id" value={item.id} />
                  <input type="hidden" name="active" value={item.activo ? "0" : "1"} />
                  <button
                    disabled={!canWrite}
                    className="flex h-10 w-10 items-center justify-center rounded-xl border border-border bg-card text-foreground transition-all hover:bg-foreground/5 disabled:opacity-50"
                    title={item.activo ? "Desactivar" : "Activar"}
                  >
                    {item.activo ? "🚫" : "✅"}
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
