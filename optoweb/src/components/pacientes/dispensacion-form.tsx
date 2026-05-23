"use client";

import Link from "next/link";
import { useActionState, useMemo, useState } from "react";
import type { DispensacionFormProps, ItemDraft, PagoDraft } from "@/lib/dispensacion-types";
import { ItemCard } from "./dispensacion/LentesForm";
import { PagosSection } from "./dispensacion/PagosSection";

function emptyItem(): ItemDraft {
  return {
    id: crypto.randomUUID(),
    tipoLente: "",
    subTipoBifocal: "",
    distanciaLente: "",
    altura: "",
    materialLente: "",
    tratamientos: [],
    colorLente: "",
    notasDiseno: "",
    origenMontura: "",
    monturaId: "",
    tipoAro: "",
    materialMontura: "",
    descripcionMontura: "",
  };
}

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
  const [items, setItems] = useState<ItemDraft[]>(
    initial.items.length > 0 ? initial.items : [emptyItem()],
  );
  const [itemsToDelete, setItemsToDelete] = useState<string[]>([]);
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

  function updateItem(index: number, item: ItemDraft) {
    setItems((prev) => {
      const next = [...prev];
      next[index] = item;
      return next;
    });
  }

  function addItem() {
    setItems((prev) => [...prev, emptyItem()]);
  }

  function removeItem(index: number) {
    setItems((prev) => {
      const removed = prev[index];
      const toDelete = removed.id ? [...itemsToDelete, removed.id] : itemsToDelete;
      setItemsToDelete(toDelete);
      const next = prev.filter((_, i) => i !== index);
      return next.length === 0 ? [emptyItem()] : next;
    });
  }

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
      <input type="hidden" name="fecha" value={fecha} />
      <input type="hidden" name="ot" value={ot} />
      <input
        type="hidden"
        name="itemsJson"
        value={JSON.stringify(items)}
      />
      <input
        type="hidden"
        name="itemsDeleteJson"
        value={JSON.stringify(itemsToDelete)}
      />
      <input
        type="hidden"
        name="previousItemIdsJson"
        value={JSON.stringify(initial.previousItemIds)}
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

          {/* Items (lente + montura) */}
          <div className="space-y-4">
            <h3 className="font-heading text-sm font-bold text-primary">
              Productos
            </h3>
            {items.map((item, i) => (
              <ItemCard
                key={item.id}
                item={item}
                index={i}
                isOnly={items.length <= 1}
                monturas={monturas}
                onChange={(updated) => updateItem(i, updated)}
                onRemove={() => removeItem(i)}
              />
            ))}
            <button
              type="button"
              onClick={addItem}
              className="w-full rounded-xl border-2 border-dashed border-primary/30 py-3 text-sm font-bold text-primary/60 transition-all hover:border-primary/60 hover:text-primary/80 active:scale-[0.98]"
            >
              + Agregar otro producto (lente + montura)
            </button>
          </div>

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
