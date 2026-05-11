"use client";

import Link from "next/link";
import { useActionState, useMemo, useState } from "react";

type SaveAction = (
  prevState: { error?: string } | null,
  formData: FormData
) => Promise<{ error?: string } | null>;
type PacienteOption = { id: string; nombre_completo: string | null };
type PagoDraft = {
  id: string;
  fecha: string;
  monto: string;
  metodoPago: string;
  nota: string;
};

type Props = {
  mode: "create" | "edit";
  servicioId?: string;
  backHref: string;
  returnTo: string;
  errorBase: string;
  opticaLine: string;
  todayDate: string;
  pacientes: PacienteOption[];
  initial: {
    ot: string;
    descripcion: string;
    montoTotal: string;
    estado: string;
    fecha: string;
    pacienteId: string;
    pagos: PagoDraft[];
  };
  saveAction: SaveAction;
};

const METODO_PAGO = ["Efectivo", "Tarjeta", "Transferencia"] as const;
const ESTADO = ["Pendiente", "Entregado"] as const;

function parseNum(raw: string): number {
  const n = Number(raw.replace(",", "."));
  return Number.isFinite(n) ? n : 0;
}

export function ServicioForm({
  mode,
  servicioId,
  backHref,
  returnTo,
  errorBase,
  opticaLine,
  todayDate,
  pacientes,
  initial,
  saveAction
}: Props) {
  const [actionState, action, isPending] = useActionState(saveAction, null);

  const [ot, setOt] = useState(initial.ot);
  const [descripcion, setDescripcion] = useState(initial.descripcion);
  const [montoTotal, setMontoTotal] = useState(initial.montoTotal);
  const [estado, setEstado] = useState(initial.estado || "Pendiente");
  const [fecha, setFecha] = useState(initial.fecha || todayDate);
  const [pacienteId, setPacienteId] = useState(initial.pacienteId);
  const [pagos, setPagos] = useState<PagoDraft[]>(initial.pagos);
  const [pagosDeleteIds, setPagosDeleteIds] = useState<string[]>([]);
  const persistedPagoIds = useMemo(
    () => new Set(initial.pagos.map((p) => p.id).filter(Boolean)),
    [initial.pagos]
  );
  const [pacienteSearch, setPacienteSearch] = useState("");
  const [showPagoDialog, setShowPagoDialog] = useState(false);
  const [editingPagoId, setEditingPagoId] = useState<string | null>(null);
  const [dialogFecha, setDialogFecha] = useState(todayDate);
  const [dialogMonto, setDialogMonto] = useState("");
  const [dialogMetodo, setDialogMetodo] = useState("Efectivo");
  const [dialogNota, setDialogNota] = useState("");
  const [localError, setLocalError] = useState("");

  const total = parseNum(montoTotal);
  const pagado = useMemo(() => pagos.reduce((acc, p) => acc + parseNum(p.monto), 0), [pagos]);
  const saldo = total - pagado;
  const filteredPacientes = useMemo(() => {
    const q = pacienteSearch.trim().toLowerCase();
    if (!q) return pacientes;
    return pacientes.filter((p) => (p.nombre_completo ?? "").toLowerCase().includes(q));
  }, [pacienteSearch, pacientes]);

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
      .filter((x) => x.id !== editingPagoId)
      .reduce((acc, p) => acc + parseNum(p.monto), 0);
    if (others + monto > total) {
      setLocalError("El pago no puede ser mayor al total.");
      return;
    }
    setLocalError("");
    const payload: PagoDraft = {
      id: editingPagoId || crypto.randomUUID(),
      fecha: dialogFecha,
      monto: monto.toFixed(2),
      metodoPago: dialogMetodo,
      nota: dialogNota.trim()
    };
    setPagos((prev) => {
      if (!editingPagoId) return [...prev, payload];
      return prev.map((x) => (x.id === editingPagoId ? payload : x));
    });
    setShowPagoDialog(false);
  }

  function deletePago(id: string) {
    setPagos((prev) => prev.filter((x) => x.id !== id));
    if (persistedPagoIds.has(id)) {
      setPagosDeleteIds((prev) => Array.from(new Set([...prev, id])));
    }
  }

  return (
    <form action={action} className="space-y-4">
      {actionState?.error && (
        <p className="mb-3 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {actionState.error}
        </p>
      )}
      <input type="hidden" name="servicioId" value={servicioId ?? ""} />
      <input type="hidden" name="ot" value={ot} />
      <input type="hidden" name="descripcion" value={descripcion} />
      <input type="hidden" name="montoTotal" value={montoTotal} />
      <input type="hidden" name="estado" value={estado} />
      <input type="hidden" name="fecha" value={fecha} />
      <input type="hidden" name="pacienteId" value={pacienteId} />
      <input type="hidden" name="pagosJson" value={JSON.stringify(pagos)} />
      <input type="hidden" name="pagosDeleteJson" value={JSON.stringify(pagosDeleteIds)} />
      <input type="hidden" name="returnTo" value={returnTo} />
      <input type="hidden" name="errorBase" value={errorBase} />

      <div className="rounded-2xl border border-border bg-card text-foreground shadow-xl overflow-hidden">
        <div className="border-b border-border px-4 py-2 text-[11px] font-black uppercase tracking-widest text-primary/70">{opticaLine}</div>
        <div className="flex items-center gap-3 bg-primary px-4 py-4 text-primary-foreground shadow-md">
          <Link href={backHref} className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 hover:bg-white/20 transition-all active:scale-95">
            <span className="text-xl font-bold">{`←`}</span>
          </Link>
          <h2 className="flex-1 font-heading text-xl font-bold">{mode === "edit" ? "Editar Servicio" : "Nuevo Servicio"}</h2>
          <button type="submit" disabled={isPending} className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 hover:bg-white/20 transition-all active:scale-95 disabled:opacity-50">
            {isPending ? "…" : "💾"}
          </button>
        </div>

        <div className="space-y-6 px-5 py-6">
          <div className="space-y-4">
            <input value={ot} onChange={(e) => setOt(e.target.value)} placeholder="N° Orden (OT) - Opcional" className="w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none" />
            <input value={descripcion} onChange={(e) => setDescripcion(e.target.value)} placeholder="Descripción del servicio" className="w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none" />
            <div className="relative">
              <span className="absolute left-4 top-1/2 -translate-y-1/2 font-bold text-muted-foreground/50">S/</span>
              <input value={montoTotal} onChange={(e) => setMontoTotal(e.target.value)} placeholder="Costo Total" className="w-full rounded-xl border border-border bg-foreground/[0.03] py-3 pl-10 pr-4 text-base font-bold transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none" />
            </div>
          </div>

          <div className="space-y-4 rounded-2xl border border-border bg-foreground/[0.02] p-5">
            <div className="flex items-center justify-between">
              <p className="font-heading text-sm font-bold text-primary">Historial de Abonos</p>
              <button type="button" onClick={openAddPago} className="rounded-lg bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary hover:bg-primary/20 transition-all">
                + Agregar
              </button>
            </div>
            
            <div className="space-y-2">
              {pagos.map((p) => (
                <div key={p.id} className="flex items-center justify-between rounded-xl border border-border bg-card p-3 shadow-sm">
                  <div>
                    <p className="text-sm font-bold">S/ {parseNum(p.monto).toFixed(2)} <span className="text-[10px] font-medium text-muted-foreground/60">({p.metodoPago})</span></p>
                    <p className="text-[10px] text-muted-foreground/60">{p.fecha}</p>
                  </div>
                  <div className="flex gap-2">
                    <button type="button" onClick={() => openEditPago(p.id)} className="text-[10px] font-bold text-primary hover:underline">Editar</button>
                    <button type="button" onClick={() => deletePago(p.id)} className="text-[10px] font-bold text-destructive hover:underline">Borrar</button>
                  </div>
                </div>
              ))}
            </div>

            <div className="rounded-xl bg-background p-4 text-center">
              <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">Saldo Pendiente</p>
              <p className={"font-heading text-3xl font-black mt-1 " + (saldo > 0 ? "text-destructive" : "text-emerald-500")}>
                S/ {saldo.toFixed(2)}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <label className="block">
              <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/50 ml-1">Estado</span>
              <select value={estado} onChange={(e) => setEstado(e.target.value)} className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm font-bold outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                {ESTADO.map((x) => <option key={x} value={x}>{x}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/50 ml-1">Fecha</span>
              <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm font-bold outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </label>
          </div>

          <div className="space-y-3 rounded-2xl border border-border bg-background p-5">
            <p className="font-heading text-sm font-bold text-primary">Asociar a Paciente</p>
            <input
              value={pacienteSearch}
              onChange={(e) => setPacienteSearch(e.target.value)}
              placeholder="Buscar paciente por nombre..."
              className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm font-medium outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            />
            <select value={pacienteId} onChange={(e) => setPacienteId(e.target.value)} className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm font-medium outline-none focus:border-primary focus:ring-1 focus:ring-primary">
              <option value="">Ninguno</option>
              {filteredPacientes.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nombre_completo ?? p.id}
                </option>
              ))}
            </select>
          </div>

          <button type="submit" disabled={isPending} className="w-full rounded-xl bg-primary px-6 py-4 font-heading text-base font-black text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:opacity-50">
            {isPending ? "Guardando…" : mode === "edit" ? "💾 ACTUALIZAR SERVICIO" : "🚀 GUARDAR SERVICIO"}
          </button>
          {localError && <p className="text-center text-xs font-bold text-destructive animate-bounce">⚠️ {localError}</p>}
        </div>
      </div>

      {showPagoDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="absolute inset-0" onClick={() => setShowPagoDialog(false)} role="presentation" />
          <div className="relative z-10 w-full max-w-md rounded-xl border border-zinc-700 bg-zinc-900 p-4 text-zinc-100 space-y-3">
            <h4 className="font-semibold">{editingPagoId ? "Editar Abono" : "Agregar Abono"}</h4>
            <label className="block text-sm">Fecha del abono
              <input type="date" value={dialogFecha} onChange={(e) => setDialogFecha(e.target.value)} className="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2" />
            </label>
            <label className="block text-sm">Monto
              <input value={dialogMonto} onChange={(e) => setDialogMonto(e.target.value)} className="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2" />
            </label>
            <label className="block text-sm">Metodo de pago
              <select value={dialogMetodo} onChange={(e) => setDialogMetodo(e.target.value)} className="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2">
                {METODO_PAGO.map((x) => <option key={x} value={x}>{x}</option>)}
              </select>
            </label>
            <label className="block text-sm">Nota
              <input value={dialogNota} onChange={(e) => setDialogNota(e.target.value)} className="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2" />
            </label>
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setShowPagoDialog(false)} className="rounded-md border border-zinc-600 px-3 py-1.5 text-sm">Cancelar</button>
              <button type="button" onClick={savePagoDialog} className="rounded-md bg-sky-400 px-3 py-1.5 text-sm font-medium text-zinc-900">Guardar</button>
            </div>
          </div>
        </div>
      )}
    </form>
  );
}
