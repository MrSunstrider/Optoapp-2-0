import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchDispensacionesPorPaciente } from "@/lib/paciente-clinico";

function formatMoney(n: number | null): string {
  if (n === null || Number.isNaN(n)) return "—";
  return `S/ ${n.toFixed(2)}`;
}

export default async function PacienteDispensacionesPage({
  params,
  searchParams
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ msg?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  const { id } = await params;
  const query = await searchParams;
  const supabase = await createClient();
  const rows = await fetchDispensacionesPorPaciente(supabase, activeOptica.opticaId, id);

  return (
    <div>
      {query.msg === "creada" && (
        <p className="mb-3 text-sm text-emerald-400">Dispensación creada correctamente.</p>
      )}
      {query.msg === "actualizada" && (
        <p className="mb-3 text-sm text-emerald-400">Dispensación actualizada correctamente.</p>
      )}
      <h2 className="mb-2 text-lg font-semibold text-white">Dispensaciones</h2>
      <p className="mb-4 text-sm text-zinc-500">
        Órdenes de trabajo y montos asociados a este paciente.
      </p>
      {rows.length === 0 ? (
        <p className="text-sm text-zinc-500">No hay dispensaciones para este paciente.</p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-zinc-700">
          <table className="min-w-full text-sm text-zinc-200">
            <thead className="bg-zinc-900 text-left text-zinc-400">
              <tr>
                <th className="px-3 py-2 font-medium">Fecha</th>
                <th className="px-3 py-2 font-medium">OT</th>
                <th className="px-3 py-2 font-medium">Total</th>
                <th className="px-3 py-2 font-medium">Pagado</th>
                <th className="px-3 py-2 font-medium">Entrega</th>
                <th className="px-3 py-2 font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className="border-t">
                  <td className="px-3 py-2 whitespace-nowrap">{r.fecha ?? "—"}</td>
                  <td className="px-3 py-2">{r.ot?.trim() || "—"}</td>
                  <td className="px-3 py-2">{formatMoney(r.monto_total)}</td>
                  <td className="px-3 py-2">{formatMoney(r.monto_pagado)}</td>
                  <td className="px-3 py-2">{r.estado_entrega ?? "—"}</td>
                  <td className="px-3 py-2">
                    <a
                      href={`/pacientes/${id}/dispensaciones/${r.id}/editar`}
                      className="text-sky-400 hover:underline"
                    >
                      Editar
                    </a>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
