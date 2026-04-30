import type { CierreTx } from "@/lib/cierre-caja";
import { money } from "@/lib/cierre-caja";

export function TransactionsList({ rows }: { rows: CierreTx[] }) {
  if (rows.length === 0) {
    return (
      <p className="py-10 text-center text-lg text-zinc-400">
        No hay transacciones para la fecha seleccionada.
      </p>
    );
  }

  return (
    <ul className="space-y-3">
      {rows.map((row) => (
        <li key={row.id} className="rounded-xl bg-[#1E1F2A] p-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm text-zinc-400">{row.tipoOperacion}</p>
              <p className="text-2xl font-semibold text-zinc-100">{row.medioPago}</p>
              <p className="text-xs text-zinc-500">{row.referencia}</p>
            </div>
            <p className="text-4xl font-semibold text-emerald-300">{money(row.monto)}</p>
          </div>
        </li>
      ))}
    </ul>
  );
}
