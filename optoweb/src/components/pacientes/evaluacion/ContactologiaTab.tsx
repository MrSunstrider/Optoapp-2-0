"use client";

import { useState } from "react";
import type { EvaluacionDraft, DraftUpdateFn } from "@/lib/evaluacion-types";
import { parsePower } from "@/lib/evaluacion-constants";

type Props = {
  draft: EvaluacionDraft;
  update: DraftUpdateFn;
  inputCls: string;
};

function roundQuarter(value: number): number {
  return Math.round(value * 4) / 4;
}

function vertexCompensate(power: number): number {
  const result = power / (1 - 0.012 * power);
  return roundQuarter(result);
}

function formatPower(value: number): string {
  return value.toFixed(2);
}

function buildLcSuggestion(k1: string, k2: string): string | null {
  const n1 = Number(k1.replace(",", "."));
  const n2 = Number(k2.replace(",", "."));
  if (!Number.isFinite(n1) || !Number.isFinite(n2)) return null;
  const diff = Math.abs(n1 - n2);
  if (diff >= 4) {
    return "Sugerencia: Lente RGP (rigido gas permeable) - Astigmatismo corneal alto.";
  }
  if (diff >= 2.5) {
    return "Sugerencia: Valorar RGP o lente torico blando - Astigmatismo corneal moderado.";
  }
  return "Sugerencia: Lente blando (esferico o torico segun refraccion) - Astigmatismo corneal bajo.";
}

function autoCalculo(
  esfStr: string,
  cilStr: string,
  recorteActivo: boolean
): {
  esf: number;
  cil: number;
  warning: boolean;
} {
  const sph = parsePower(esfStr);
  const cyl = parsePower(cilStr);
  const canRecortar = Math.abs(cyl) <= Math.abs(sph) / 4;
  const recortar = recorteActivo && canRecortar;
  if (recortar) {
    const sphTemp = sph + cyl / 2;
    return { esf: vertexCompensate(sphTemp), cil: 0, warning: false };
  }
  return {
    esf: vertexCompensate(sph),
    cil: cyl,
    warning: recorteActivo && !canRecortar && cyl !== 0
  };
}

