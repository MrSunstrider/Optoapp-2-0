import { AppShell } from "@/components/app-shell";

export default async function ModulePlaceholder({
  params
}: {
  params: Promise<{ module: string[] }>;
}) {
  const resolved = await params;
  const name = resolved.module?.[0] ?? "modulo";
  const enabled = new Set([
    "pacientes",
    "evaluaciones",
    "dispensaciones",
    "servicios",
    "agenda",
    "inventario",
    "configuracion",
    "reportes"
  ]);

  if (!enabled.has(name)) {
    return (
      <AppShell>
        <h1 className="text-2xl font-semibold">Ruta no disponible</h1>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <h1 className="text-2xl font-semibold capitalize mb-2">{name}</h1>
      <p className="text-sm text-slate-600">
        Placeholder inicial del modulo `{name}` para P4.
      </p>
    </AppShell>
  );
}
