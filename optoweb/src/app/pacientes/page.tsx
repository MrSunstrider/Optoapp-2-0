import Link from "next/link";
import { redirect } from "next/navigation";

import { PacientesListFab } from "@/components/pacientes/pacientes-list-fab";
import { PacientesFilterBar } from "@/components/pacientes/PacientesFilterBar";
import { AppShell } from "@/components/app-shell";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { getOpticaPacienteLimitInfo } from "@/lib/optica-limits";
import {
  countPacientes,
  fetchPacientes,
  parseEtiquetasRecientes
} from "@/lib/pacientes";
import {
  canManagePacientes,
  canReadPacientes
} from "@/lib/roles";
import { parsePacientesSearchParams } from "@/lib/pacientes-utils";
import { shortIdPreview } from "@/lib/pacientes-utils";

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
  const sp = parsePacientesSearchParams(params);
  const pageSize = 20;
  const supabase = await createClient();

  const listOpts = {
    search: sp.q,
    minEdad: sp.minEdad,
    maxEdad: sp.maxEdad,
    chip: sp.chip
  };

  const total = await countPacientes(supabase, activeOptica.opticaId, listOpts);
  const limitInfo = await getOpticaPacienteLimitInfo(
    supabase,
    activeOptica.opticaId
  );
  const canManage = canManagePacientes(activeOptica.rol);
  const reachedLimit = !limitInfo.puedeCrearMas;

  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  if (sp.page > totalPages && total > 0) {
    const qp = new URLSearchParams();
    if (sp.q) qp.set("q", sp.q);
    if (params.minEdad) qp.set("minEdad", params.minEdad);
    if (params.maxEdad) qp.set("maxEdad", params.maxEdad);
    if (sp.chip !== "none") qp.set("chip", sp.chip);
    qp.set("page", String(totalPages));
    redirect(`/pacientes?${qp.toString()}`);
  }

  const rows = await fetchPacientes(supabase, activeOptica.opticaId, {
    ...listOpts,
    page: sp.page,
    pageSize
  });

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

        <PacientesFilterBar
          sp={sp}
          totalPages={totalPages}
          total={total}
          canManage={canManage}
          limitInfo={limitInfo}
          reachedLimit={reachedLimit}
        />

        <div className="border-t border-border mt-2 pt-2">
          {rows.length === 0 ? (
            <p className="py-10 text-center text-sm text-muted-foreground">
              Sin pacientes para este criterio.
            </p>
          ) : (
            rows.map((row) => {
              const tags = parseEtiquetasRecientes(row.ultimas_etiquetas, 2);

              return (
                <Link
                  key={row.id}
                  href={`/pacientes/${row.id}`}
                  className="block group rounded-2xl transition-all duration-200 hover:bg-card hover:shadow-sm hover:border-border/80 border border-transparent -mx-2 px-4 py-4"
                >
                  <div className="flex gap-4">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/15 to-primary/5 text-primary shadow-sm transition-all group-hover:shadow-md group-hover:from-primary/20 group-hover:to-primary/10">
                      <svg
                        viewBox="0 0 24 24"
                        width={22}
                        height={22}
                        fill="none"
                        stroke="currentColor"
                        strokeWidth={1.5}
                      >
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                      </svg>
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <p className="font-heading text-lg font-bold text-foreground truncate group-hover:text-primary transition-colors">
                          {row.nombre_completo}
                        </p>
                        <svg className="h-4 w-4 shrink-0 text-muted-foreground/30 group-hover:text-primary transition-all opacity-0 group-hover:opacity-100 -translate-x-2 group-hover:translate-x-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 18l6-6-6-6"/></svg>
                      </div>
                      <p className="text-sm text-muted-foreground mt-0.5 flex items-center gap-2">
                        <span className="inline-flex items-center gap-1">
                          <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                          {row.edad} años
                        </span>
                        <span className="text-muted-foreground/30">·</span>
                        <span>{row.telefono}</span>
                      </p>
                      <p className="mt-1.5 font-mono text-[10px] font-bold uppercase tracking-widest text-muted-foreground/40">
                        EXP: {shortIdPreview(row.id)}
                      </p>
                      {tags.length > 0 && (
                        <div className="mt-2.5 flex flex-wrap gap-1.5">
                          {tags.map((t) => (
                            <span
                              key={t}
                              className="inline-flex items-center gap-1 rounded-full border border-primary/20 bg-primary/5 px-2.5 py-0.5 text-[10px] font-bold text-primary uppercase tracking-tighter transition-all group-hover:bg-primary/10"
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
