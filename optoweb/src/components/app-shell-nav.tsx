"use client";

import type { ComponentType, SVGProps } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { LogOut } from "lucide-react";

import {
  IconAgenda,
  IconCierreCaja,
  IconConfiguracion,
  IconEstadisticas,
  IconInventario,
  IconOperacionHoy,
  IconPacientes,
  IconReportes,
  IconServiciosVarios,
  IconSincronizar
} from "./nav-icons";

export type NavIconKey =
  | "pacientes"
  | "servicios-varios"
  | "operacion-hoy"
  | "configuracion"
  | "agenda"
  | "inventario"
  | "cierre-caja"
  | "estadisticas"
  | "reportes"
  | "sincronizar";

export type AppShellNavItem = {
  href: string;
  label: string;
  iconKey: NavIconKey;
};

type Section = {
  title: string;
  items: AppShellNavItem[];
};

const SECTIONS: Section[] = [
  {
    title: "GESTIÓN",
    items: [
      { href: "/pacientes", label: "Pacientes", iconKey: "pacientes" },
      { href: "/servicios-varios", label: "Servicios Varios", iconKey: "servicios-varios" },
      { href: "/dashboard", label: "Operación de Hoy", iconKey: "operacion-hoy" },
    ]
  },
  {
    title: "PROGRAMACIÓN",
    items: [
      { href: "/agenda", label: "Agenda", iconKey: "agenda" },
      { href: "/inventario", label: "Inventario", iconKey: "inventario" },
    ]
  },
  {
    title: "FINANZAS",
    items: [
      { href: "/cierre-caja", label: "Cierre de Caja", iconKey: "cierre-caja" },
      { href: "/estadisticas", label: "Estadísticas (BI)", iconKey: "estadisticas" },
      { href: "/reportes", label: "Reportes", iconKey: "reportes" },
    ]
  },
  {
    title: "SISTEMA",
    items: [
      { href: "/configuracion", label: "Configuración", iconKey: "configuracion" },
      { href: "/sincronizar", label: "Sincronizar Cloud", iconKey: "sincronizar" },
    ]
  }
];

const ICONS: Record<NavIconKey, ComponentType<SVGProps<SVGSVGElement>>> = {
  pacientes: IconPacientes,
  "servicios-varios": IconServiciosVarios,
  "operacion-hoy": IconOperacionHoy,
  configuracion: IconConfiguracion,
  agenda: IconAgenda,
  inventario: IconInventario,
  "cierre-caja": IconCierreCaja,
  estadisticas: IconEstadisticas,
  reportes: IconReportes,
  sincronizar: IconSincronizar
};

function isNavActive(pathname: string, href: string): boolean {
  if (href === "/dashboard") return pathname === "/dashboard";
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function AppShellNav(_props?: { items?: AppShellNavItem[] }) {
  const pathname = usePathname();

  return (
    <nav className="flex flex-1 flex-col overflow-y-auto px-4 py-6">
      <div className="flex-1 space-y-6">
        {SECTIONS.map((section) => (
          <div key={section.title}>
            <p className="mb-1.5 px-4 text-[10px] font-black uppercase tracking-[0.15em] text-muted-foreground/40">
              {section.title}
            </p>
            <div className="space-y-0.5">
              {section.items.map((item) => {
                const active = isNavActive(pathname, item.href);
                const Icon = ICONS[item.iconKey];
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`group relative flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all duration-300 ${
                      active
                        ? "bg-primary/10 text-primary shadow-[inset_0_0_0_1px_hsl(var(--primary)/0.2)]"
                        : "text-muted-foreground hover:bg-foreground/5 hover:text-foreground hover:translate-x-1"
                    }`}
                  >
                    {active && (
                      <span className="absolute left-0 h-5 w-1 rounded-full bg-primary" />
                    )}
                    <span className={`flex shrink-0 items-center justify-center transition-all duration-300 ${
                      active ? "scale-110 text-primary" : "text-muted-foreground group-hover:text-primary"
                    }`}>
                      <Icon className="h-5 w-5" />
                    </span>
                    <span className="truncate tracking-wide">{item.label}</span>
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {/* Logout */}
      <div className="mt-auto pt-4 border-t border-border/30">
        <Link
          href="/auth/logout"
          className="group relative flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-muted-foreground transition-all hover:bg-destructive/5 hover:text-destructive"
        >
          <LogOut className="h-5 w-5 shrink-0" />
          <span className="truncate tracking-wide">Cerrar Sesión</span>
        </Link>
      </div>
    </nav>
  );
}
