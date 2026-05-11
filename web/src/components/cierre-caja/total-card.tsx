import { money } from "@/lib/cierre-caja";
import { Sigma, Wallet2 } from "lucide-react";

export function TotalCard({ total }: { total: number }) {
  return (
    <section className="relative overflow-hidden rounded-[2.5rem] bg-primary p-8 text-primary-foreground shadow-2xl shadow-primary/20">
      <div className="relative z-10">
        <div className="flex items-center gap-2 opacity-80">
          <Wallet2 className="h-4 w-4" />
          <p className="text-[10px] font-black uppercase tracking-[0.2em]">TOTAL RECAUDADO</p>
        </div>
        <p className="mt-4 font-heading text-6xl font-black tracking-tighter tabular-nums">
          {money(total)}
        </p>
      </div>
      
      {/* Decorative background element */}
      <div className="absolute -bottom-6 -right-6 h-32 w-32 rotate-12 opacity-10">
        <Sigma className="h-full w-full" />
      </div>
    </section>
  );
}
