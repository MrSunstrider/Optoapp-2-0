import { AppShell } from "@/components/app-shell";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { redirect } from "next/navigation";

export default async function DashboardPage() {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  return (
    <AppShell>
      <h1 className="text-2xl font-semibold mb-2">Dashboard</h1>
      <p className="text-sm text-slate-600 mb-4">
        Sesion activa: {user?.email ?? "sin email"}
      </p>
      <p className="text-sm text-slate-600 mb-4">
        Optica activa: {activeOptica.nombre} ({activeOptica.rol})
      </p>
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        <KpiCard title="Ventas del dia" value="TODO" />
        <KpiCard title="Ventas del mes" value="TODO" />
        <KpiCard title="Pacientes hoy" value="TODO" />
        <KpiCard title="Saldos pendientes" value="TODO" />
      </div>
      <p className="text-xs text-slate-500 mt-4">
        TODO: conectar KPIs a RPC/vistas con filtro por optica activa.
      </p>
    </AppShell>
  );
}

function KpiCard({ title, value }: { title: string; value: string }) {
  return (
    <div className="rounded border bg-white p-4 shadow-sm">
      <p className="text-sm text-slate-600">{title}</p>
      <p className="text-xl font-semibold mt-1">{value}</p>
    </div>
  );
}
