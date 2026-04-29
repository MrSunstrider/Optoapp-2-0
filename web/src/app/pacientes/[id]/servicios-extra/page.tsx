import { redirect } from "next/navigation";
import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchServiciosExtraPorPaciente } from "@/lib/paciente-clinico";

function formatMoney(n: number | null): string {
  if (n === null || Number.isNaN(n)) return "—";
  return `S/ ${n.toFixed(2)}`;
}

export default async function PacienteServiciosExtraPage({
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
  const rows = await fetchServiciosExtraPorPaciente(supabase, activeOptica.opticaId, id);

  return (
    <div>
      {query.msg === "creado" && (
        <p className="mb-3 rounded-lg border border-emerald-900/40 bg-emerald-950/30 px-3 py-2 text-sm text-emerald-300">
          Servicio creado correctamente.
        </p>
      )}
      {query.msg === "actualizado" && (
        <p className="mb-3 rounded-lg border border-emerald-900/40 bg-emerald-950/30 px-3 py-2 text-sm text-emerald-300">
          Servicio actualizado correctamente.
        </p>
      )}
      <h2 className="mb-2 text-lg font-semibold text-white">Servicios</h2>
      <p className="mb-4 text-sm text-zinc-500">
        Servicios adicionales vinculados al expediente (misma tabla que en la app).
      </p>
      {rows.length === 0 ? (
        <p className="text-sm text-zinc-500">No hay servicios extra para este paciente.</p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-zinc-700">
          <table className="min-w-full text-sm text-zinc-200">
            <thead className="bg-zinc-900 text-left text-zinc-400">
              <tr>
                <th className="px-3 py-2 font-medium">Fecha</th>
                <th className="px-3 py-2 font-medium">OT</th>
                <th className="px-3 py-2 font-medium">Descripción</th>
                <th className="px-3 py-2 font-medium">Total</th>
                <th className="px-3 py-2 font-medium">A cuenta</th>
                <th className="px-3 py-2 font-medium">Estado</th>
                <th className="px-3 py-2 font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className="border-t">
                  <td className="px-3 py-2 whitespace-nowrap">{r.fecha ?? "—"}</td>
                  <td className="px-3 py-2">{r.ot?.trim() || "—"}</td>
                  <td className="px-3 py-2 max-w-xs truncate">{r.descripcion ?? "—"}</td>
                  <td className="px-3 py-2">{formatMoney(r.monto_total)}</td>
                  <td className="px-3 py-2">{formatMoney(r.a_cuenta)}</td>
                  <td className="px-3 py-2">{r.estado ?? "—"}</td>
                  <td className="px-3 py-2">
                    <Link
                      href={`/pacientes/${id}/servicios-extra/${r.id}/editar`}
                      className="text-sky-300 underline"
                    >
                      Editar
                    </Link>
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
