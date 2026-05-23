import type { CierreTx } from "@/lib/cierre-caja";
import { money } from "@/lib/cierre-caja";
import { ArrowUpCircle, Receipt, User, Hash } from "lucide-react";

export function TransactionsList({ rows }: { rows: CierreTx[] }) {
  if (rows.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center space-y-4">
        <div className="h-16 w-16 rounded-full bg-muted flex items-center justify-center text-muted-foreground/40">
          <Receipt className="h-8 w-8" />
        </div>
        <p className="text-sm font-medium text-muted-foreground max-w-[200px] leading-relaxed">
          No hay transacciones registradas para este periodo.
        </p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-3 p-2">
      {rows.map((row) => (
        <div 
          key={row.id} 
          className="group relative overflow-hidden rounded-2xl border border-border/40 bg-card p-5 transition-all hover:shadow-md hover:border-primary/20"
        >
          <div className="flex items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="h-12 w-12 rounded-xl bg-foreground/[0.03] flex items-center justify-center text-primary transition-colors group-hover:bg-primary/10">
                <ArrowUpCircle className="h-6 w-6" />
              </div>
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-black uppercase tracking-widest text-primary/60">{row.tipoOperacion}</span>
                  <span className="text-border/40">•</span>
                  <span className="text-[10px] font-bold text-muted-foreground uppercase">{row.medioPago}</span>
                </div>
                <h3 className="font-heading text-lg font-bold text-foreground">
                  {row.referencia || "Transacción Directa"}
                </h3>
              </div>
            </div>
            <div className="text-right">
              <p className="font-heading text-3xl font-black text-emerald-500 tabular-nums tracking-tighter">
                {money(row.monto)}
              </p>
              <div className="flex items-center justify-end gap-1.5 mt-0.5 text-muted-foreground/40">
                <Hash className="h-3 w-3" />
                <span className="text-[10px] font-black uppercase tracking-tighter">{row.id.split('-')[0]}</span>
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
