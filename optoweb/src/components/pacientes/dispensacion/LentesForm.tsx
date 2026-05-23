"use client";

import { useMemo } from "react";
import type { ItemDraft, MonturaOption } from "@/lib/dispensacion-types";
import {
  TIPO_LENTE,
  SUB_BIFOCAL,
  DISTANCIA,
  MATERIAL_LENTE,
  TRATAMIENTOS,
  ORIGEN_MONTURA,
  TIPO_ARO,
  MATERIAL_MONTURA,
} from "@/lib/dispensacion-types";

type Props = {
  item: ItemDraft;
  index: number;
  isOnly: boolean;
  monturas: MonturaOption[];
  onChange: (item: ItemDraft) => void;
  onRemove: () => void;
};

export function ItemCard({
  item,
  index,
  isOnly,
  monturas,
  onChange,
  onRemove,
}: Props) {
  const requiresAltura = useMemo(
    () =>
      item.tipoLente === "Bifocal" ||
      item.tipoLente === "Progresivo" ||
      item.tipoLente === "Ocupacional",
    [item.tipoLente],
  );

  const monturasDisponibles = useMemo(
    () =>
      monturas.filter(
        (m) =>
          (m.activo ?? true) &&
          ((m.stock_actual ?? 0) > 0 || m.id === item.monturaId),
      ),
    [item.monturaId, monturas],
  );

  function set<K extends keyof ItemDraft>(key: K, value: ItemDraft[K]) {
    onChange({ ...item, [key]: value });
  }

  return (
    <div className="rounded-2xl border border-border bg-foreground/[0.02] p-5 space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-heading text-sm font-bold text-primary">
          Producto {index + 1}
        </h3>
        {!isOnly && (
          <button
            type="button"
            onClick={onRemove}
            className="text-xs text-red-400 hover:text-red-300 transition-colors"
          >
            Eliminar
          </button>
        )}
      </div>

      {/* Lente */}
      <div className="border-b border-border pb-3 mb-1">
        <p className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 mb-3">
          Lente
        </p>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <select
            value={item.tipoLente}
            onChange={(e) => {
              const next = e.target.value;
              onChange({
                ...item,
                tipoLente: next,
                subTipoBifocal: next !== "Bifocal" ? "" : item.subTipoBifocal,
                distanciaLente: next !== "Monofocal" ? "" : item.distanciaLente,
                altura:
                  next === "Bifocal" || next === "Progresivo" || next === "Ocupacional"
                    ? item.altura
                    : "",
              });
            }}
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          >
            <option value="">Tipo de Lente</option>
            {TIPO_LENTE.map((x) => (
              <option key={x} value={x}>{x}</option>
            ))}
          </select>

          {item.tipoLente === "Bifocal" && (
            <select
              value={item.subTipoBifocal}
              onChange={(e) => set("subTipoBifocal", e.target.value)}
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
            >
              <option value="">Sub-tipo Bifocal</option>
              {SUB_BIFOCAL.map((x) => (
                <option key={x} value={x}>{x}</option>
              ))}
            </select>
          )}

          {item.tipoLente === "Monofocal" && (
            <select
              value={item.distanciaLente}
              onChange={(e) => set("distanciaLente", e.target.value)}
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
            >
              <option value="">Distancia</option>
              {DISTANCIA.map((x) => (
                <option key={x} value={x}>{x}</option>
              ))}
            </select>
          )}

          {requiresAltura && (
            <input
              value={item.altura}
              onChange={(e) => set("altura", e.target.value)}
              placeholder="Altura (mm)"
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
            />
          )}

          <select
            value={item.materialLente}
            onChange={(e) => set("materialLente", e.target.value)}
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          >
            <option value="">Material</option>
            {MATERIAL_LENTE.map((x) => (
              <option key={x} value={x}>{x}</option>
            ))}
          </select>
        </div>

        <div className="space-y-3 mt-3">
          <p className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50">
            Tratamientos
          </p>
          {(() => {
            const arr = [...item.tratamientos];
            if (arr.length === 0 || arr[arr.length - 1] !== "Ninguno") arr.push("Ninguno");
            return arr;
          })().map((value, idx) => (
            <select
              key={`${idx}-${value}`}
              value={value}
              onChange={(e) => {
                const selected = e.target.value;
                const prev = [...item.tratamientos];
                if (idx < prev.length) {
                  if (selected === "Ninguno") prev.splice(idx, 1);
                  else prev[idx] = selected;
                } else if (selected !== "Ninguno") {
                  prev.push(selected);
                }
                set("tratamientos", Array.from(new Set(prev.filter((x) => x !== "Ninguno"))));
              }}
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
            >
              {TRATAMIENTOS.map((x) => (
                <option key={x} value={x}>{x}</option>
              ))}
            </select>
          ))}
        </div>

        <input
          value={item.colorLente}
          onChange={(e) => set("colorLente", e.target.value)}
          placeholder="Color del lente"
          className="mt-3 w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
        />
        <textarea
          value={item.notasDiseno}
          onChange={(e) => set("notasDiseno", e.target.value)}
          placeholder="Notas de diseño..."
          rows={2}
          className="mt-3 w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none resize-none"
        />
      </div>

      {/* Montura */}
      <div>
        <p className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 mb-3">
          Montura
        </p>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <select
            value={item.origenMontura}
            onChange={(e) => {
              onChange({
                ...item,
                origenMontura: e.target.value,
                monturaId: e.target.value !== "Tienda" ? "" : item.monturaId,
              });
            }}
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          >
            <option value="">Origen</option>
            {ORIGEN_MONTURA.map((x) => (
              <option key={x} value={x}>{x}</option>
            ))}
          </select>

          {item.origenMontura === "Tienda" && (
            <select
              value={item.monturaId}
              onChange={(e) => {
                const id = e.target.value;
                const m = monturasDisponibles.find((x) => x.id === id);
                onChange({
                  ...item,
                  monturaId: id,
                  descripcionMontura: m
                    ? `${m.marca ?? ""} ${m.modelo ?? ""}`.trim()
                    : item.descripcionMontura,
                });
              }}
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
            >
              <option value="">Montura de inventario</option>
              {monturasDisponibles.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.sku ?? m.id} - {(m.marca ?? "").trim()} {(m.modelo ?? "").trim()} (Stock: {m.stock_actual ?? 0})
                </option>
              ))}
            </select>
          )}

          <select
            value={item.tipoAro}
            onChange={(e) => set("tipoAro", e.target.value)}
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          >
            <option value="">Tipo de Aro</option>
            {TIPO_ARO.map((x) => (
              <option key={x} value={x}>{x}</option>
            ))}
          </select>

          <select
            value={item.materialMontura}
            onChange={(e) => set("materialMontura", e.target.value)}
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          >
            <option value="">Material</option>
            {MATERIAL_MONTURA.map((x) => (
              <option key={x} value={x}>{x}</option>
            ))}
          </select>
        </div>
        <input
          value={item.descripcionMontura}
          onChange={(e) => set("descripcionMontura", e.target.value)}
          placeholder="Descripción (Marca, Modelo)"
          className="mt-3 w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
        />
      </div>
    </div>
  );
}
