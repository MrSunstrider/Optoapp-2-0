import Link from "next/link";
import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule, canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

function formatMoney(n: number | null): string {
  if (n === null || Number.isNaN(n)) return "—";
  return `S/ ${n.toFixed(2)}`;
}

export default async function ServiciosVariosPage({
  searchParams
}: {
  searchParams: Promise<{ msg?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canAccessModule(activeOptica.rol, "servicios-varios")) redirect("/dashboard");
  if (!canManagePacientes(activeOptica.rol)) redirect("/dashboard");

  const query = await searchParams;
  const supabase = await createClient();
  const { data } = await supabase
    .from("servicios_extra")
    .select("id,fecha,ot,descripcion,monto_total,a_cuenta,estado,paciente_id")
    .eq("optica_id", activeOptica.opticaId)
    .order("fecha", { ascending: false });

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold">Servicios varios</h1>
          <Link href="/servicios-varios/nuevo" className="rounded-lg bg-sky-400 px-3 py-2 text-sm font-semibold text-zinc-900">
            Nuevo Servicio
          </Link>
        </div>
        {query.msg === "creado" && (
          <p className="rounded-lg border border-emerald-900/40 bg-emerald-950/30 px-3 py-2 text-sm text-emerald-300">
            Servicio creado correctamente.
          </p>
        )}
        {query.msg === "actualizado" && (
          <p className="rounded-lg border border-emerald-900/40 bg-emerald-950/30 px-3 py-2 text-sm text-emerald-300">
            Servicio actualizado correctamente.
          </p>
        )}
        <div className="overflow-x-auto rounded-lg border border-zinc-700">
          <table className="min-w-full text-sm">
            <thead className="bg-zinc-900 text-zinc-300">
              <tr>
                <th className="px-3 py-2 text-left font-medium">Fecha</th>
                <th className="px-3 py-2 text-left font-medium">OT</th>
                <th className="px-3 py-2 text-left font-medium">Descripcion</th>
                <th className="px-3 py-2 text-left font-medium">Total</th>
                <th className="px-3 py-2 text-left font-medium">A cuenta</th>
                <th className="px-3 py-2 text-left font-medium">Estado</th>
                <th className="px-3 py-2 text-left font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {(data ?? []).map((r) => (
                <tr key={r.id} className="border-t border-zinc-800">
                  <td className="px-3 py-2">{r.fecha ?? "—"}</td>
                  <td className="px-3 py-2">{r.ot?.trim() || "—"}</td>
                  <td className="px-3 py-2">{r.descripcion ?? "—"}</td>
                  <td className="px-3 py-2">{formatMoney(r.monto_total)}</td>
                  <td className="px-3 py-2">{formatMoney(r.a_cuenta)}</td>
                  <td className="px-3 py-2">{r.estado ?? "—"}</td>
                  <td className="px-3 py-2">
                    <Link href={`/servicios-varios/${r.id}/editar`} className="text-sky-300 underline">
                      Editar
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </AppShell>
  );
}
