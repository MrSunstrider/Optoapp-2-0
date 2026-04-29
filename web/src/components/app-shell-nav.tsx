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
    <nav className="flex-1 space-y-0.5 overflow-y-auto px-3 py-3">
      {items.map((item) => {
        const active = isNavActive(pathname, item.href);
        const Icon = ICONS[item.iconKey];
        return (
          <Link
            key={item.href}
            href={item.href}
            className={
              "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors " +
              (active
                ? "bg-[#34495E] text-white shadow-sm"
                : "text-[#B0B0B0] hover:bg-white/[0.06] hover:text-white")
            }
          >
            <span
              className={
                "flex shrink-0 items-center justify-center " +
                (active ? "text-white" : "text-[#B0B0B0]")
              }
            >
              <Icon className="h-5 w-5" />
            </span>
            <span className="truncate">{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
