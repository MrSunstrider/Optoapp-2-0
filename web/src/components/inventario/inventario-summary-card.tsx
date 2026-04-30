import Link from "next/link";
import { soles, type InventarioSummary } from "@/lib/inventario";

export function InventarioSummaryCard({
  summary,
  q
}: {
  summary: InventarioSummary;
  q: string;
}) {
  const pdfHref = q.trim()
    ? `/api/inventario/pdf?q=${encodeURIComponent(q.trim())}`
    : "/api/inventario/pdf";
  return (
    <section className="rounded-2xl bg-[#4A4856] p-4">
      <h2 className="text-3xl font-semibold text-zinc-100">Resumen inventario</h2>
      <div className="mt-3 space-y-1 text-2xl text-zinc-200">
        <p>Monturas listadas: {summary.listed}</p>
        <p>Stock total: {summary.stockTotal}</p>
        <p>Valor costo: {soles(summary.valorCosto)}</p>
        <p>Valor venta: {soles(summary.valorVenta)}</p>
      </div>
      <div className="mt-4 flex flex-wrap gap-2">
        <Link
          href={pdfHref}
          className="rounded-full bg-[#8AB4F8] px-6 py-2 text-lg font-semibold text-zinc-900"
        >
          Generar PDF
        </Link>
        <Link
          href={pdfHref}
          className="rounded-full border border-zinc-400 px-6 py-2 text-lg text-zinc-100"
        >
          Compartir PDF
        </Link>
      </div>
    </section>
  );
}
