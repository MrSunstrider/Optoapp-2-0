"use client";

import Link from "next/link";
import { useMemo, useState } from "react";

type SaveAction = (formData: FormData) => void | Promise<void>;
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
    <form action={saveAction} className="space-y-4">
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

      <div className="rounded-xl border border-zinc-800 bg-[#121212] text-zinc-100">
        <div className="border-b border-zinc-800 px-3 py-2 text-[13px] text-sky-400">{opticaLine}</div>
        <div className="flex items-center gap-2 bg-zinc-900 px-2 py-2 text-zinc-100">
          <Link href={backHref} className="flex h-10 w-10 items-center justify-center rounded-lg hover:bg-white/10">
            <span className="text-xl">{`<`}</span>
          </Link>
          <h2 className="flex-1 text-lg font-bold">{mode === "edit" ? "Editar Servicio" : "Nuevo Servicio"}</h2>
          <button type="submit" className="flex h-10 w-10 items-center justify-center rounded-lg text-xl hover:bg-white/10">
            ✓
          </button>
        </div>

        <div className="space-y-4 px-4 py-4">
          <input value={ot} onChange={(e) => setOt(e.target.value)} placeholder="OT (Opcional)" className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2" />
          <input value={descripcion} onChange={(e) => setDescripcion(e.target.value)} placeholder="Descripcion" className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2" />
          <input value={montoTotal} onChange={(e) => setMontoTotal(e.target.value)} placeholder="Monto Total" className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2" />

          <hr className="border-zinc-800" />
          <p className="font-semibold text-sky-300">Historial de Abonos</p>
          {pagos.map((p) => (
            <div key={p.id} className="flex items-start justify-between rounded-lg border border-zinc-700 bg-zinc-900/60 p-2">
              <div>
                <p className="text-sm font-medium">{p.metodoPago}: s/. {parseNum(p.monto).toFixed(2)}</p>
                {p.nota && <p className="text-xs text-zinc-400">{p.nota}</p>}
                <p className="text-xs text-zinc-500">{p.fecha}</p>
              </div>
              <div className="flex gap-2">
                <button type="button" onClick={() => openEditPago(p.id)} className="text-xs text-sky-400">Editar</button>
                <button type="button" onClick={() => deletePago(p.id)} className="text-xs text-red-400">Borrar</button>
              </div>
            </div>
          ))}
          <button type="button" onClick={openAddPago} className="w-full rounded-full border border-zinc-600 py-2 text-sm">
            + Agregar Abono
          </button>

          <div className="rounded-xl border border-zinc-700 bg-zinc-800/40 p-3">
            <p className="text-sm font-semibold text-zinc-200">Saldo Restante:</p>
            <p className={"text-4xl font-extrabold " + (saldo > 0 ? "text-red-400" : "text-emerald-400")}>
              s/. {saldo.toFixed(2)}
            </p>
          </div>

          <select value={estado} onChange={(e) => setEstado(e.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2">
            {ESTADO.map((x) => <option key={x} value={x}>{x}</option>)}
          </select>
          <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2" />

          <div className="space-y-2 rounded-xl border border-zinc-700 bg-zinc-800/40 p-3">
            <p className="font-semibold text-sky-300">Asociar a Paciente (Opcional)</p>
            <input
              value={pacienteSearch}
              onChange={(e) => setPacienteSearch(e.target.value)}
              placeholder="Buscar paciente..."
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2"
            />
            <select value={pacienteId} onChange={(e) => setPacienteId(e.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2">
              <option value="">Ninguno</option>
              {filteredPacientes.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nombre_completo ?? p.id}
                </option>
              ))}
            </select>
          </div>

          <button type="submit" className="w-full rounded-lg bg-sky-400 px-4 py-2.5 font-semibold text-zinc-900">
            {mode === "edit" ? "Actualizar Servicio" : "Guardar Servicio"}
          </button>
          {localError && <p className="text-sm text-red-400">{localError}</p>}
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
