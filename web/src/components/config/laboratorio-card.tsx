"use client";

import { useState } from "react";

type Props = {
  initial: {
    preferredLab: string;
    deliveryDays: number;
    costBase: number;
    priority: "normal" | "urgente";
    notes: string;
  };
};

export function LaboratorioCard({ initial }: Props) {
  const [form, setForm] = useState(initial);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setMessage(null);
    if (form.deliveryDays < 0 || form.deliveryDays > 120) {
      setError("Tiempo estimado debe estar entre 0 y 120 días.");
      return;
    }
    setSaving(true);
    try {
      const res = await fetch("/api/config/user-settings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          opticaConfigPatch: {
            labConfig: form,
            labUpdatedAt: new Date().toISOString()
          }
        })
      });
      const data = (await res.json().catch(() => ({}))) as { error?: string };
      if (!res.ok) {
        setError(data.error ?? "No se pudo guardar configuración de laboratorio.");
        return;
      }
      setMessage("Configuración de laboratorio guardada.");
    } catch {
      setError("Error de red al guardar.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <h2 className="mb-4 text-xl font-semibold text-[#8AB4F8]">Laboratorio (esta óptica)</h2>
      <form className="grid grid-cols-1 gap-3 md:grid-cols-2" onSubmit={onSubmit}>
        <Field label="Nombre de laboratorio preferido">
          <input
            value={form.preferredLab}
            onChange={(e) => setForm((s) => ({ ...s, preferredLab: e.target.value }))}
            className={inputClass}
          />
        </Field>
        <Field label="Tiempo estimado entrega (días)">
          <input
            type="number"
            min={0}
            max={120}
            value={form.deliveryDays}
            onChange={(e) => setForm((s) => ({ ...s, deliveryDays: Number(e.target.value) }))}
            className={inputClass}
          />
        </Field>
        <Field label="Costo base laboratorio">
          <input
            type="number"
            min={0}
            step="0.01"
            value={form.costBase}
            onChange={(e) => setForm((s) => ({ ...s, costBase: Number(e.target.value) }))}
            className={inputClass}
          />
        </Field>
        <Field label="Política de prioridad">
          <select
            value={form.priority}
            onChange={(e) =>
              setForm((s) => ({ ...s, priority: e.target.value as "normal" | "urgente" }))
            }
            className={inputClass}
          >
            <option value="normal">Normal</option>
            <option value="urgente">Urgente</option>
          </select>
        </Field>
        <label className="md:col-span-2">
          <span className="text-sm text-zinc-200">Notas internas</span>
          <textarea
            value={form.notes}
            onChange={(e) => setForm((s) => ({ ...s, notes: e.target.value }))}
            className={inputClass + " mt-1 min-h-[90px]"}
          />
        </label>
        <div className="md:col-span-2">
          <button
            type="submit"
            disabled={saving}
            className="rounded-full bg-[#8AB4F8] px-5 py-2.5 text-sm font-semibold text-zinc-900 disabled:opacity-60"
          >
            {saving ? "Guardando..." : "Guardar laboratorio"}
          </button>
        </div>
      </form>
      <div className="mt-3 min-h-5 text-sm" aria-live="polite">
        {message ? <p className="text-emerald-300">{message}</p> : null}
        {error ? <p className="text-red-300">{error}</p> : null}
      </div>
    </section>
  );
}

const inputClass =
  "mt-1 w-full rounded-xl border border-zinc-500/70 bg-[#4A4756] px-3 py-2 text-sm text-zinc-100";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label>
      <span className="text-sm text-zinc-200">{label}</span>
      {children}
    </label>
  );
}
