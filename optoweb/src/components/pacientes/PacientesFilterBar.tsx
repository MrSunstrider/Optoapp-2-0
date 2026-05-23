import Link from "next/link";
import { Search } from "lucide-react";

import type { ListChipFilter } from "@/lib/pacientes";
import type { PacientesSearchParams } from "@/lib/pacientes-utils";
import {
  hrefPage,
  hrefChipToggle,
  baseParamsWithoutChip
} from "@/lib/pacientes-utils";

type LimitInfo = {
  puedeCrearMas: boolean;
  maxPacientes: number | null;
  pacientesActuales: number;
};

type PacientesFilterBarProps = {
  sp: PacientesSearchParams;
  totalPages: number;
  total: number;
  canManage: boolean;
  limitInfo: LimitInfo;
  reachedLimit: boolean;
};

export function PacientesFilterBar({
  sp,
  totalPages,
  total,
  canManage,
  limitInfo,
  reachedLimit
}: PacientesFilterBarProps) {
  const { q, chip, page, minEdad, maxEdad } = sp;

  return (
    <>
      {sp.msg === "eliminado" && (
        <div className="mb-4 rounded-2xl border border-emerald-500/20 bg-emerald-500/5 px-5 py-3.5 text-sm font-medium text-emerald-700 dark:text-emerald-300">
          <div className="flex items-center gap-2">
            <svg className="h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <p>Paciente eliminado correctamente.</p>
          </div>
          {sp.restantesValid !== null && (
            <p className="mt-1.5 text-xs text-emerald-600/80 dark:text-emerald-400/80 ml-6">
              Eliminaciones restantes hoy: <span className="font-bold tabular-nums">{sp.restantesValid}</span>.
            </p>
          )}
        </div>
      )}
      {canManage && reachedLimit && (
        <div className="mb-4 rounded-2xl border border-amber-500/20 bg-amber-500/5 px-5 py-3.5 text-sm font-medium text-amber-700 dark:text-amber-300">
          Límite de pacientes alcanzado
          {limitInfo.maxPacientes != null
            ? ` (${limitInfo.pacientesActuales}/${limitInfo.maxPacientes}). `
            : ". "}
          <Link href="/configuracion" className="ml-1 underline font-bold">
            Actualizar plan
          </Link>
        </div>
      )}

      <form className="mb-6 flex flex-col gap-5" method="get" role="search">
        <div className="flex flex-wrap gap-3">
          <div className="relative min-w-0 flex-1 group">
            <Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground/50 transition-colors group-focus-within:text-primary" />
            <input
              type="search"
              name="q"
              defaultValue={q}
              placeholder="Buscar por nombre, ID o teléfono…"
              className="w-full rounded-2xl border border-border bg-background py-3.5 pl-12 pr-4 text-sm font-medium text-foreground transition-all focus:bg-card focus:ring-2 focus:ring-primary/20 outline-none shadow-sm"
            />
          </div>
          {chip !== "none" && (
            <input type="hidden" name="chip" value={chip} />
          )}
          <button
            type="submit"
            className="shrink-0 rounded-2xl bg-primary/10 px-6 py-3.5 text-sm font-bold text-primary transition-all hover:bg-primary/20 active:scale-95"
          >
            Buscar
          </button>
        </div>

        <div className="flex flex-wrap gap-2">
          {(() => {
            const qp = baseParamsWithoutChip(sp);
            qp.set("page", "1");
            return (
              <Link
                href={`/pacientes?${qp.toString()}`}
                className={
                  "rounded-full border px-3 py-1.5 text-xs font-bold transition-colors " +
                  (chip === "none"
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-border bg-background text-muted-foreground hover:border-primary/50")
                }
              >
                Todos
              </Link>
            );
          })()}
          <Link
            href={hrefChipToggle(sp, "saldo")}
            className={
              "rounded-full border px-3 py-1.5 text-xs font-bold transition-colors " +
              (chip === "saldo"
                ? "border-primary bg-primary text-primary-foreground"
                : "border-border bg-background text-muted-foreground hover:border-primary/50")
            }
          >
            Saldo Pendiente
          </Link>
          <Link
            href={hrefChipToggle(sp, "entrega")}
            className={
              "rounded-full border px-3 py-1.5 text-xs font-bold transition-colors " +
              (chip === "entrega"
                ? "border-primary bg-primary text-primary-foreground"
                : "border-border bg-background text-muted-foreground hover:border-primary/50")
            }
          >
            Estado de entrega: Pendiente
          </Link>
        </div>

        <details className="group text-sm">
          <summary className="cursor-pointer rounded-xl border border-border/50 bg-foreground/[0.02] px-4 py-2.5 text-xs font-bold text-muted-foreground transition-all hover:bg-accent hover:text-accent-foreground group-open:rounded-b-none group-open:border-b-0 list-none flex items-center gap-2">
            <svg className="h-3.5 w-3.5 transition-transform group-open:rotate-90" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 18l6-6-6-6"/></svg>
            Más filtros (edad)
          </summary>
          <div className="flex flex-wrap gap-2 rounded-b-xl border border-border/50 bg-foreground/[0.01] p-4">
            <input
              type="number"
              name="minEdad"
              defaultValue={minEdad ?? ""}
              placeholder="Edad mín."
              className="w-28 rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-primary/20 outline-none"
            />
            <input
              type="number"
              name="maxEdad"
              defaultValue={maxEdad ?? ""}
              placeholder="Edad máx."
              className="w-28 rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-primary/20 outline-none"
            />
            <button
              type="submit"
              className="rounded-lg bg-primary px-4 py-2 text-xs font-bold text-primary-foreground shadow-sm hover:bg-primary/90 transition-all"
            >
              Aplicar
            </button>
          </div>
        </details>
      </form>

      <div className="mt-6 flex flex-wrap items-center justify-between gap-4 border-t border-border pt-5 text-sm">
        <span className="text-muted-foreground font-medium">
          Página {page} de {totalPages} · {total} pacientes
        </span>
        <div className="flex gap-2">
          <Link
            href={hrefPage(sp, Math.max(1, page - 1))}
            className={
              "inline-flex items-center gap-1.5 rounded-xl border border-border px-4 py-2 text-xs font-bold text-foreground transition-all hover:bg-accent hover:text-accent-foreground " +
              (page <= 1 ? "pointer-events-none opacity-30" : "")
            }
          >
            <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 18l-6-6 6-6"/></svg>
            Anterior
          </Link>
          <Link
            href={hrefPage(sp, Math.min(totalPages, page + 1))}
            className={
              "inline-flex items-center gap-1.5 rounded-xl border border-border px-4 py-2 text-xs font-bold text-foreground transition-all hover:bg-accent hover:text-accent-foreground " +
              (page >= totalPages ? "pointer-events-none opacity-30" : "")
            }
          >
            Siguiente
            <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 18l6-6-6-6"/></svg>
          </Link>
        </div>
      </div>
    </>
  );
}
