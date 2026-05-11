import { Banknote, Smartphone, CreditCard } from "lucide-react";
import { money } from "@/lib/cierre-caja";

export function PaymentSummaryCards({
  efectivo,
  movilTrans,
  tarjeta
}: {
  efectivo: number;
  movilTrans: number;
  tarjeta: number;
}) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <MiniCard 
        title="Efectivo" 
        value={money(efectivo)} 
        icon={<Banknote className="h-5 w-5" />}
        variant="emerald" 
      />
      <MiniCard 
        title="Móvil/Trans" 
        value={money(movilTrans)} 
        icon={<Smartphone className="h-5 w-5" />}
        variant="primary" 
      />
      <MiniCard 
        title="Tarjeta" 
        value={money(tarjeta)} 
        icon={<CreditCard className="h-5 w-5" />}
        variant="blue" 
      />
    </div>
  );
}

function MiniCard({
  title,
  value,
  icon,
  variant
}: {
  title: string;
  value: string;
  icon: React.ReactNode;
  variant: "emerald" | "primary" | "blue";
}) {
  const variants = {
    emerald: "text-emerald-500 bg-emerald-500/10 border-emerald-500/20",
    primary: "text-primary bg-primary/10 border-primary/20",
    blue: "text-blue-500 bg-blue-500/10 border-blue-500/20"
  };

  return (
    <div className={`rounded-3xl border p-6 transition-all hover:shadow-lg ${variants[variant]}`}>
      <div className="flex items-center gap-2 mb-4 opacity-70">
        {icon}
        <p className="text-[10px] font-black uppercase tracking-[0.2em]">{title}</p>
      </div>
      <p className="font-heading text-3xl font-black tabular-nums tracking-tighter">
        {value}
      </p>
    </div>
  );
}
