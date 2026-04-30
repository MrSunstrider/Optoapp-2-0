import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { ServiciosVariosList } from "@/components/servicios-varios/servicios-varios-list";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import type { ServicioVariosListRow } from "@/lib/servicios-extra-display";
import { canAccessModule, canManagePacientes } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

export default async function ServiciosVariosPage({
  searchParams
}: {
  searchParams: Promise<{ msg?: string; error?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canAccessModule(activeOptica.rol, "servicios-varios")) redirect("/dashboard");
  const canManage = canManagePacientes(activeOptica.rol);
  if (!canManage) redirect("/dashboard");

  const query = await searchParams;
  const supabase = await createClient();
  const [fiscal, { data }] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    supabase
      .from("servicios_extra")
      .select("id,fecha,ot,descripcion,monto_total,a_cuenta,estado,paciente_id")
      .eq("optica_id", activeOptica.opticaId)
      .order("fecha", { ascending: false })
  ]);

  const opticaLine = formatOpticaActivaLine(activeOptica.nombre, fiscal);
  const rows = (data ?? []) as ServicioVariosListRow[];

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-m-6 min-h-screen space-y-4 bg-[#121214] p-6 text-zinc-100">
        <p className="text-xs text-zinc-400">{opticaLine}</p>

        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight text-white">
            Servicios Varios
          </h1>
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
        {query.msg === "eliminado" && (
          <p className="rounded-lg border border-emerald-900/40 bg-emerald-950/30 px-3 py-2 text-sm text-emerald-300">
            Servicio eliminado correctamente.
          </p>
        )}
        {query.error === "eliminar" && (
          <p className="rounded-lg border border-red-900/40 bg-red-950/30 px-3 py-2 text-sm text-red-300" role="alert">
            No se pudo eliminar el servicio. Intente de nuevo.
          </p>
        )}

        <ServiciosVariosList rows={rows} canManage={canManage} />
      </div>
    </AppShell>
  );
}
