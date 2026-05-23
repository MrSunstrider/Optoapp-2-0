"use client";

import { useState } from "react";
import {
  PagoDraft,
  ESTADO_ENTREGA,
  METODO_PAGO,
  parseNum,
} from "@/lib/dispensacion-types";

type Props = {
  montoTotal: string;
  setMontoTotal: (v: string) => void;
  pagos: PagoDraft[];
  setPagos: React.Dispatch<React.SetStateAction<PagoDraft[]>>;
  pagosDeleteIds: string[];
  setPagosDeleteIds: React.Dispatch<React.SetStateAction<string[]>>;
  persistedPagoIds: Set<string>;
  estadoEntrega: string;
  setEstadoEntrega: (v: string) => void;
  todayDate: string;
  isPending: boolean;
  mode: "create" | "edit";
};

export function PagosSection({
  montoTotal,
  setMontoTotal,
  pagos,
  setPagos,
  pagosDeleteIds,
  setPagosDeleteIds,
  persistedPagoIds,
  estadoEntrega,
  setEstadoEntrega,
  todayDate,
  isPending,
  mode,
}: Props) {
  const [showPagoDialog, setShowPagoDialog] = useState(false);
  const [editingPagoId, setEditingPagoId] = useState<string | null>(null);
  const [dialogFecha, setDialogFecha] = useState(todayDate);
  const [dialogMonto, setDialogMonto] = useState("");
  const [dialogMetodo, setDialogMetodo] = useState("Efectivo");
  const [dialogNota, setDialogNota] = useState("");
  const [localError, setLocalError] = useState("");

  const montoNum = parseNum(montoTotal);
  const pagado = pagos.reduce((acc, p) => acc + parseNum(p.monto), 0);
  const saldo = montoNum - pagado;

  function openAddPago() {
    setEditingPagoId(null);
    setDialogFecha(todayDate);
    setDialogMonto("");
    setDialogMetodo("Efectivo");
    setDialogNota("");
    setShowPagoDialog(true);
  }

  function openEditPago(id: string) {
    const p = pagos.find((x) => x.id === id);
    if (!p) return;
    setEditingPagoId(id);
    setDialogFecha(p.fecha);
    setDialogMonto(p.monto);
    setDialogMetodo(p.metodoPago);
    setDialogNota(p.nota);
    setShowPagoDialog(true);
  }

  function savePagoDialog() {
    const total = parseNum(montoTotal);
    if (total <= 0) {
      setLocalError("Define primero un monto total válido.");
      return;
    }
    const monto = parseNum(dialogMonto);
    if (monto <= 0) {
      setLocalError("El pago debe ser mayor a 0.");
      return;
    }
    const others = pagos
      .filter((p) => p.id !== editingPagoId)
      .reduce((acc, p) => acc + parseNum(p.monto), 0);
    if (others + monto > total) {
      setLocalError("El pago no puede ser mayor al total.");
      return;
    }
    setLocalError("");
    const row: PagoDraft = {
      id: editingPagoId || crypto.randomUUID(),
      fecha: dialogFecha,
      monto: monto.toFixed(2),
      metodoPago: dialogMetodo,
      nota: dialogNota.trim(),
    };
    setPagos((prev) => {
      if (!editingPagoId) return [...prev, row];
      return prev.map((p) => (p.id === editingPagoId ? row : p));
    });
    setShowPagoDialog(false);
  }

  function deletePago(id: string) {
    setPagos((prev) => prev.filter((p) => p.id !== id));
    if (persistedPagoIds.has(id)) {
      setPagosDeleteIds((prev) => Array.from(new Set([...prev, id])));
    }
  }

  return (
    <section className="rounded-2xl border border-border bg-foreground/[0.04] p-5 space-y-5">
      <h3 className="font-heading text-sm font-bold text-primary">
        Resumen Financiero
      </h3>

      <label className="block">
        <span className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 ml-1">
          Costo Total del Servicio
        </span>
        <div className="relative mt-1">
          <span className="absolute left-4 top-1/2 -translate-y-1/2 font-bold text-muted-foreground/60">
            S/
          </span>
          <input
            value={montoTotal}
            onChange={(e) => setMontoTotal(e.target.value)}
            placeholder="0.00"
            className="w-full rounded-xl border border-border bg-card py-3 pl-10 pr-4 text-lg font-black text-foreground transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          />
        </div>
      </label>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground/70">
            Historial de Abonos
          </p>
          <button
            type="button"
            onClick={openAddPago}
            className="rounded-lg bg-primary/10 px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-primary transition-all hover:bg-primary/20 active:scale-95"
          >
            + Agregar
          </button>
        </div>

        <div className="space-y-2">
          {pagos.map((p) => (
            <div
              key={p.id}
              className="flex items-center justify-between rounded-xl border border-border bg-card p-3 shadow-sm transition-all hover:border-primary/30"
            >
              <div>
                <p className="text-sm font-bold text-foreground">
                  S/ {parseNum(p.monto).toFixed(2)}{" "}
                  <span className="ml-1 text-[10px] font-medium text-muted-foreground/60">
                    ({p.metodoPago})
                  </span>
                </p>
                <p className="text-[10px] font-medium text-muted-foreground/60">
                  {p.fecha}
                </p>
                {p.nota && (
                  <p className="mt-0.5 text-[10px] italic text-muted-foreground/80">
                    &quot;{p.nota}&quot;
                  </p>
                )}
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => openEditPago(p.id)}
                  className="text-[10px] font-bold text-primary hover:underline"
                >
                  Editar
                </button>
                <button
                  type="button"
                  onClick={() => deletePago(p.id)}
                  className="text-[10px] font-bold text-destructive hover:underline"
                >
                  Borrar
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-2xl bg-foreground/5 p-4 text-center">
        <p className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/60">
          Saldo Pendiente
        </p>
        <p
          className={
            "font-heading text-3xl font-black mt-1 " +
            (saldo > 0 ? "text-destructive" : "text-emerald-500")
          }
        >
          S/ {saldo.toFixed(2)}
        </p>
      </div>

      <label className="block">
        <span className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 ml-1">
          Estado de la Orden
        </span>
        <select
          value={estadoEntrega}
          onChange={(e) => setEstadoEntrega(e.target.value)}
          className="mt-1 w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-bold text-foreground transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
        >
          {ESTADO_ENTREGA.map((x) => (
            <option key={x} value={x}>
              {x}
            </option>
          ))}
        </select>
      </label>

      <button
        type="submit"
        disabled={isPending}
        className="w-full rounded-xl bg-primary px-6 py-4 font-heading text-base font-black text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:opacity-50"
      >
        {isPending
          ? "Guardando…"
          : mode === "edit"
            ? "💾 ACTUALIZAR ORDEN"
            : "🚀 CONFIRMAR ORDEN"}
      </button>

      {localError && (
        <p className="text-center text-xs font-bold text-destructive animate-bounce">
          ⚠️ {localError}
        </p>
      )}

      {showPagoDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div
            className="absolute inset-0"
            onClick={() => setShowPagoDialog(false)}
            role="presentation"
          />
          <div className="relative z-10 w-full max-w-md rounded-3xl border border-border bg-card p-6 text-foreground shadow-2xl transition-all">
            <h4 className="font-heading text-xl font-bold">
              {editingPagoId ? "Editar Abono" : "Agregar Abono"}
            </h4>

            <div className="mt-6 space-y-4">
              <label className="block">
                <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 ml-1">
                  Fecha del Abono
                </span>
                <input
                  type="date"
                  value={dialogFecha}
                  onChange={(e) => setDialogFecha(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium focus:border-primary focus:ring-1 focus:ring-primary outline-none"
                />
              </label>

              <label className="block">
                <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 ml-1">
                  Monto (S/)
                </span>
                <input
                  value={dialogMonto}
                  onChange={(e) => setDialogMonto(e.target.value)}
                  placeholder="0.00"
                  className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-bold focus:border-primary focus:ring-1 focus:ring-primary outline-none"
                />
              </label>

              <label className="block">
                <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 ml-1">
                  Método de Pago
                </span>
                <select
                  value={dialogMetodo}
                  onChange={(e) => setDialogMetodo(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium focus:border-primary focus:ring-1 focus:ring-primary outline-none"
                >
                  {METODO_PAGO.map((x) => (
                    <option key={x} value={x}>
                      {x}
                    </option>
                  ))}
                </select>
              </label>

              <label className="block">
                <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 ml-1">
                  Nota / Referencia
                </span>
                <input
                  value={dialogNota}
                  onChange={(e) => setDialogNota(e.target.value)}
                  placeholder="Ej. Operación #12345"
                  className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium focus:border-primary focus:ring-1 focus:ring-primary outline-none"
                />
              </label>
            </div>

            <div className="mt-8 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setShowPagoDialog(false)}
                className="rounded-xl border border-border bg-card px-5 py-2.5 text-sm font-bold text-foreground transition-all hover:bg-foreground/5"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={savePagoDialog}
                className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-105 active:scale-95"
              >
                Guardar Abono
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
