"use client";

import type { EvaluacionDraft, DraftUpdateFn } from "@/lib/evaluacion-types";
import {
  ESTEREOPSIS_OPTIONS,
  LANG_OPTIONS,
  WORTH_OPTIONS,
  FARNSWORTH_OPTIONS,
  SENSIBILIDAD_OPTIONS,
  CAMPO_VISUAL_OPTIONS
} from "@/lib/evaluacion-constants";

type Props = {
  draft: EvaluacionDraft;
  update: DraftUpdateFn;
  onOpenOsdi: () => void;
  inputCls: string;
};

export function ExamenVisualTab({ draft, update, onOpenOsdi, inputCls }: Props) {
  const cardCls = "space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-3";
  return (
    <div className="space-y-4">
      <div className={cardCls}>
        <p className="text-sm font-semibold text-sky-300">Agudeza Visual SIN correccion</p>
        <label className="block text-sm text-zinc-300">
          Ambos ojos
          <input
            value={draft.avScAo}
            onChange={(e) => update("avScAo", e.target.value)}
            className={inputCls}
          />
        </label>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            OD
            <input
              value={draft.avScOdLejos}
              onChange={(e) => update("avScOdLejos", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OI
            <input
              value={draft.avScOiLejos}
              onChange={(e) => update("avScOiLejos", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
      </div>

      <div className={cardCls}>
        <p className="text-sm font-semibold text-sky-300">Agudeza Visual CON correccion PX</p>
        <label className="block text-sm text-zinc-300">
          Ambos ojos
          <input
            value={draft.avCcAoPx}
            onChange={(e) => update("avCcAoPx", e.target.value)}
            className={inputCls}
          />
        </label>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            OD
            <input
              value={draft.avCcOdLejos}
              onChange={(e) => update("avCcOdLejos", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OI
            <input
              value={draft.avCcOiLejos}
              onChange={(e) => update("avCcOiLejos", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
      </div>

      <div className={cardCls}>
        <p className="text-sm font-semibold text-sky-300">Vision Binocular y Percepcion</p>
        <label className="block text-sm text-zinc-300">
          Estereopsis
          <select
            value={draft.estereopsisValor}
            onChange={(e) => update("estereopsisValor", e.target.value)}
            className={inputCls}
          >
            <option value="">Seleccionar...</option>
            {ESTEREOPSIS_OPTIONS.map((x) => (
              <option key={x} value={x}>
                {x}
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm text-zinc-300">
          Segundos de arco (opcional)
          <input
            value={draft.estereopsisSegundos}
            onChange={(e) => update("estereopsisSegundos", e.target.value)}
            className={inputCls}
          />
        </label>
        <label className="block text-sm text-zinc-300">
          Test de Lang
          <select
            value={draft.lang}
            onChange={(e) => update("lang", e.target.value)}
            className={inputCls}
          >
            <option value="">Seleccionar...</option>
            {LANG_OPTIONS.map((x) => (
              <option key={x} value={x}>
                {x}
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm text-zinc-300">
          Test de Worth
          <select
            value={draft.worth}
            onChange={(e) => update("worth", e.target.value)}
            className={inputCls}
          >
            <option value="">Seleccionar...</option>
            {WORTH_OPTIONS.map((x) => (
              <option key={x} value={x}>
                {x}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className={cardCls}>
        <p className="text-sm font-semibold text-sky-300">Percepcion del Color</p>
        <label className="block text-sm text-zinc-300">
          Test de Ishihara
          <input
            value={draft.ishihara}
            onChange={(e) => update("ishihara", e.target.value)}
            className={inputCls}
          />
        </label>
        <label className="block text-sm text-zinc-300">
          Test de Farnsworth
          <select
            value={draft.farnsworth}
            onChange={(e) => update("farnsworth", e.target.value)}
            className={inputCls}
          >
            <option value="">Seleccionar...</option>
            {FARNSWORTH_OPTIONS.map((x) => (
              <option key={x} value={x}>
                {x}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className={cardCls}>
        <p className="text-sm font-semibold text-sky-300">Salud de la Superficie Ocular y Funcion Visual</p>
        <p className="text-sm font-medium text-zinc-300">Test de Schirmer (mm)</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            OD
            <input
              value={draft.schirmerOd}
              onChange={(e) => update("schirmerOd", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OI
            <input
              value={draft.schirmerOi}
              onChange={(e) => update("schirmerOi", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <p className="text-sm font-medium text-zinc-300">Test OSDI</p>
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={onOpenOsdi}
            className="rounded-lg border border-zinc-600 px-3 py-2 text-sm hover:bg-zinc-800"
          >
            Realizar test OSDI
          </button>
          {draft.osdiPuntuacion && (
            <span className="text-sm font-semibold text-sky-300">
              {draft.osdiPuntuacion} - {draft.osdiClasificacion}
            </span>
          )}
        </div>
        <label className="block text-sm text-zinc-300">
          Sensibilidad al contraste
          <select
            value={draft.sensibilidadContraste}
            onChange={(e) => update("sensibilidadContraste", e.target.value)}
            className={inputCls}
          >
            <option value="">Seleccionar...</option>
            {SENSIBILIDAD_OPTIONS.map((x) => (
              <option key={x} value={x}>
                {x}
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm text-zinc-300">
          Frecuencia espacial (opcional)
          <input
            value={draft.sensibilidadFrecuencia}
            onChange={(e) => update("sensibilidadFrecuencia", e.target.value)}
            className={inputCls}
          />
        </label>
      </div>

      <div className={cardCls}>
        <p className="text-sm font-semibold text-zinc-300">Otras Pruebas y Examenes Previos</p>
        <label className="block text-sm text-zinc-300">
          Test de Amsler
          <input
            value={draft.amsler}
            onChange={(e) => update("amsler", e.target.value)}
            className={inputCls}
          />
        </label>
        <label className="block text-sm text-zinc-300">
          Campo visual por confrontacion
          <select
            value={draft.campoVisual}
            onChange={(e) => update("campoVisual", e.target.value)}
            className={inputCls}
          >
            <option value="">Seleccionar...</option>
            {CAMPO_VISUAL_OPTIONS.map((x) => (
              <option key={x} value={x}>
                {x}
              </option>
            ))}
          </select>
        </label>
        {draft.campoVisual === "Anomalia detectada" && (
          <label className="block text-sm text-zinc-300">
            Descripcion de anomalia (Campo Visual)
            <input
              value={draft.campoVisualDescripcion}
              onChange={(e) => update("campoVisualDescripcion", e.target.value)}
              className={inputCls}
            />
          </label>
        )}
        <hr className="border-zinc-800" />
        <p className="text-sm font-medium text-zinc-300">Examenes Previos</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            PH OD
            <input
              value={draft.phOd}
              onChange={(e) => update("phOd", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            PH OI
            <input
              value={draft.phOi}
              onChange={(e) => update("phOi", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            Kappa OD
            <input
              value={draft.kappaOd}
              onChange={(e) => update("kappaOd", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            Kappa OI
            <input
              value={draft.kappaOi}
              onChange={(e) => update("kappaOi", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <label className="block text-sm text-zinc-300">
          Hirshberg
          <input
            value={draft.hirshberg}
            onChange={(e) => update("hirshberg", e.target.value)}
            className={inputCls}
          />
        </label>
        <p className="text-sm font-medium text-zinc-300">Ducciones</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            OD
            <input
              value={draft.duccionesOd}
              onChange={(e) => update("duccionesOd", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OI
            <input
              value={draft.duccionesOi}
              onChange={(e) => update("duccionesOi", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <label className="block text-sm text-zinc-300">
          Versiones Ambos Ojos
          <input
            value={draft.versionesAo}
            onChange={(e) => update("versionesAo", e.target.value)}
            className={inputCls}
          />
        </label>
        <p className="text-sm font-medium text-zinc-300">Cover Test</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            6m
            <input
              value={draft.coverTest6m}
              onChange={(e) => update("coverTest6m", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            40cm
            <input
              value={draft.coverTest40cm}
              onChange={(e) => update("coverTest40cm", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            10cm
            <input
              value={draft.coverTest10cm}
              onChange={(e) => update("coverTest10cm", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <p className="text-sm font-medium text-zinc-300">PPC</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            OR
            <input
              value={draft.ppcOr}
              onChange={(e) => update("ppcOr", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            Luz
            <input
              value={draft.ppcLuz}
              onChange={(e) => update("ppcLuz", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            FR + L
            <input
              value={draft.ppcFrl}
              onChange={(e) => update("ppcFrl", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <p className="text-sm font-medium text-zinc-300">Reflejos Pupilares</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            Fotomotor
            <input
              value={draft.reflejoFotomotor}
              onChange={(e) => update("reflejoFotomotor", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            Consensual
            <input
              value={draft.reflejoConsensual}
              onChange={(e) => update("reflejoConsensual", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            Acomodativo
            <input
              value={draft.reflejoAcomodativo}
              onChange={(e) => update("reflejoAcomodativo", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
      </div>
    </div>
  );
}