export function ContactologiaTab({ draft, update, inputCls }: Props) {
  const cardCls = "space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-3";
  const subtitle = "text-sm font-semibold text-sky-300";
  const [aplicarRecorteOd, setAplicarRecorteOd] = useState(false);
  const [aplicarRecorteOi, setAplicarRecorteOi] = useState(false);

  const odSug = buildLcSuggestion(draft.k1Od, draft.k2Od);
  const oiSug = buildLcSuggestion(draft.k1Oi, draft.k2Oi);
  const odAuto = autoCalculo(draft.recetaOdEsf, draft.recetaOdCil, aplicarRecorteOd);
  const oiAuto = autoCalculo(draft.recetaOiEsf, draft.recetaOiCil, aplicarRecorteOi);

  function cargarOd() {
    update("lcOdEsf", formatPower(odAuto.esf));
    update("lcOdCil", formatPower(odAuto.cil));
  }
  function cargarOi() {
    update("lcOiEsf", formatPower(oiAuto.esf));
    update("lcOiCil", formatPower(oiAuto.cil));
  }

  return (
    <div className="space-y-4">
      <div className={cardCls}>
        <p className={subtitle}>Queratometria</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            K1 OD
            <input
              value={draft.k1Od}
              onChange={(e) => update("k1Od", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            K2 OD
            <input
              value={draft.k2Od}
              onChange={(e) => update("k2Od", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            K1 OI
            <input
              value={draft.k1Oi}
              onChange={(e) => update("k1Oi", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            K2 OI
            <input
              value={draft.k2Oi}
              onChange={(e) => update("k2Oi", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Sugerencias y Calculos</p>
        {odSug && (
          <div className="rounded-lg border border-zinc-700 bg-zinc-950/60 p-3 text-sm text-zinc-300">
            <p className="font-medium text-sky-300">OD</p>
            <p>{odSug}</p>
            <p className="mt-1 text-xs text-zinc-500">
              Considerar tambien comodidad del paciente, estilo de vida y regularidad corneal.
            </p>
          </div>
        )}
        {oiSug && (
          <div className="rounded-lg border border-zinc-700 bg-zinc-950/60 p-3 text-sm text-zinc-300">
            <p className="font-medium text-sky-300">OI</p>
            <p>{oiSug}</p>
            <p className="mt-1 text-xs text-zinc-500">
              Considerar tambien comodidad del paciente, estilo de vida y regularidad corneal.
            </p>
          </div>
        )}

        <div className="rounded-lg border border-sky-900/50 bg-sky-950/20 p-3">
          <p className="font-medium text-sky-300">Auto-Calculo OD</p>
          <label className="mt-2 flex items-center gap-2 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={aplicarRecorteOd}
              onChange={(e) => setAplicarRecorteOd(e.target.checked)}
            />
            Aplicar recorte de cilindro
          </label>
          <p className="mt-2 text-sm text-zinc-200">
            Recomendacion LC: Esf {formatPower(odAuto.esf)}
            {odAuto.cil !== 0 ? ` / Cil ${formatPower(odAuto.cil)}` : ""}
          </p>
          {odAuto.warning && (
            <p className="mt-1 text-xs text-amber-300">
              El cilindro supera el limite para recorte, se requiere lente torica.
            </p>
          )}
          <button
            type="button"
            onClick={cargarOd}
            className="mt-2 rounded-lg bg-sky-400 px-4 py-1.5 text-sm font-medium text-zinc-900"
          >
            Cargar
          </button>
        </div>

        <div className="rounded-lg border border-sky-900/50 bg-sky-950/20 p-3">
          <p className="font-medium text-sky-300">Auto-Calculo OI</p>
          <label className="mt-2 flex items-center gap-2 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={aplicarRecorteOi}
              onChange={(e) => setAplicarRecorteOi(e.target.checked)}
            />
            Aplicar recorte de cilindro
          </label>
          <p className="mt-2 text-sm text-zinc-200">
            Recomendacion LC: Esf {formatPower(oiAuto.esf)}
            {oiAuto.cil !== 0 ? ` / Cil ${formatPower(oiAuto.cil)}` : ""}
          </p>
          {oiAuto.warning && (
            <p className="mt-1 text-xs text-amber-300">
              El cilindro supera el limite para recorte, se requiere lente torica.
            </p>
          )}
          <button
            type="button"
            onClick={cargarOi}
            className="mt-2 rounded-lg bg-sky-400 px-4 py-1.5 text-sm font-medium text-zinc-900"
          >
            Cargar
          </button>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Prueba / Adaptacion Final</p>
        <p className="text-sm font-medium text-zinc-300">Poder del Lente</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            LC OD Esf
            <input
              value={draft.lcOdEsf}
              onChange={(e) => update("lcOdEsf", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            LC OD Cil
            <input
              value={draft.lcOdCil}
              onChange={(e) => update("lcOdCil", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            LC OD Eje
            <input
              value={draft.lcOdEje}
              onChange={(e) => update("lcOdEje", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            LC OI Esf
            <input
              value={draft.lcOiEsf}
              onChange={(e) => update("lcOiEsf", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            LC OI Cil
            <input
              value={draft.lcOiCil}
              onChange={(e) => update("lcOiCil", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            LC OI Eje
            <input
              value={draft.lcOiEje}
              onChange={(e) => update("lcOiEje", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>

        <p className="pt-1 text-sm font-medium text-zinc-300">Parametros Fisicos</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            Curva Base (CB) OD
            <input
              value={draft.lcRadioBaseOd}
              onChange={(e) => update("lcRadioBaseOd", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            Curva Base (CB) OI
            <input
              value={draft.lcRadioBaseOi}
              onChange={(e) => update("lcRadioBaseOi", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            DIA OD
            <input
              value={draft.lcOdDia}
              onChange={(e) => update("lcOdDia", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            DIA OI
            <input
              value={draft.lcOiDia}
              onChange={(e) => update("lcOiDia", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>

        <label className="block text-sm text-zinc-300">
          Laboratorio / Marca
          <input
            value={draft.lcLaboratorio}
            onChange={(e) => update("lcLaboratorio", e.target.value)}
            className={inputCls}
          />
        </label>
        <label className="block text-sm text-zinc-300">
          Tipo de Lente
          <select
            value={draft.lcTipoLente}
            onChange={(e) => update("lcTipoLente", e.target.value)}
            className={inputCls}
          >
            <option value="">Seleccionar...</option>
            <option value="Blanda">Blanda</option>
            <option value="Rigida (RGP)">Rigida (RGP)</option>
            <option value="Torica">Torica</option>
            <option value="Multifocal">Multifocal</option>
            <option value="Cosmetica">Cosmetica</option>
          </select>
        </label>
        <label className="block text-sm text-zinc-300">
          Material
          <select
            value={draft.lcMaterial}
            onChange={(e) => update("lcMaterial", e.target.value)}
            className={inputCls}
          >
            <option value="">Seleccionar...</option>
            <option value="Hidrogel">Hidrogel</option>
            <option value="Silicona Hidrogel">Silicona Hidrogel</option>
            <option value="PMMA">PMMA</option>
            <option value="Gas Permeable">Gas Permeable</option>
          </select>
        </label>
        <label className="block text-sm text-zinc-300">
          Fecha Adaptacion
          <input
            type="date"
            value={draft.lcFechaAdaptacion}
            onChange={(e) => update("lcFechaAdaptacion", e.target.value)}
            className={inputCls}
          />
        </label>
        <label className="block text-sm text-zinc-300">
          Notas Contactologia
          <textarea
            value={draft.lcObservaciones}
            onChange={(e) => update("lcObservaciones", e.target.value)}
            rows={3}
            className={inputCls}
          />
        </label>
      </div>
    </div>
  );
}
