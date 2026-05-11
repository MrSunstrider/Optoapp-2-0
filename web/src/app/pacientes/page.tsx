import { Search } from "lucide-react";
import Link from "next/link";
import { redirect } from "next/navigation";

import { PacientesListFab } from "@/components/pacientes/pacientes-list-fab";
import { AppShell } from "@/components/app-shell";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { getOpticaPacienteLimitInfo } from "@/lib/optica-limits";
import {
  countPacientes,
  fetchPacientes,
  ListChipFilter,
  parseEtiquetasRecientes
} from "@/lib/pacientes";
import {
  canManagePacientes,
  canReadPacientes
} from "@/lib/roles";

function parseChip(raw: string | undefined): ListChipFilter {
  if (raw === "saldo") return "saldo";
  if (raw === "entrega") return "entrega";
  return "none";
}

export default async function PacientesPage({
  searchParams
}: {
  searchParams: Promise<{
    q?: string;
    page?: string;
    minEdad?: string;
    maxEdad?: string;
    msg?: string;
    chip?: string;
    restantes?: string;
  }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  if (!canReadPacientes(activeOptica.rol)) {
    return (
      <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
        <h1 className="mb-2 text-2xl font-semibold">Pacientes</h1>
        <p className="text-sm text-muted-foreground">
          Tu rol actual no tiene permiso para consultar pacientes.
        </p>
      </AppShell>
    );
  }

  const params = await searchParams;
  const restantesRaw = params.restantes?.trim();
  const restantesElim =
    restantesRaw !== undefined && restantesRaw !== ""
      ? Number(restantesRaw)
      : null;
  const restantesValid =
    restantesElim !== null && Number.isFinite(restantesElim)
      ? Math.max(0, Math.floor(restantesElim))
      : null;
  const q = params.q ?? "";
  const page = Math.max(1, Number(params.page ?? "1") || 1);
  const minEdad = params.minEdad ? Number(params.minEdad) : undefined;
  const maxEdad = params.maxEdad ? Number(params.maxEdad) : undefined;
  const chip = parseChip(params.chip);
  const pageSize = 20;
  const supabase = await createClient();

  const listOpts = {
    search: q,
    minEdad,
    maxEdad,
    chip
  };

  const total = await countPacientes(supabase, activeOptica.opticaId, listOpts);
  const limitInfo = await getOpticaPacienteLimitInfo(
    supabase,
    activeOptica.opticaId
  );
  const canManage = canManagePacientes(activeOptica.rol);
  const reachedLimit = !limitInfo.puedeCrearMas;

  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  if (page > totalPages && total > 0) {
    const qp = new URLSearchParams();
    if (q) qp.set("q", q);
    if (params.minEdad) qp.set("minEdad", params.minEdad);
    if (params.maxEdad) qp.set("maxEdad", params.maxEdad);
    if (chip !== "none") qp.set("chip", chip);
    qp.set("page", String(totalPages));
    redirect(`/pacientes?${qp.toString()}`);
  }

  const rows = await fetchPacientes(supabase, activeOptica.opticaId, {
    ...listOpts,
    page,
    pageSize
  });

  const prevPage = Math.max(1, page - 1);
  const nextPage = Math.min(totalPages, page + 1);

  function baseParamsWithoutChip(): URLSearchParams {
    const qp = new URLSearchParams();
    if (q) qp.set("q", q);
    if (params.minEdad) qp.set("minEdad", params.minEdad);
    if (params.maxEdad) qp.set("maxEdad", params.maxEdad);
    return qp;
  }

  function baseParamsFull(): URLSearchParams {
    const qp = baseParamsWithoutChip();
    if (chip !== "none") qp.set("chip", chip);
    return qp;
  }

  function hrefPage(p: number): string {
    const qp = baseParamsFull();
    qp.set("page", String(p));
    return `/pacientes?${qp.toString()}`;
  }

  function hrefChipToggle(which: "saldo" | "entrega"): string {
    const qp = baseParamsWithoutChip();
    if (which === "saldo") {
      if (chip === "saldo") {
        /* quitar filtro */
      } else {
        qp.set("chip", "saldo");
      }
    } else if (chip === "entrega") {
      /* quitar filtro */
    } else {
      qp.set("chip", "entrega");
    }
    qp.set("page", "1");
    return `/pacientes?${qp.toString()}`;
  }

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="-mx-2 rounded-2xl border border-border bg-card px-5 py-6 text-foreground shadow-xl sm:mx-0 sm:px-6">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <h1 className="font-heading text-2xl font-bold tracking-tight text-primary sm:text-3xl">
            Pacientes
          </h1>
          {canManage && (
            <Link
              href="/pacientes/nuevo"
              className="hidden rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-105 active:scale-95 md:inline-flex"
            >
              Nuevo Paciente
            </Link>
          )}
        </div>

        {params.msg === "eliminado" && (
          <div className="mb-3 rounded-lg border border-emerald-900/50 bg-emerald-950/40 px-3 py-2 text-sm text-emerald-300">
            <p>Paciente eliminado correctamente.</p>
            {restantesValid !== null && (
              <p className="mt-1 text-emerald-400/90">
                Eliminaciones permitidas restantes hoy en esta óptica:{" "}
                <span className="font-semibold tabular-nums">{restantesValid}</span>.
              </p>
            )}
          </div>
        )}
        {canManage && reachedLimit && (
          <div className="mb-3 rounded-lg border border-amber-800/40 bg-amber-950/30 px-3 py-2 text-sm text-amber-200">
            Límite de pacientes alcanzado
            {limitInfo.maxPacientes != null
              ? ` (${limitInfo.pacientesActuales}/${limitInfo.maxPacientes}). `
              : ". "}
            <Link href="/configuracion" className="ml-1 underline">
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
              const qp = baseParamsWithoutChip();
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
              href={hrefChipToggle("saldo")}
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
              href={hrefChipToggle("entrega")}
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

          <details className="text-sm text-muted-foreground">
            <summary className="cursor-pointer hover:text-primary transition-colors">
              Más filtros (edad)
            </summary>
            <div className="mt-2 flex flex-wrap gap-2">
              <input
                type="number"
                name="minEdad"
                defaultValue={params.minEdad ?? ""}
                placeholder="Edad mín."
                className="w-28 rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-primary/20 outline-none"
              />
              <input
                type="number"
                name="maxEdad"
                defaultValue={params.maxEdad ?? ""}
                placeholder="Edad máx."
                className="w-28 rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-primary/20 outline-none"
              />
              <button
                type="submit"
                className="rounded-lg border border-border bg-muted px-4 py-2 text-xs font-bold text-foreground hover:bg-primary/10"
              >
                Aplicar
              </button>
            </div>
          </details>
        </form>

        <div className="border-t border-border mt-2 pt-2">
          {rows.length === 0 ? (
            <p className="py-10 text-center text-sm text-muted-foreground">
              Sin pacientes para este criterio.
            </p>
          ) : (
            rows.map((row) => {
              const tags = parseEtiquetasRecientes(row.ultimas_etiquetas, 2);
              const idPreview = (id: string) => id.length > 8 ? `${id.slice(0, 8)}…` : id;
              
              return (
                <Link
                  key={row.id}
                  href={`/pacientes/${row.id}`}
                  className="block group py-5 transition-colors border-b border-border last:border-b-0 hover:bg-muted/30"
                >
                  <div className="flex gap-5">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                      <svg
                        viewBox="0 0 24 24"
                        width={24}
                        height={24}
                        fill="none"
                        stroke="currentColor"
                        strokeWidth={2}
                      >
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                      </svg>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="font-heading text-lg font-bold text-foreground truncate">
                        {row.nombre_completo}
                      </p>
                      <p className="text-sm text-muted-foreground mt-0.5">
                        {row.edad} años · {row.telefono}
                      </p>
                      <p className="mt-1 font-mono text-[10px] font-bold uppercase tracking-widest text-muted-foreground/50">
                        EXP: {idPreview(row.id)}
                      </p>
                      {tags.length > 0 && (
                        <div className="mt-2 flex flex-wrap gap-2">
                          {tags.map((t) => (
                            <span
                              key={t}
                              className="rounded-full border border-primary/20 bg-primary/5 px-2.5 py-0.5 text-[10px] font-bold text-primary uppercase tracking-tighter"
                            >
                              {t}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                </Link>
              );
            })
          )}
        </div>

        <div className="mt-5 flex flex-wrap items-center justify-between gap-2 border-t border-zinc-800 pt-4 text-sm text-zinc-500">
          <span>
            Página {page} de {totalPages} · {total} pacientes
          </span>
          <div className="flex gap-2">
            <Link
              href={hrefPage(prevPage)}
              className={
                "rounded-md border border-zinc-700 px-2 py-1 text-zinc-300 hover:bg-zinc-800 " +
                (page <= 1 ? "pointer-events-none opacity-40" : "")
              }
            >
              Anterior
            </Link>
            <Link
              href={hrefPage(nextPage)}
              className={
                "rounded-md border border-zinc-700 px-2 py-1 text-zinc-300 hover:bg-zinc-800 " +
                (page >= totalPages ? "pointer-events-none opacity-40" : "")
              }
            >
              Siguiente
            </Link>
          </div>
        </div>
      </div>

      {canManage && (
        <PacientesListFab
          puedeCrear={limitInfo.puedeCrearMas}
          maxPacientes={limitInfo.maxPacientes}
          pacientesActuales={limitInfo.pacientesActuales}
        />
      )}
    </AppShell>
  );
}
