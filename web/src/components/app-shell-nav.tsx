"use client";

import type { ComponentType, SVGProps } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";

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
  if (href === "/dashboard") {
    return pathname === "/dashboard";
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function AppShellNav({ items }: { items: AppShellNavItem[] }) {
  const pathname = usePathname();

  return (
    <nav className="flex-1 space-y-1.5 overflow-y-auto px-4 py-6">
      {items.map((item) => {
        const active = isNavActive(pathname, item.href);
        const Icon = ICONS[item.iconKey];
        return (
          <Link
            key={item.href}
            href={item.href}
            className={
              "group relative flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all duration-300 " +
              (active
                ? "bg-primary/10 text-primary shadow-[inset_0_0_0_1px_rgba(39,174,96,0.2)]"
                : "text-muted-foreground hover:bg-foreground/5 hover:text-foreground hover:translate-x-1")
            }
          >
            {active && (
              <span className="absolute left-0 h-5 w-1 rounded-full bg-primary" />
            )}
            <span
              className={
                "flex shrink-0 items-center justify-center transition-all duration-300 " +
                (active ? "scale-110 text-primary" : "text-muted-foreground group-hover:text-primary")
              }
            >
              <Icon className="h-5 w-5" />
            </span>
            <span className="truncate tracking-wide">{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
