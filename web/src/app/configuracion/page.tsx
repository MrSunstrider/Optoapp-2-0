import { redirect } from "next/navigation";
import { AppShell } from "@/components/app-shell";
import { ModuleCard } from "@/components/config/module-card";
import { CONFIG_MODULES } from "@/lib/config/modules";
import { canAccessConfigModule } from "@/lib/config/permissions";
import { getActiveOpticaContext } from "@/lib/optica-context";

export default async function ConfiguracionPage() {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-5xl space-y-4">
          <h1 className="text-2xl font-semibold tracking-tight text-white">Configuración</h1>
          <p className="text-sm text-zinc-400">
            Selecciona un módulo para administrar seguridad, operación y datos de la
            óptica.
          </p>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {CONFIG_MODULES.map((item) => (
              <ModuleCard
                key={item.key}
                href={`/configuracion/${item.key}`}
                title={item.title}
                description={item.description}
                blocked={!canAccessConfigModule(activeOptica.rol, item.key)}
              />
            ))}
          </div>
        </div>
      </div>
    </AppShell>
  );
}
