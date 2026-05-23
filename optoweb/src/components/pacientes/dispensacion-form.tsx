"use client";

import Link from "next/link";
import { useActionState, useMemo, useState } from "react";
import type { DispensacionFormProps, PagoDraft } from "@/lib/dispensacion-types";
import {
  ORIGEN_MONTURA,
  TIPO_ARO,
  MATERIAL_MONTURA,
} from "@/lib/dispensacion-types";
import { LentesForm } from "./dispensacion/LentesForm";
import { PagosSection } from "./dispensacion/PagosSection";

export function DispensacionForm({
  pacienteId,
  dispensacionId,
  mode,
  backHref,
  opticaLine,
  todayDate,
  initial,
  monturas,
  saveAction,
}: DispensacionFormProps) {
  const [actionState, action, isPending] = useActionState(saveAction, null);

  const [fecha, setFecha] = useState(initial.fecha || todayDate);
  const [ot, setOt] = useState(initial.ot);
  const [tipoLente, setTipoLente] = useState(initial.tipoLente);
  const [subTipoBifocal, setSubTipoBifocal] = useState(initial.subTipoBifocal);
  const [distanciaLente, setDistanciaLente] = useState(initial.distanciaLente);
  const [altura, setAltura] = useState(initial.altura);
  const [materialLente, setMaterialLente] = useState(initial.materialLente);
  const [tratamientos, setTratamientos] = useState<string[]>(
    initial.tratamientos.length > 0 ? initial.tratamientos : [],
  );
  const [colorLente, setColorLente] = useState(initial.colorLente);
  const [notasDiseno, setNotasDiseno] = useState(initial.notasDiseno);
  const [origenMontura, setOrigenMontura] = useState(
    initial.origenMontura || "Tienda",
  );
  const [monturaId, setMonturaId] = useState(initial.monturaId);
  const [tipoAro, setTipoAro] = useState(initial.tipoAro);
  const [materialMontura, setMaterialMontura] = useState(
    initial.materialMontura,
  );
  const [descripcionMontura, setDescripcionMontura] = useState(
    initial.descripcionMontura,
  );
  const [montoTotal, setMontoTotal] = useState(initial.montoTotal);
  const [estadoEntrega, setEstadoEntrega] = useState(
    initial.estadoEntrega || "Pendiente",
  );
  const [pagos, setPagos] = useState<PagoDraft[]>(initial.pagos);
  const [pagosDeleteIds, setPagosDeleteIds] = useState<string[]>([]);

  const persistedPagoIds = useMemo(
    () => new Set(initial.pagos.map((p) => p.id).filter(Boolean)),
    [initial.pagos],
  );

  const monturasDisponibles = useMemo(
    () =>
      monturas.filter(
        (m) =>
          (m.activo ?? true) &&
          ((m.stock_actual ?? 0) > 0 || m.id === monturaId),
      ),
    [monturaId, monturas],
  );

  async function suggestOt() {
    const res = await fetch(
      `/api/dispensaciones/suggest-ot?fecha=${encodeURIComponent(fecha)}`,
      { cache: "no-store" },
    );
    if (!res.ok) return;
    const data = (await res.json()) as { ot?: string };
    if (data.ot) setOt(data.ot);
  }

  return (
    <form action={action} className="space-y-4">
      {actionState?.error && (
        <p className="mb-3 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {actionState.error}
        </p>
      )}

      <input type="hidden" name="pacienteId" value={pacienteId} />
      <input type="hidden" name="dispensacionId" value={dispensacionId ?? ""} />
      <input
        type="hidden"
        name="previousMonturaId"
        value={initial.previousMonturaId}
      />
      <input type="hidden" name="fecha" value={fecha} />
      <input type="hidden" name="ot" value={ot} />
      <input type="hidden" name="tipoLente" value={tipoLente} />
      <input type="hidden" name="subTipoBifocal" value={subTipoBifocal} />
      <input type="hidden" name="distanciaLente" value={distanciaLente} />
      <input type="hidden" name="altura" value={altura} />
      <input type="hidden" name="materialLente" value={materialLente} />
      <input
        type="hidden"
        name="tratamientosJson"
        value={JSON.stringify(tratamientos)}
      />
      <input type="hidden" name="colorLente" value={colorLente} />
      <input type="hidden" name="notasDiseno" value={notasDiseno} />
      <input type="hidden" name="origenMontura" value={origenMontura} />
      <input type="hidden" name="monturaId" value={monturaId} />
      <input type="hidden" name="tipoAro" value={tipoAro} />
      <input type="hidden" name="materialMontura" value={materialMontura} />
      <input
        type="hidden"
        name="descripcionMontura"
        value={descripcionMontura}
      />
      <input type="hidden" name="montoTotal" value={montoTotal} />
      <input type="hidden" name="estadoEntrega" value={estadoEntrega} />
      <input
        type="hidden"
        name="pagosJson"
        value={JSON.stringify(pagos)}
      />
      <input
        type="hidden"
        name="pagosDeleteJson"
        value={JSON.stringify(pagosDeleteIds)}
      />

      <div className="rounded-2xl border border-border bg-card text-foreground shadow-xl">
        <div className="border-b border-border px-4 py-2 text-[11px] font-black uppercase tracking-widest text-primary/70">
          {opticaLine}
        </div>

        <div className="flex items-center gap-3 bg-primary px-4 py-4 text-primary-foreground shadow-md">
          <Link
            href={backHref}
            className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 hover:bg-white/20 transition-all active:scale-95"
          >
            <span className="text-xl font-bold">{`←`}</span>
          </Link>
          <h2 className="flex-1 font-heading text-xl font-bold">
            {mode === "edit"
              ? "Editar Dispensación"
              : "Nueva Dispensación"}
          </h2>
          <button
            type="submit"
            disabled={isPending}
            className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 hover:bg-white/20 transition-all active:scale-95 disabled:opacity-50"
          >
            {isPending ? "…" : "💾"}
          </button>
        </div>

        <div className="space-y-6 px-5 py-6">
          <div className="space-y-4">
            <label className="block">
              <span className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 ml-1">
                Fecha de Orden
              </span>
              <input
                type="date"
                value={fecha}
                onChange={(e) => setFecha(e.target.value)}
                className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium text-foreground transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
              />
            </label>

            <div className="flex items-end gap-3">
              <label className="flex-1">
                <span className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 ml-1">
                  N° Orden (OT)
                </span>
                <input
                  value={ot}
                  onChange={(e) => setOt(e.target.value)}
                  placeholder="OT-AAAA-####"
                  className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium text-foreground transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
                />
              </label>
              <button
                type="button"
                onClick={suggestOt}
                className="h-[46px] rounded-xl border border-primary/20 bg-primary/5 px-4 text-xs font-bold text-primary transition-all hover:bg-primary/10 active:scale-95"
              >
                Sugerir OT
              </button>
            </div>
          </div>

          <LentesForm
            tipoLente={tipoLente}
            setTipoLente={setTipoLente}
            subTipoBifocal={subTipoBifocal}
            setSubTipoBifocal={setSubTipoBifocal}
            distanciaLente={distanciaLente}
            setDistanciaLente={setDistanciaLente}
            altura={altura}
            setAltura={setAltura}
            materialLente={materialLente}
            setMaterialLente={setMaterialLente}
            tratamientos={tratamientos}
            setTratamientos={setTratamientos}
            colorLente={colorLente}
            setColorLente={setColorLente}
            notasDiseno={notasDiseno}
            setNotasDiseno={setNotasDiseno}
          />

          <section className="rounded-xl border border-zinc-700 bg-zinc-800/40 p-3 space-y-2">
            <h3 className="font-semibold text-sky-300">
              Informacion de Montura
            </h3>

            <select
              value={origenMontura}
              onChange={(e) => {
                setOrigenMontura(e.target.value);
                if (e.target.value !== "Tienda") setMonturaId("");
              }}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2"
            >
              {ORIGEN_MONTURA.map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>

            {origenMontura === "Tienda" && (
              <select
                value={monturaId}
                onChange={(e) => {
                  const id = e.target.value;
                  setMonturaId(id);
                  const m = monturasDisponibles.find((x) => x.id === id);
                  if (m)
                    setDescripcionMontura(
                      `${m.marca ?? ""} ${m.modelo ?? ""}`.trim(),
                    );
                }}
                className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2"
              >
                <option value="">Montura de inventario</option>
                {monturasDisponibles.map((m) => (
                  <option key={m.id} value={m.id}>
                    {(m.sku ?? m.id)} - {(m.marca ?? "").trim()}{" "}
                    {(m.modelo ?? "").trim()} (Stock: {m.stock_actual ?? 0})
                  </option>
                ))}
              </select>
            )}

            <select
              value={tipoAro}
              onChange={(e) => setTipoAro(e.target.value)}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2"
            >
              <option value="">Tipo de Aro</option>
              {TIPO_ARO.map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>

            <select
              value={materialMontura}
              onChange={(e) => setMaterialMontura(e.target.value)}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2"
            >
              <option value="">Material</option>
              {MATERIAL_MONTURA.map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>

            <input
              value={descripcionMontura}
              onChange={(e) => setDescripcionMontura(e.target.value)}
              placeholder="Descripción (Marca, Modelo)"
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
            />
          </section>

          <PagosSection
            montoTotal={montoTotal}
            setMontoTotal={setMontoTotal}
            pagos={pagos}
            setPagos={setPagos}
            pagosDeleteIds={pagosDeleteIds}
            setPagosDeleteIds={setPagosDeleteIds}
            persistedPagoIds={persistedPagoIds}
            estadoEntrega={estadoEntrega}
            setEstadoEntrega={setEstadoEntrega}
            todayDate={todayDate}
            isPending={isPending}
            mode={mode}
          />
        </div>
      </div>
    </form>
  );
}
