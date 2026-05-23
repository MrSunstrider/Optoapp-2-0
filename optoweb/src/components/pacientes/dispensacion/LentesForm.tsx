"use client";

import { useMemo } from "react";
import {
  TIPO_LENTE,
  SUB_BIFOCAL,
  DISTANCIA,
  MATERIAL_LENTE,
  TRATAMIENTOS,
} from "@/lib/dispensacion-types";

type Props = {
  tipoLente: string;
  setTipoLente: (v: string) => void;
  subTipoBifocal: string;
  setSubTipoBifocal: (v: string) => void;
  distanciaLente: string;
  setDistanciaLente: (v: string) => void;
  altura: string;
  setAltura: (v: string) => void;
  materialLente: string;
  setMaterialLente: (v: string) => void;
  tratamientos: string[];
  setTratamientos: React.Dispatch<React.SetStateAction<string[]>>;
  colorLente: string;
  setColorLente: (v: string) => void;
  notasDiseno: string;
  setNotasDiseno: (v: string) => void;
};

export function LentesForm({
  tipoLente,
  setTipoLente,
  subTipoBifocal,
  setSubTipoBifocal,
  distanciaLente,
  setDistanciaLente,
  altura,
  setAltura,
  materialLente,
  setMaterialLente,
  tratamientos,
  setTratamientos,
  colorLente,
  setColorLente,
  notasDiseno,
  setNotasDiseno,
}: Props) {
  const requiresAltura = useMemo(
    () =>
      tipoLente === "Bifocal" ||
      tipoLente === "Progresivo" ||
      tipoLente === "Ocupacional",
    [tipoLente],
  );

  const tratamientosUi = useMemo(() => {
    const arr = [...tratamientos];
    if (arr.length === 0 || arr[arr.length - 1] !== "Ninguno")
      arr.push("Ninguno");
    return arr;
  }, [tratamientos]);

  function onTipoLenteChange(next: string) {
    setTipoLente(next);
    if (next !== "Bifocal") setSubTipoBifocal("");
    if (next !== "Monofocal") setDistanciaLente("");
    if (
      !(
        next === "Bifocal" ||
        next === "Progresivo" ||
        next === "Ocupacional"
      )
    )
      setAltura("");
  }

  return (
    <section className="rounded-2xl border border-border bg-foreground/[0.02] p-5 space-y-4">
      <h3 className="font-heading text-sm font-bold text-primary">
        Información del Lente
      </h3>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <select
          value={tipoLente}
          onChange={(e) => onTipoLenteChange(e.target.value)}
          className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
        >
          <option value="">Tipo de Lente</option>
          {TIPO_LENTE.map((x) => (
            <option key={x} value={x}>
              {x}
            </option>
          ))}
        </select>

        {tipoLente === "Bifocal" && (
          <select
            value={subTipoBifocal}
            onChange={(e) => setSubTipoBifocal(e.target.value)}
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          >
            <option value="">Sub-tipo Bifocal</option>
            {SUB_BIFOCAL.map((x) => (
              <option key={x} value={x}>
                {x}
              </option>
            ))}
          </select>
        )}

        {tipoLente === "Monofocal" && (
          <select
            value={distanciaLente}
            onChange={(e) => setDistanciaLente(e.target.value)}
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          >
            <option value="">Distancia</option>
            {DISTANCIA.map((x) => (
              <option key={x} value={x}>
                {x}
              </option>
            ))}
          </select>
        )}

        {requiresAltura && (
          <input
            value={altura}
            onChange={(e) => setAltura(e.target.value)}
            placeholder="Altura (mm)"
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          />
        )}

        <select
          value={materialLente}
          onChange={(e) => setMaterialLente(e.target.value)}
          className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
        >
          <option value="">Material</option>
          {MATERIAL_LENTE.map((x) => (
            <option key={x} value={x}>
              {x}
            </option>
          ))}
        </select>
      </div>

      <div className="space-y-3">
        <p className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/50 ml-1">
          Tratamientos Aplicados
        </p>
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
                return Array.from(
                  new Set(arr.filter((x) => x !== "Ninguno")),
                );
              });
            }}
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          >
            {TRATAMIENTOS.map((x) => (
              <option key={x} value={x}>
                {x}
              </option>
            ))}
          </select>
        ))}
      </div>

      <input
        value={colorLente}
        onChange={(e) => setColorLente(e.target.value)}
        placeholder="Color del lente"
        className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none"
      />

      <textarea
        value={notasDiseno}
        onChange={(e) => setNotasDiseno(e.target.value)}
        placeholder="Notas de diseño adicionales..."
        rows={2}
        className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm font-medium transition-all focus:border-primary focus:ring-1 focus:ring-primary outline-none resize-none"
      />
    </section>
  );
}
