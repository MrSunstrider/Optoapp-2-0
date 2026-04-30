import { money } from "@/lib/cierre-caja";

export function TotalCard({ total }: { total: number }) {
  return (
    <section className="rounded-2xl bg-[#5B3FA1] p-5">
      <p className="text-lg font-semibold text-zinc-100">TOTAL RECAUDADO</p>
      <p className="mt-2 text-6xl font-bold tracking-tight text-zinc-100">
        {money(total)}
      </p>
    </section>
  );
}
