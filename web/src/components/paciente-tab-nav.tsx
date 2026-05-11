"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const tabs = (base: string) =>
  [
    {
      href: `${base}/evaluaciones`,
      label: "Evaluaciones",
      match: "prefix" as const
    },
    {
      href: `${base}/dispensaciones`,
      label: "Dispensaciones",
      match: "prefix" as const
    },
    {
      href: `${base}/servicios-extra`,
      label: "Servicios extra",
      match: "prefix" as const
    }
  ] as const;

function isTabActive(
  pathname: string,
  href: string,
  match: "exact" | "prefix"
): boolean {
  if (match === "exact") {
    return pathname === href || pathname === `${href}/`;
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function PacienteTabNav({
  pacienteId,
  variant = "default"
}: {
  pacienteId: string;
  nombre?: string;
  variant?: "default" | "fichaDark";
}) {
  const pathname = usePathname();
  const base = `/pacientes/${pacienteId}`;
  const dark = variant === "fichaDark";

  return (
    <nav className="mt-4 overflow-x-auto border-b border-zinc-800">
      <div className="flex min-w-max gap-1 sm:gap-6">
      {tabs(base).map((t) => {
        const active = isTabActive(pathname, t.href, t.match);
        return (
          <Link
            key={t.href}
            href={t.href}
            className={
              dark
                ? "-mb-px border-b-2 px-2 py-2.5 text-sm font-medium transition-colors sm:px-1 " +
                  (active
                    ? "border-sky-400 text-sky-300"
                    : "border-transparent text-zinc-500 hover:text-zinc-300")
                : "rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors " +
                  (active
                    ? "border-primary bg-primary text-primary-foreground shadow-sm"
                    : "border-border bg-card text-foreground hover:bg-accent hover:text-accent-foreground")
            }
          >
            {t.label}
          </Link>
        );
      })}
      </div>
    </nav>
  );
}
