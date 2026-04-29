"use client";

import Link from "next/link";
import { useMemo, useState } from "react";

type SaveAction = (formData: FormData) => void | Promise<void>;

type MonturaOption = {
  id: string;
  sku: string | null;
  marca: string | null;
  modelo: string | null;
  stock_actual: number | null;
  activo: boolean | null;
};

type PagoDraft = {
  id: string;
  fecha: string;
  monto: string;
  metodoPago: string;
  nota: string;
};

type Props = {
  pacienteId: string;
  dispensacionId?: string;
  mode: "create" | "edit";
  backHref: string;
  opticaLine: string;
  todayDate: string;
  initial: {
    fecha: string;
    ot: string;
    tipoLente: string;
    subTipoBifocal: string;
    distanciaLente: string;
    altura: string;
    materialLente: string;
    tratamientos: string[];
    colorLente: string;
    notasDiseno: string;
    origenMontura: string;
    monturaId: string;
    previousMonturaId: string;
    tipoAro: string;
    materialMontura: string;
    descripcionMontura: string;
    montoTotal: string;
    estadoEntrega: string;
    pagos: PagoDraft[];
  };
  monturas: MonturaOption[];
  saveAction: SaveAction;
};

const TIPO_LENTE = ["Monofocal", "Bifocal", "Progresivo", "Ocupacional"] as const;
const SUB_BIFOCAL = ["Flaptop", "Invisible"] as const;
const DISTANCIA = ["Lejos", "Intermedia", "Cerca"] as const;
const MATERIAL_LENTE = ["Resina", "Policarbonato", "Cristal", "Trivex"] as const;
const ORIGEN_MONTURA = ["Tienda", "Paciente"] as const;
const TIPO_ARO = ["Aro Completo", "Semi al aire", "Al aire"] as const;
const MATERIAL_MONTURA = ["Acetato", "Metal", "Carey", "Econ"] as const;
const METODO_PAGO = ["Efectivo", "Tarjeta", "Transferencia"] as const;
const ESTADO_ENTREGA = ["Pendiente", "Entregado"] as const;
const TRATAMIENTOS = [
  "Ninguno",
  "Antireflejo",
  "Antirayas",
  "Filtro UV 400",
  "Fotocromático",
  "AR Blue Defense"
] as const;

