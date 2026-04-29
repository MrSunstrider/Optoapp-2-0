import { AppShell } from "@/components/app-shell";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canAccessModule } from "@/lib/roles";
import { redirect } from "next/navigation";

export default async function ModulePlaceholder({
  params
}: {
  params: Promise<{ module: string[] }>;
}) {
  const resolved = await params;
  const name = resolved.module?.[0] ?? "modulo";
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  if (
    name === "evaluaciones" ||
    name === "dispensaciones" ||
    name === "servicios"
  ) {
    redirect("/pacientes");
  }

  const enabled = new Set([
    "pacientes",
    "agenda",
    "inventario",
    "configuracion",
    "reportes",
    "servicios-varios",
    "cierre-caja",
    "estadisticas",
    "sincronizar"
  ]);

  if (!enabled.has(name)) {
    return (
      <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
        <h1 className="text-2xl font-semibold">Ruta no disponible</h1>
      </AppShell>
    );
  }

  if (!canAccessModule(activeOptica.rol, name)) {
    return (
      <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
        <h1 className="text-2xl font-semibold mb-2">Acceso restringido</h1>
        <p className="text-sm text-slate-600">
          Tu rol actual no tiene permiso para entrar a `{name}`.
        </p>
      </AppShell>
    );
  }

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <h1 className="text-2xl font-semibold capitalize mb-2">{name}</h1>
      <p className="text-sm text-slate-600">
        Placeholder inicial del modulo `{name}` para P4.
      </p>
    </AppShell>
  );
}
