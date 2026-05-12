import Link from "next/link";
import { AppShell } from "@/components/app-shell";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { redirect } from "next/navigation";

export default async function PacienteNotFound() {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="flex flex-col items-center justify-center py-20 text-center space-y-4">
        <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center">
          <svg className="h-8 w-8 text-muted-foreground/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <circle cx="12" cy="12" r="10"/><path d="M16 16s-1.5-2-4-2-4 2-4 2M9 9h.01M15 9h.01"/>
          </svg>
        </div>
        <div>
          <h1 className="font-heading text-xl font-bold text-foreground">Paciente no encontrado</h1>
          <p className="text-sm text-muted-foreground mt-1">
            No existe un paciente con ese identificador en tu óptica activa.
          </p>
        </div>
        <Link
          href="/pacientes"
          className="inline-flex items-center gap-2 rounded-xl bg-primary/10 px-5 py-2.5 text-xs font-bold text-primary transition-all hover:bg-primary/20"
        >
          <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          Volver al listado
        </Link>
      </div>
    </AppShell>
  );
}
