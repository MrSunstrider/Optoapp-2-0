"use client";

import { deleteServicioExtraAction } from "@/app/pacientes/_actions/servicio-crud";
import {
  computeSaldoServicioExtra,
  formatFechaServicioListado,
  formatSoles,
  isSaldoPagado,
  type ServicioVariosListRow
} from "@/lib/servicios-extra-display";
import { Pencil, Search, Trash2 } from "lucide-react";
import Link from "next/link";
import { useEffect, useMemo, useState, useTransition } from "react";

function EstadoPill({ estado }: { estado: string | null }) {
  const raw = estado?.trim() || "";
  const isEntregado = raw.toLowerCase() === "entregado";
  if (isEntregado) {
    return (
      <span className="inline-flex rounded-full bg-emerald-500/20 px-2.5 py-0.5 text-xs font-medium text-emerald-200 ring-1 ring-inset ring-emerald-500/40">
        Entregado
      </span>
    );
  }
  return (
    <span className="inline-flex rounded-full bg-teal-500/20 px-2.5 py-0.5 text-xs font-medium text-teal-200 ring-1 ring-inset ring-teal-500/40">
      {raw || "Pendiente"}
    </span>
  );
}

export function ServiciosVariosList({
  rows,
  canManage
}: {
  rows: ServicioVariosListRow[];
  canManage: boolean;
}) {
  const [search, setSearch] = useState("");
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [deleteLabel, setDeleteLabel] = useState("");
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    if (!deleteId) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !isPending) setDeleteId(null);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [deleteId, isPending]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => {
      const desc = (r.descripcion ?? "").toLowerCase();
      const otRaw = (r.ot ?? "").trim().toLowerCase();
      const digitsOt = otRaw.replace(/\D/g, "");
      const qDigits = q.replace(/\D/g, "");
      if (desc.includes(q)) return true;
      if (otRaw.includes(q)) return true;
      if (qDigits.length > 0 && digitsOt.includes(qDigits)) return true;
      return false;
    });
  }, [rows, search]);

  function requestDelete(r: ServicioVariosListRow) {
    setDeleteId(r.id);
    setDeleteLabel(r.descripcion?.trim() || "este servicio");
  }

  function confirmDelete() {
    if (!deleteId) return;
    startTransition(async () => {
      await deleteServicioExtraAction(deleteId);
    });
  }

  const fabClass =
    "fixed bottom-24 right-6 z-40 flex h-14 w-14 items-center justify-center rounded-full text-2xl font-light text-white shadow-lg transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-400 md:bottom-20";

  return (
    <>
      <div className="space-y-4">
        <div className="relative">
          <label htmlFor="sv-search" className="sr-only">
            Buscar por descripción u orden de trabajo
          </label>
          <Search
            className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-zinc-500"
            aria-hidden
          />
          <input
            id="sv-search"
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar por descripción u OT…"
            className="w-full rounded-xl border border-zinc-600 bg-zinc-900/80 py-3 pl-11 pr-3 text-[15px] text-white placeholder:text-zinc-500 focus-visible:border-sky-500/60 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-sky-500/50"
            autoComplete="off"
          />
        </div>

        {rows.length === 0 ? (
          <p className="rounded-xl border border-zinc-800 bg-zinc-900/40 px-4 py-8 text-center text-sm text-zinc-400">
            No hay servicios registrados.
          </p>
        ) : filtered.length === 0 ? (
          <p className="rounded-xl border border-zinc-800 bg-zinc-900/40 px-4 py-8 text-center text-sm text-zinc-400">
            No hay resultados para la búsqueda.
          </p>
        ) : (
          <ul className="divide-y divide-zinc-800 rounded-xl border border-zinc-800 bg-[#121212]">
            {filtered.map((r) => {
              const saldo = computeSaldoServicioExtra(r.monto_total, r.a_cuenta);
              const pagado = isSaldoPagado(saldo);
              const ot = r.ot?.trim();

              return (
                <li key={r.id} className="p-4">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0 flex-1 space-y-1">
                      {ot ? (
                        <p className="text-xs text-zinc-500">OT: {ot}</p>
                      ) : null}
                      <h2 className="text-base font-medium leading-snug text-sky-300">
                        {r.descripcion?.trim() || "—"}
                      </h2>
                      <p className="text-sm text-zinc-200">
                        Total:{" "}
                        <span className="tabular-nums">
                          {formatSoles(r.monto_total)}
                        </span>
                      </p>
                      <p className="text-sm">
                        Saldo:{" "}
                        <span
                          className={`tabular-nums font-medium ${
                            pagado ? "text-emerald-400" : "text-red-400"
                          }`}
                        >
                          {formatSoles(saldo)}
                        </span>
                      </p>
                    </div>

                    <div className="flex shrink-0 flex-row items-start justify-between gap-3 sm:flex-col sm:items-end">
                      {canManage && (
                        <div className="flex items-center gap-1 sm:order-first">
                          <Link
                            href={`/servicios-varios/${r.id}/editar`}
                            className="rounded-lg p-2 text-sky-400 transition-colors hover:bg-white/5 hover:text-sky-300 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-400"
                            aria-label="Editar servicio"
                            title="Editar"
                          >
                            <Pencil className="h-5 w-5" aria-hidden />
                          </Link>
                          <button
                            type="button"
                            className="rounded-lg p-2 text-red-400 transition-colors hover:bg-white/5 hover:text-red-300 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400 disabled:opacity-50"
                            aria-label="Eliminar servicio"
                            title="Eliminar"
                            disabled={isPending}
                            onClick={() => requestDelete(r)}
                          >
                            <Trash2 className="h-5 w-5" aria-hidden />
                          </button>
                        </div>
                      )}

                      <div className="flex flex-col items-end gap-2 text-right">
                        <EstadoPill estado={r.estado} />
                        <time
                          className="text-xs text-zinc-500"
                          dateTime={r.fecha ?? undefined}
                        >
                          {formatFechaServicioListado(r.fecha)}
                        </time>
                      </div>
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      {canManage && (
        <Link
          href="/servicios-varios/nuevo"
          className={
            fabClass +
            " bg-violet-600 shadow-violet-900/40 hover:bg-violet-500"
          }
          aria-label="Nuevo servicio"
          title="Nuevo servicio"
        >
          +
        </Link>
      )}

      {deleteId && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
          role="presentation"
          onClick={() => !isPending && setDeleteId(null)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="sv-del-title"
            className="w-full max-w-md rounded-xl border border-zinc-700 bg-zinc-900 p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="sv-del-title" className="text-lg font-semibold text-white">
              ¿Eliminar servicio?
            </h2>
            <p className="mt-2 text-sm text-zinc-400">
              Se eliminará “{deleteLabel}” y sus abonos asociados. Esta acción no se
              puede deshacer.
            </p>
            <div className="mt-6 flex flex-wrap justify-end gap-2">
              <button
                type="button"
                className="rounded-lg border border-zinc-600 px-4 py-2 text-sm font-medium text-zinc-200 hover:bg-white/5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-zinc-400"
                onClick={() => setDeleteId(null)}
                disabled={isPending}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400 disabled:opacity-60"
                onClick={confirmDelete}
                disabled={isPending}
              >
                {isPending ? "Eliminando…" : "Eliminar"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
