import type { MonturaItem } from "@/lib/inventario";

export function StockAlertCard({ items }: { items: MonturaItem[] }) {
  const critical = items.filter((i) => i.stockActual <= i.stockMinimo).slice(0, 5);
  return (
    <section className="rounded-2xl bg-[#4A4856] p-4">
      <h2 className="text-3xl font-semibold text-zinc-100">
        Alertas de stock bajo: {critical.length}
      </h2>
      {critical.length === 0 ? (
        <p className="mt-2 text-2xl text-zinc-300">No hay monturas críticas por reposición.</p>
      ) : (
        <ul className="mt-3 space-y-2 text-zinc-100">
          {critical.map((c) => (
            <li key={c.id} className="rounded-lg border border-red-500/40 bg-red-950/20 p-2 text-xl">
              {c.sku} · {c.marca} {c.modelo} · stock {c.stockActual} / mínimo {c.stockMinimo}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
