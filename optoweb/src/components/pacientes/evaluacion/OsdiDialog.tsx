"use client";

import { useState } from "react";

type Props = {
  onClose: () => void;
  onSave: (score: number, clasificacion: string) => void;
};

const OPTIONS: Array<{ label: string; value: number }> = [
  { label: "Ninguna vez (0)", value: 0 },
  { label: "Algunas veces (1)", value: 1 },
  { label: "Mitad de las veces (2)", value: 2 },
  { label: "La mayoria de las veces (3)", value: 3 },
  { label: "Todo el tiempo (4)", value: 4 }
];

const QUESTIONS = [
  "Sensibilidad a la luz?",
  "Sensacion de arenilla o polvo?",
  "Dolor o ardor en los ojos?",
  "Vision borrosa o baja?",
  "Leer o mirar pantallas?",
  "Conducir de noche?",
  "Usar cajeros automaticos o leer senales?",
  "Ver television?",
  "Viento o aire acondicionado?",
  "Ambientes muy secos (calefaccion, aire acondicionado)?",
  "Ambientes con humo o contaminacion?",
  "Usar lentes de contacto (si aplica)?"
];

export function OsdiDialog({ onClose, onSave }: Props) {
  const [answers, setAnswers] = useState<Record<number, number>>({});

  const save = () => {
    const vals = Object.values(answers);
    if (vals.length === 0) return onClose();
    const score = Math.round((vals.reduce((a, b) => a + b, 0) * 25) / vals.length);
    const clasificacion =
      score <= 12 ? "Normal" : score <= 22 ? "Leve" : score <= 32 ? "Moderado" : "Severo";
    onSave(score, clasificacion);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-3">
      <div className="absolute inset-0" onClick={onClose} role="presentation" />
      <div className="relative z-10 max-h-[90vh] w-full max-w-2xl overflow-auto rounded-xl border border-zinc-700 bg-zinc-900 p-4 text-zinc-100">
        <h3 className="text-lg font-semibold text-sky-300">Cuestionario OSDI</h3>
        <div className="mt-3 space-y-3">
          {QUESTIONS.map((q, i) => (
            <label key={q} className="block text-sm text-zinc-300">
              {i + 1}. {q}
              <select
                value={answers[i] ?? ""}
                onChange={(e) => {
                  const v = e.target.value;
                  setAnswers((prev) => {
                    if (v === "") {
                      const next = { ...prev };
                      delete next[i];
                      return next;
                    }
                    return { ...prev, [i]: Number(v) };
                  });
                }}
                className="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
              >
                <option value="">Seleccionar...</option>
                {OPTIONS.map((opt) => (
                  <option key={opt.label} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </label>
          ))}
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-zinc-600 px-4 py-2 text-sm"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={save}
            className="rounded-lg bg-sky-500 px-4 py-2 text-sm font-medium text-zinc-900"
          >
            Guardar y Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