function parseNum(raw: string): number {
  const n = Number(raw.replace(",", "."));
  return Number.isFinite(n) ? n : 0;
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
  saveAction
}: Props) {
  const [fecha, setFecha] = useState(initial.fecha || todayDate);
  const [ot, setOt] = useState(initial.ot);
  const [tipoLente, setTipoLente] = useState(initial.tipoLente);
  const [subTipoBifocal, setSubTipoBifocal] = useState(initial.subTipoBifocal);
  const [distanciaLente, setDistanciaLente] = useState(initial.distanciaLente);
  const [altura, setAltura] = useState(initial.altura);
  const [materialLente, setMaterialLente] = useState(initial.materialLente);
  const [tratamientos, setTratamientos] = useState<string[]>(
    initial.tratamientos.length > 0 ? initial.tratamientos : []
  );
  const [colorLente, setColorLente] = useState(initial.colorLente);
  const [notasDiseno, setNotasDiseno] = useState(initial.notasDiseno);
  const [origenMontura, setOrigenMontura] = useState(initial.origenMontura || "Tienda");
  const [monturaId, setMonturaId] = useState(initial.monturaId);
  const [tipoAro, setTipoAro] = useState(initial.tipoAro);
  const [materialMontura, setMaterialMontura] = useState(initial.materialMontura);
  const [descripcionMontura, setDescripcionMontura] = useState(initial.descripcionMontura);
  const [montoTotal, setMontoTotal] = useState(initial.montoTotal);
  const [estadoEntrega, setEstadoEntrega] = useState(initial.estadoEntrega || "Pendiente");
  const [pagos, setPagos] = useState<PagoDraft[]>(initial.pagos);
  const [pagosDeleteIds, setPagosDeleteIds] = useState<string[]>([]);
  const persistedPagoIds = useMemo(
    () => new Set(initial.pagos.map((p) => p.id).filter(Boolean)),
    [initial.pagos]
  );

  const [showPagoDialog, setShowPagoDialog] = useState(false);
  const [editingPagoId, setEditingPagoId] = useState<string | null>(null);
  const [dialogFecha, setDialogFecha] = useState(todayDate);
  const [dialogMonto, setDialogMonto] = useState("");
  const [dialogMetodo, setDialogMetodo] = useState("Efectivo");
  const [dialogNota, setDialogNota] = useState("");
  const [localError, setLocalError] = useState("");

  const montoNum = parseNum(montoTotal);
  const pagado = useMemo(() => pagos.reduce((acc, p) => acc + parseNum(p.monto), 0), [pagos]);
  const saldo = montoNum - pagado;
  const requiresAltura =
    tipoLente === "Bifocal" || tipoLente === "Progresivo" || tipoLente === "Ocupacional";

  const monturasDisponibles = useMemo(
    () =>
      monturas.filter((m) => (m.activo ?? true) && ((m.stock_actual ?? 0) > 0 || m.id === monturaId)),
    [monturaId, monturas]
  );

  const tratamientosUi = useMemo(() => {
    const arr = [...tratamientos];
    if (arr.length === 0 || arr[arr.length - 1] !== "Ninguno") arr.push("Ninguno");
    return arr;
  }, [tratamientos]);

  async function suggestOt() {
    const res = await fetch(`/api/dispensaciones/suggest-ot?fecha=${encodeURIComponent(fecha)}`, {
      cache: "no-store"
    });
    if (!res.ok) return;
    const data = (await res.json()) as { ot?: string };
    if (data.ot) setOt(data.ot);
  }

  function onTipoLenteChange(next: string) {
    setTipoLente(next);
    if (next !== "Bifocal") setSubTipoBifocal("");
    if (next !== "Monofocal") setDistanciaLente("");
    if (!(next === "Bifocal" || next === "Progresivo" || next === "Ocupacional")) setAltura("");
  }

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
      nota: dialogNota.trim()
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
    <form action={saveAction} className="space-y-4">
      <input type="hidden" name="pacienteId" value={pacienteId} />
      <input type="hidden" name="dispensacionId" value={dispensacionId ?? ""} />
      <input type="hidden" name="previousMonturaId" value={initial.previousMonturaId} />
      <input type="hidden" name="fecha" value={fecha} />
      <input type="hidden" name="ot" value={ot} />
      <input type="hidden" name="tipoLente" value={tipoLente} />
      <input type="hidden" name="subTipoBifocal" value={subTipoBifocal} />
      <input type="hidden" name="distanciaLente" value={distanciaLente} />
      <input type="hidden" name="altura" value={altura} />
      <input type="hidden" name="materialLente" value={materialLente} />
      <input type="hidden" name="tratamientosJson" value={JSON.stringify(tratamientos)} />
      <input type="hidden" name="colorLente" value={colorLente} />
      <input type="hidden" name="notasDiseno" value={notasDiseno} />
      <input type="hidden" name="origenMontura" value={origenMontura} />
      <input type="hidden" name="monturaId" value={monturaId} />
      <input type="hidden" name="tipoAro" value={tipoAro} />
      <input type="hidden" name="materialMontura" value={materialMontura} />
      <input type="hidden" name="descripcionMontura" value={descripcionMontura} />
      <input type="hidden" name="montoTotal" value={montoTotal} />
      <input type="hidden" name="estadoEntrega" value={estadoEntrega} />
      <input type="hidden" name="pagosJson" value={JSON.stringify(pagos)} />
      <input type="hidden" name="pagosDeleteJson" value={JSON.stringify(pagosDeleteIds)} />

      <div className="rounded-xl border border-zinc-800 bg-[#121212] text-zinc-100">
        <div className="border-b border-zinc-800 px-3 py-2 text-[13px] text-sky-400">{opticaLine}</div>
        <div className="flex items-center gap-2 bg-zinc-900 px-2 py-2 text-zinc-100">
          <Link href={backHref} className="flex h-10 w-10 items-center justify-center rounded-lg hover:bg-white/10">
            <span className="text-xl">{`<`}</span>
          </Link>
          <h2 className="flex-1 text-lg font-bold">
            {mode === "edit" ? "Editar Dispensacion" : "Nueva Dispensacion"}
          </h2>
          <button type="submit" className="flex h-10 w-10 items-center justify-center rounded-lg text-xl hover:bg-white/10">
            ✓
          </button>
        </div>

        <div className="space-y-4 px-4 py-4">
          <button type="button" className="w-full rounded-full border border-zinc-700 py-2 text-sm" onClick={() => {}}>
            Fecha: {new Date(fecha + "T12:00:00").toLocaleDateString("es-PE", { day: "2-digit", month: "short", year: "numeric" })}
          </button>
          <input
            type="date"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2 text-sm"
          />
          <div className="flex items-center gap-2">
            <input
              value={ot}
              onChange={(e) => setOt(e.target.value)}
              placeholder="N° OT (OT-AAAA-####)"
              className="flex-1 rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2"
            />
            <button type="button" onClick={suggestOt} className="text-sm text-sky-400">
              Sugerir OT
            </button>
          </div>

          <section className="rounded-xl border border-zinc-700 bg-zinc-800/40 p-3 space-y-2">
            <h3 className="font-semibold text-sky-300">Informacion del Lente</h3>
            <select value={tipoLente} onChange={(e) => onTipoLenteChange(e.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2">
              <option value="">Tipo de Lente</option>
              {TIPO_LENTE.map((x) => <option key={x} value={x}>{x}</option>)}
            </select>
            {tipoLente === "Bifocal" && (
              <select value={subTipoBifocal} onChange={(e) => setSubTipoBifocal(e.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2">
                <option value="">Sub-tipo Bifocal</option>
                {SUB_BIFOCAL.map((x) => <option key={x} value={x}>{x}</option>)}
              </select>
            )}
            {tipoLente === "Monofocal" && (
              <select value={distanciaLente} onChange={(e) => setDistanciaLente(e.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2">
                <option value="">Distancia</option>
                {DISTANCIA.map((x) => <option key={x} value={x}>{x}</option>)}
              </select>
            )}
            {requiresAltura && (
              <input value={altura} onChange={(e) => setAltura(e.target.value)} placeholder="Altura (mm)" className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2" />
            )}
            <select value={materialLente} onChange={(e) => setMaterialLente(e.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2">
              <option value="">Material</option>
              {MATERIAL_LENTE.map((x) => <option key={x} value={x}>{x}</option>)}
            </select>
            <p className="text-sm text-zinc-300">Tratamientos</p>
            {tratamientosUi.map((value, idx) => (
              <select
                key={`${idx}-${value}`}
                value={value}
                onChange={(e) => {
                  const selected = e.target.value;
                  setTratamientos((prev) => {
                    const arr = [...prev];
                    if (idx < arr.length) {
                      if (selected === "Ninguno") arr.splice(idx, 1);
                      else arr[idx] = selected;
                    } else if (selected !== "Ninguno") {
                      arr.push(selected);
                    }
                    return Array.from(new Set(arr.filter((x) => x !== "Ninguno")));
                  });
                }}
                className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2"
              >
                {TRATAMIENTOS.map((x) => <option key={x} value={x}>{x}</option>)}
              </select>
            ))}
            <input value={colorLente} onChange={(e) => setColorLente(e.target.value)} placeholder="Color" className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2" />
            <textarea value={notasDiseno} onChange={(e) => setNotasDiseno(e.target.value)} placeholder="Notas de Diseño" rows={2} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2" />
          </section>

          <section className="rounded-xl border border-zinc-700 bg-zinc-800/40 p-3 space-y-2">
            <h3 className="font-semibold text-sky-300">Informacion de Montura</h3>
            <select value={origenMontura} onChange={(e) => { setOrigenMontura(e.target.value); if (e.target.value !== "Tienda") setMonturaId(""); }} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2">
              {ORIGEN_MONTURA.map((x) => <option key={x} value={x}>{x}</option>)}
            </select>
            {origenMontura === "Tienda" && (
              <select
                value={monturaId}
                onChange={(e) => {
                  const id = e.target.value;
                  setMonturaId(id);
                  const m = monturasDisponibles.find((x) => x.id === id);
                  if (m) setDescripcionMontura(`${m.marca ?? ""} ${m.modelo ?? ""}`.trim());
                }}
                className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2"
              >
                <option value="">Montura de inventario</option>
                {monturasDisponibles.map((m) => (
                  <option key={m.id} value={m.id}>
                    {(m.sku ?? m.id)} - {(m.marca ?? "").trim()} {(m.modelo ?? "").trim()} (Stock:{" "}
                    {m.stock_actual ?? 0})
                  </option>
                ))}
              </select>
            )}
            <select value={tipoAro} onChange={(e) => setTipoAro(e.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2">
              <option value="">Tipo de Aro</option>
              {TIPO_ARO.map((x) => <option key={x} value={x}>{x}</option>)}
            </select>
            <select value={materialMontura} onChange={(e) => setMaterialMontura(e.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2">
              <option value="">Material</option>
              {MATERIAL_MONTURA.map((x) => <option key={x} value={x}>{x}</option>)}
            </select>
            <input value={descripcionMontura} onChange={(e) => setDescripcionMontura(e.target.value)} placeholder="Descripcion (Marca, Modelo)" className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2" />
          </section>

          <section className="rounded-xl border border-zinc-700 bg-zinc-800/40 p-3 space-y-3">
            <h3 className="font-semibold text-sky-300">Informacion Financiera</h3>
            <input value={montoTotal} onChange={(e) => setMontoTotal(e.target.value)} placeholder="Monto Total" className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2" />
            <hr className="border-zinc-700" />
            <p className="font-semibold text-zinc-200">Historial de Abonos</p>
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
            <button type="button" onClick={openAddPago} className="w-full rounded-lg border border-zinc-600 py-2 text-sm">
              Agregar Abono
            </button>

            <div className="text-center">
              <p className="text-xs text-zinc-400">SALDO RESTANTE</p>
              <p className={"text-3xl font-extrabold " + (saldo > 0 ? "text-red-400" : "text-emerald-400")}>
                s/. {saldo.toFixed(2)}
              </p>
            </div>
            <select value={estadoEntrega} onChange={(e) => setEstadoEntrega(e.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2">
              {ESTADO_ENTREGA.map((x) => <option key={x} value={x}>{x}</option>)}
            </select>
            <button type="submit" className="w-full rounded-lg bg-sky-400 px-4 py-2.5 font-semibold text-zinc-900">
              {mode === "edit" ? "Actualizar Orden" : "Confirmar Orden"}
            </button>
            {localError && <p className="text-sm text-red-400">{localError}</p>}
          </section>
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
