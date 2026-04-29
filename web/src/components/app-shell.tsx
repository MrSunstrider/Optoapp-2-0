import { canAccessModule } from "@/lib/roles";

import { AppShellNav, type AppShellNavItem } from "./app-shell-nav";
import { IconLogout, LogoOptoArc } from "./nav-icons";

const modules: AppShellNavItem[] = [
  { href: "/pacientes", label: "Pacientes", iconKey: "pacientes" },
  {
    href: "/servicios-varios",
    label: "Servicios Varios",
    iconKey: "servicios-varios"
  },
  {
    href: "/dashboard",
    label: "Operación de Hoy",
    iconKey: "operacion-hoy"
  },
  { href: "/configuracion", label: "Configuración", iconKey: "configuracion" },
  { href: "/agenda", label: "Agenda", iconKey: "agenda" },
  {
    href: "/inventario",
    label: "Inventario Monturas",
    iconKey: "inventario"
  },
  { href: "/cierre-caja", label: "Cierre de Caja", iconKey: "cierre-caja" },
  {
    href: "/estadisticas",
    label: "Estadísticas (BI)",
    iconKey: "estadisticas"
  },
  { href: "/reportes", label: "Reportes", iconKey: "reportes" },
  {
    href: "/sincronizar",
    label: "Sincronizar Cloud",
    iconKey: "sincronizar"
  }
];

function moduleKeyFromHref(href: string): string {
  return href.replace(/^\//, "").split("/")[0] ?? "";
}

export function AppShell({
  children,
  role,
  opticaName
}: {
  children: React.ReactNode;
  role?: string;
  opticaName?: string;
}) {
  const r = role ?? "invitado";

  const visibleModules = modules.filter((item) => {
    const key = moduleKeyFromHref(item.href);
    if (key === "dashboard") return true;
    return canAccessModule(r, key);
  });

  return (
    <div className="grid min-h-screen grid-cols-1 md:grid-cols-[280px_1fr] bg-background">
      <aside className="flex min-h-[auto] flex-col border-r border-white/[0.08] bg-[#121214] text-white md:min-h-screen">
        <header className="shrink-0 border-b border-white/[0.08] px-4 pb-4 pt-6">
          <div className="flex flex-col items-center text-center">
            <LogoOptoArc className="mb-2" />
            <span className="text-base font-semibold tracking-tight text-white">
              OptoApp
            </span>
            <span className="mt-0.5 text-xs font-normal text-[#B0B0B0]">
              Sersa Visual & Preventiva
            </span>
          </div>
          {(opticaName || role) && (
            <div className="mt-4 border-t border-white/[0.08] pt-3 text-center text-[11px] leading-snug text-[#B0B0B0]">
              {opticaName && (
                <p className="truncate">
                  <span className="font-medium text-white/90">Óptica:</span>{" "}
                  {opticaName}
                </p>
              )}
              {role && (
                <p className="mt-1">
                  <span className="font-medium text-white/90">Rol:</span>{" "}
                  {role}
                </p>
              )}
            </div>
          )}
        </header>

        <AppShellNav items={visibleModules} />

        <div className="mt-auto shrink-0 border-t border-white/[0.08] p-3">
          <form action="/auth/logout" method="post">
            <button
              type="submit"
              className="flex w-full items-center justify-center gap-2 rounded-lg px-3 py-2.5 text-sm font-medium text-[#B0B0B0] transition-colors hover:bg-white/[0.06] hover:text-white"
            >
              <IconLogout className="shrink-0" />
              Cerrar Sesión
            </button>
          </form>
        </div>
      </aside>

      <main className="min-h-screen bg-background p-6">{children}</main>
    </div>
  );
}
