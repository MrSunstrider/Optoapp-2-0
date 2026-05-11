import Link from "next/link";
import { AppShell } from "@/components/app-shell";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { redirect } from "next/navigation";

export default async function PacienteNotFound() {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <h1 className="text-xl font-semibold mb-2">Paciente no encontrado</h1>
      <p className="text-sm text-slate-600 mb-4">
        No existe un paciente con ese identificador en tu óptica activa.
      </p>
      <Link className="text-sm text-slate-700 underline" href="/pacientes">
        Volver al listado
      </Link>
    </AppShell>
  );
}
