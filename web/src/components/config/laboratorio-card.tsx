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
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">Laboratorio (esta óptica)</h2>
      <form className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2" onSubmit={onSubmit}>
        <Field label="Nombre de laboratorio preferido">
          <input
            value={form.preferredLab}
            onChange={(e) => setForm((s) => ({ ...s, preferredLab: e.target.value }))}
            className={inputClass}
            placeholder="Ej. Lab Optical Center"
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
          <div className="relative">
            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-sm font-bold text-muted-foreground/60">S/</span>
            <input
              type="number"
              min={0}
              step="0.01"
              value={form.costBase}
              onChange={(e) => setForm((s) => ({ ...s, costBase: Number(e.target.value) }))}
              className={inputClass + " pl-10"}
            />
          </div>
        </Field>
        <Field label="Política de prioridad">
          <select
            value={form.priority}
            onChange={(e) =>
              setForm((s) => ({ ...s, priority: e.target.value as "normal" | "urgente" }))
            }
            className={inputClass + " appearance-none"}
          >
            <option value="normal">🟢 Normal</option>
            <option value="urgente">🔴 Urgente</option>
          </select>
        </Field>
        <label className="md:col-span-2">
          <span className="text-xs font-bold uppercase tracking-widest text-muted-foreground/60">Notas internas</span>
          <textarea
            value={form.notes}
            onChange={(e) => setForm((s) => ({ ...s, notes: e.target.value }))}
            placeholder="Detalles adicionales sobre el flujo con este laboratorio..."
            className={inputClass + " mt-2 min-h-[100px] resize-none"}
          />
        </label>
        <div className="md:col-span-2">
          <button
            type="submit"
            disabled={saving}
            className="w-full rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:opacity-50"
          >
            {saving ? "⌛ Guardando..." : "💾 Guardar Ajustes de Laboratorio"}
          </button>
        </div>
      </form>
      <div className="mt-4 min-h-6" aria-live="polite">
        {message ? (
          <p className="flex items-center gap-2 text-sm font-bold text-primary">
            <span>✨</span> {message}
          </p>
        ) : null}
        {error ? (
          <p className="flex items-center gap-2 text-sm font-bold text-destructive">
            <span>❌</span> {error}
          </p>
        ) : null}
      </div>
    </section>
  );
}

const inputClass =
  "w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium text-foreground placeholder:text-muted-foreground/50 focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-xs font-bold uppercase tracking-widest text-muted-foreground/60">{label}</span>
      {children}
    </label>
  );
}
