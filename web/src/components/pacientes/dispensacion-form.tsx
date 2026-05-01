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

      <div className="rounded-2xl border border-border bg-card text-foreground shadow-xl overflow-hidden">
        <div className="border-b border-border px-4 py-2 text-[11px] font-black uppercase tracking-widest text-primary/70">{opticaLine}</div>
        <div className="flex items-center gap-3 bg-primary px-4 py-4 text-primary-foreground shadow-md">
          <Link href={backHref} className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 hover:bg-white/20 transition-all active:scale-95">
            <span className="text-xl font-bold">{`←`}</span>
          </Link>
          <h2 className="flex-1 font-heading text-xl font-bold">
            {mode === "edit" ? "Editar Dispensación" : "Nueva Dispensación"}
          </h2>
          <button type="submit" className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 hover:bg-white/20 transition-all active:scale-95">
            💾
          </button>
        </div>

        <div className="space-y-6 px-5 py-6">
          <div className="space-y-4">
            <label className="block">
              <span className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 ml-1">Fecha de Orden</span>
              <input
                type="date"
                value={fecha}
                onChange={(e) => setFecha(e.target.value)}
                className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium text-foreground transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
              />
            </label>
            <div className="flex items-end gap-3">
              <label className="flex-1">
                <span className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 ml-1">N° Orden (OT)</span>
                <input
                  value={ot}
                  onChange={(e) => setOt(e.target.value)}
                  placeholder="OT-AAAA-####"
                  className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium text-foreground transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
                />
              </label>
              <button type="button" onClick={suggestOt} className="h-[46px] rounded-xl border border-primary/20 bg-primary/5 px-4 text-xs font-bold text-primary transition-all hover:bg-primary/10 active:scale-95">
                Sugerir OT
              </button>
            </div>
          </div>

          <section className="rounded-2xl border border-border bg-foreground/[0.02] p-5 space-y-4">
            <h3 className="font-heading text-sm font-bold text-primary">Información del Lente</h3>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <select value={tipoLente} onChange={(e) => onTipoLenteChange(e.target.value)} className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none">
                <option value="">Tipo de Lente</option>
                {TIPO_LENTE.map((x) => <option key={x} value={x}>{x}</option>)}
              </select>
              {tipoLente === "Bifocal" && (
                <select value={subTipoBifocal} onChange={(e) => setSubTipoBifocal(e.target.value)} className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none">
                  <option value="">Sub-tipo Bifocal</option>
                  {SUB_BIFOCAL.map((x) => <option key={x} value={x}>{x}</option>)}
                </select>
              )}
              {tipoLente === "Monofocal" && (
                <select value={distanciaLente} onChange={(e) => setDistanciaLente(e.target.value)} className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none">
                  <option value="">Distancia</option>
                  {DISTANCIA.map((x) => <option key={x} value={x}>{x}</option>)}
                </select>
              )}
              {requiresAltura && (
                <input value={altura} onChange={(e) => setAltura(e.target.value)} placeholder="Altura (mm)" className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none" />
              )}
              <select value={materialLente} onChange={(e) => setMaterialLente(e.target.value)} className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none">
                <option value="">Material</option>
                {MATERIAL_LENTE.map((x) => <option key={x} value={x}>{x}</option>)}
              </select>
            </div>
            
            <div className="space-y-3">
              <p className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 ml-1">Tratamientos Aplicados</p>
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
                  className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
                >
                  {TRATAMIENTOS.map((x) => <option key={x} value={x}>{x}</option>)}
                </select>
              ))}
            </div>
            <input value={colorLente} onChange={(e) => setColorLente(e.target.value)} placeholder="Color del lente" className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none" />
            <textarea value={notasDiseno} onChange={(e) => setNotasDiseno(e.target.value)} placeholder="Notas de diseño adicionales..." rows={2} className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none resize-none" />
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
            <input value={descripcionMontura} onChange={(e) => setDescripcionMontura(e.target.value)} placeholder="Descripción (Marca, Modelo)" className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none" />
          </section>

          <section className="rounded-2xl border border-border bg-foreground/[0.04] p-5 space-y-5">
            <h3 className="font-heading text-sm font-bold text-primary">Resumen Financiero</h3>
            <label className="block">
              <span className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 ml-1">Costo Total del Servicio</span>
              <div className="relative mt-1">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 font-bold text-muted-foreground/60">S/</span>
                <input value={montoTotal} onChange={(e) => setMontoTotal(e.target.value)} placeholder="0.00" className="w-full rounded-xl border border-border bg-card py-3 pl-10 pr-4 text-lg font-black text-foreground transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none" />
              </div>
            </label>
            
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground/70">Historial de Abonos</p>
                <button type="button" onClick={openAddPago} className="rounded-lg bg-primary/10 px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-primary transition-all hover:bg-primary/20 active:scale-95">
                  + Agregar
                </button>
              </div>
              
              <div className="space-y-2">
                {pagos.map((p) => (
                  <div key={p.id} className="flex items-center justify-between rounded-xl border border-border bg-card p-3 shadow-sm transition-all hover:border-primary/30">
                    <div>
                      <p className="text-sm font-bold text-foreground">S/ {parseNum(p.monto).toFixed(2)} <span className="ml-1 text-[10px] font-medium text-muted-foreground/60">({p.metodoPago})</span></p>
                      <p className="text-[10px] font-medium text-muted-foreground/60">{p.fecha}</p>
                      {p.nota && <p className="mt-0.5 text-[10px] italic text-muted-foreground/80">"{p.nota}"</p>}
                    </div>
                    <div className="flex gap-2">
                      <button type="button" onClick={() => openEditPago(p.id)} className="text-[10px] font-bold text-primary hover:underline">Editar</button>
                      <button type="button" onClick={() => deletePago(p.id)} className="text-[10px] font-bold text-destructive hover:underline">Borrar</button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="rounded-2xl bg-foreground/5 p-4 text-center">
              <p className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/60">Saldo Pendiente</p>
              <p className={"font-heading text-3xl font-black mt-1 " + (saldo > 0 ? "text-destructive" : "text-emerald-500")}>
                S/ {saldo.toFixed(2)}
              </p>
            </div>

            <label className="block">
              <span className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 ml-1">Estado de la Orden</span>
              <select value={estadoEntrega} onChange={(e) => setEstadoEntrega(e.target.value)} className="mt-1 w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-bold text-foreground transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none">
                {ESTADO_ENTREGA.map((x) => <option key={x} value={x}>{x}</option>)}
              </select>
            </label>

            <button type="submit" className="w-full rounded-xl bg-primary px-6 py-4 font-heading text-base font-black text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95">
              {mode === "edit" ? "💾 ACTUALIZAR ORDEN" : "🚀 CONFIRMAR ORDEN"}
            </button>
            {localError && <p className="text-center text-xs font-bold text-destructive animate-bounce">⚠️ {localError}</p>}
          </section>
        </div>
      </div>

      {showPagoDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="absolute inset-0" onClick={() => setShowPagoDialog(false)} role="presentation" />
          <div className="relative z-10 w-full max-w-md rounded-3xl border border-border bg-card p-6 text-foreground shadow-2xl transition-all">
            <h4 className="font-heading text-xl font-bold">{editingPagoId ? "Editar Abono" : "Agregar Abono"}</h4>
            
            <div className="mt-6 space-y-4">
              <label className="block">
                <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 ml-1">Fecha del Abono</span>
                <input type="date" value={dialogFecha} onChange={(e) => setDialogFecha(e.target.value)} className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium focus:border-primary focus:ring-1 focus:ring-primary outline-none" />
              </label>
              <label className="block">
                <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 ml-1">Monto (S/)</span>
                <input value={dialogMonto} onChange={(e) => setDialogMonto(e.target.value)} placeholder="0.00" className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-bold focus:border-primary focus:ring-1 focus:ring-primary outline-none" />
              </label>
              <label className="block">
                <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 ml-1">Método de Pago</span>
                <select value={dialogMetodo} onChange={(e) => setDialogMetodo(e.target.value)} className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium focus:border-primary focus:ring-1 focus:ring-primary outline-none">
                  {METODO_PAGO.map((x) => <option key={x} value={x}>{x}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 ml-1">Nota / Referencia</span>
                <input value={dialogNota} onChange={(e) => setDialogNota(e.target.value)} placeholder="Ej. Operación #12345" className="mt-1 w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium focus:border-primary focus:ring-1 focus:ring-primary outline-none" />
              </label>
            </div>

            <div className="mt-8 flex justify-end gap-3">
              <button type="button" onClick={() => setShowPagoDialog(false)} className="rounded-xl border border-border bg-card px-5 py-2.5 text-sm font-bold text-foreground transition-all hover:bg-foreground/5">Cancelar</button>
              <button type="button" onClick={savePagoDialog} className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-105 active:scale-95">Guardar Abono</button>
            </div>
          </div>
        </div>
      )}
    </form>
  );
}
