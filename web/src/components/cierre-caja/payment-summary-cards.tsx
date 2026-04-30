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
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
      <MiniCard title="Efectivo" value={money(efectivo)} tone="emerald" />
      <MiniCard title="Móvil/Trans" value={money(movilTrans)} tone="cyan" />
      <MiniCard title="Tarjeta" value={money(tarjeta)} tone="sky" />
    </div>
  );
}

function MiniCard({
  title,
  value,
  tone
}: {
  title: string;
  value: string;
  tone: "emerald" | "cyan" | "sky";
}) {
  const toneClass =
    tone === "emerald"
      ? "text-emerald-300"
      : tone === "cyan"
        ? "text-cyan-300"
        : "text-sky-300";
  return (
    <div className="rounded-xl bg-[#1E1F2A] p-4">
      <p className={`text-sm ${toneClass}`}>{title}</p>
      <p className={`mt-1 text-3xl font-semibold ${toneClass}`}>{value}</p>
    </div>
  );
}
