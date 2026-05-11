"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export function PacienteExpedienteFab({ pacienteId }: { pacienteId: string }) {
  const pathname = usePathname();
  const base = `/pacientes/${pacienteId}`;

  let href: string | null = null;
  let label = "";

  if (
    pathname === `${base}/evaluaciones` ||
    pathname.startsWith(`${base}/evaluaciones/`)
  ) {
    if (!pathname.includes("/nueva")) {
      href = `${base}/evaluaciones/nueva`;
      label = "Nueva evaluación";
    }
  } else if (
    pathname === `${base}/dispensaciones` ||
    pathname.startsWith(`${base}/dispensaciones/`)
  ) {
    if (!pathname.includes("/nueva")) {
      href = `${base}/dispensaciones/nueva`;
      label = "Nueva dispensación";
    }
  } else if (
    pathname === `${base}/servicios-extra` ||
    pathname.startsWith(`${base}/servicios-extra/`)
  ) {
    if (!pathname.includes("/nuevo")) {
      href = `${base}/servicios-extra/nuevo`;
      label = "Nuevo servicio";
    }
  }

  if (!href) return null;

  if (pathname.includes("/editar")) return null;

  return (
    <Link
      href={href}
      className="fixed bottom-24 right-6 z-40 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary text-3xl font-bold text-primary-foreground shadow-2xl shadow-primary/30 transition-all hover:scale-110 active:scale-90 md:bottom-20"
      title={label}
      aria-label={label}
    >
      +
    </Link>
  );
}
