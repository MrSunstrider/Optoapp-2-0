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
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-2xl font-bold text-foreground">Resumen de Inventario</h2>
      
      <div className="mt-6 grid grid-cols-2 gap-4">
        <div className="rounded-2xl bg-foreground/[0.03] p-4">
          <p className="text-xs font-bold uppercase tracking-tight text-muted-foreground/60">Monturas</p>
          <p className="font-heading text-2xl font-black text-foreground">{summary.listed}</p>
        </div>
        <div className="rounded-2xl bg-foreground/[0.03] p-4">
          <p className="text-xs font-bold uppercase tracking-tight text-muted-foreground/60">Stock Total</p>
          <p className="font-heading text-2xl font-black text-foreground">{summary.stockTotal}</p>
        </div>
        <div className="rounded-2xl bg-primary/5 p-4">
          <p className="text-xs font-bold uppercase tracking-tight text-primary/60">Valor Costo</p>
          <p className="font-heading text-xl font-black text-primary">{soles(summary.valorCosto)}</p>
        </div>
        <div className="rounded-2xl bg-primary/5 p-4">
          <p className="text-xs font-bold uppercase tracking-tight text-primary/60">Valor Venta</p>
          <p className="font-heading text-xl font-black text-primary">{soles(summary.valorVenta)}</p>
        </div>
      </div>

      <div className="mt-8 flex flex-wrap gap-3">
        <Link
          href={pdfHref}
          className="flex-1 flex items-center justify-center gap-2 rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
        >
          📄 Exportar PDF
        </Link>
        <Link
          href={pdfHref}
          className="flex items-center justify-center gap-2 rounded-xl border border-border bg-card px-6 py-3 text-sm font-bold text-foreground transition-all hover:bg-foreground/5 active:scale-95"
        >
          📤 Compartir
        </Link>
      </div>
    </section>
  );
}
