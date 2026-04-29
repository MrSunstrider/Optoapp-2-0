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
      {/* Panel estilo app móvil: fondo ~#121212, tipografía y jerarquía alineada a la captura */}
      <div className="-mx-2 rounded-2xl bg-[#121212] px-3 py-5 text-zinc-100 shadow-inner sm:mx-0 sm:px-5">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-xl font-semibold tracking-tight text-white sm:text-2xl">
            Pacientes
          </h1>
          {canManage && (
            <Link
              href="/pacientes/nuevo"
              className="hidden rounded-lg bg-violet-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-violet-500 md:inline-flex"
            >
              Nuevo paciente
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

        <form className="mb-5 flex flex-col gap-4" method="get" role="search">
          <div className="flex flex-wrap gap-2">
            <div className="relative min-w-0 flex-1">
              <svg
                className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2}
                aria-hidden
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
              <input
                type="search"
                name="q"
                defaultValue={q}
                placeholder="Buscar por nombre, ID o teléfono…"
                className="w-full rounded-full border border-zinc-700 bg-[#1e1e1e] py-2.5 pl-10 pr-4 text-sm text-zinc-100 placeholder:text-zinc-500 focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500"
              />
            </div>
            {chip !== "none" && (
              <input type="hidden" name="chip" value={chip} />
            )}
            <button
              type="submit"
              className="shrink-0 rounded-full border border-zinc-600 bg-zinc-800 px-4 py-2.5 text-sm font-medium text-zinc-200 hover:bg-zinc-700"
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
                    "rounded-full border px-3 py-1.5 text-xs font-medium transition-colors " +
                    (chip === "none"
                      ? "border-violet-500 bg-violet-600 text-white"
                      : "border-zinc-600 bg-transparent text-zinc-300 hover:border-zinc-500")
                  }
                >
                  Todos
                </Link>
              );
            })()}
            <Link
              href={hrefChipToggle("saldo")}
              className={
                "rounded-full border px-3 py-1.5 text-xs font-medium transition-colors " +
                (chip === "saldo"
                  ? "border-violet-500 bg-violet-600 text-white"
                  : "border-zinc-600 bg-transparent text-zinc-300 hover:border-zinc-500")
              }
            >
              Saldo Pendiente
            </Link>
            <Link
              href={hrefChipToggle("entrega")}
              className={
                "rounded-full border px-3 py-1.5 text-xs font-medium transition-colors " +
                (chip === "entrega"
                  ? "border-violet-500 bg-violet-600 text-white"
                  : "border-zinc-600 bg-transparent text-zinc-300 hover:border-zinc-500")
              }
            >
              Estado de entrega: Pendiente
            </Link>
          </div>

          <details className="text-sm text-zinc-400">
            <summary className="cursor-pointer hover:text-zinc-300">
              Más filtros (edad)
            </summary>
            <div className="mt-2 flex flex-wrap gap-2">
              <input
                type="number"
                name="minEdad"
                defaultValue={params.minEdad ?? ""}
                placeholder="Edad mín."
                className="w-28 rounded-md border border-zinc-700 bg-[#1e1e1e] px-2 py-1 text-zinc-100"
              />
              <input
                type="number"
                name="maxEdad"
                defaultValue={params.maxEdad ?? ""}
                placeholder="Edad máx."
                className="w-28 rounded-md border border-zinc-700 bg-[#1e1e1e] px-2 py-1 text-zinc-100"
              />
              <button
                type="submit"
                className="rounded-md border border-zinc-600 bg-zinc-800 px-2 py-1 text-xs text-zinc-200"
              >
                Aplicar
              </button>
            </div>
          </details>
        </form>

        <div className="divide-y divide-zinc-800">
          {rows.length === 0 ? (
            <p className="py-10 text-center text-sm text-zinc-500">
              Sin pacientes para este criterio.
            </p>
          ) : (
            rows.map((row) => {
              const tags = parseEtiquetasRecientes(row.ultimas_etiquetas, 2);
              const fechaFmt = row.fecha_creacion
                ? new Date(row.fecha_creacion + "T12:00:00").toLocaleDateString(
                    "es-PE",
                    { day: "numeric", month: "short", year: "numeric" }
                  )
                : "—";
              const idPreview =
                row.id.length > 8 ? `${row.id.slice(0, 8)}…` : row.id;
              return (
                <Link
                  key={row.id}
                  href={`/pacientes/${row.id}`}
                  className="block py-4 transition-colors hover:bg-white/[0.04] first:pt-0"
                >
                  <div className="flex items-start justify-between gap-3">
                    <p className="min-w-0 flex-1 font-semibold leading-snug text-sky-400">
                      {row.nombre_completo}
                    </p>
                    <span className="shrink-0 font-mono text-[11px] text-zinc-500">
                      ID: {idPreview}
                    </span>
                  </div>
                  <p className="mt-1.5 text-sm text-zinc-300">
                    Edad: {row.edad}{" "}
                    <span className="text-zinc-600">·</span> Tel: {row.telefono}
                  </p>
                  <p className="mt-1 text-xs text-zinc-500">Creado: {fechaFmt}</p>
                  {tags.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-2">
                      {tags.map((t) => (
                        <span
                          key={t}
                          className="rounded-full border border-zinc-700 bg-zinc-800/80 px-2 py-0.5 text-[11px] text-zinc-400"
                        >
                          {t}
                        </span>
                      ))}
                    </div>
                  )}
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
