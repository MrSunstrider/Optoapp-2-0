"use client";

import { useState } from "react";

type Props = {
  initialAutomaticReminders: boolean;
};

export function GeneralSettingsCard({ initialAutomaticReminders }: Props) {
  const [automaticReminders, setAutomaticReminders] = useState(
    initialAutomaticReminders
  );
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function onToggle(next: boolean) {
    setSaving(true);
    setMessage(null);
    setError(null);
    try {
      const res = await fetch("/api/config/user-settings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ automaticReminders: next })
      });
      const data = (await res.json().catch(() => ({}))) as {
        error?: string;
        automaticReminders?: boolean;
      };
      if (!res.ok) {
        setError(data.error ?? "No se pudo guardar el ajuste.");
        return;
      }
      setAutomaticReminders(data.automaticReminders ?? next);
      setMessage("Ajuste guardado correctamente.");
    } catch {
      setError("Error de red al guardar el ajuste.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm transition-all hover:shadow-md">
      <h2 className="font-heading text-xl font-bold text-foreground">Ajustes Generales</h2>

      <div className="mt-6 flex items-start justify-between gap-4 rounded-2xl border border-border bg-foreground/[0.02] p-4 transition-all hover:bg-foreground/[0.04]">
        <div>
          <p className="font-bold text-foreground">Recordatorios Automáticos</p>
          <p className="mt-1 text-xs font-medium text-muted-foreground/80">
            Aviso el día de la cita alrededor de las 12:00 (hora local).
          </p>
        </div>
        <button
          type="button"
          role="switch"
          aria-checked={automaticReminders}
          aria-label="Recordatorios automáticos"
          disabled={saving}
          onClick={() => void onToggle(!automaticReminders)}
          className={`relative h-8 w-14 shrink-0 rounded-full transition-all duration-300 ${
            automaticReminders ? "bg-primary shadow-lg shadow-primary/30" : "bg-muted"
          }`}
        >
          <span
            className={`absolute top-1 h-6 w-6 rounded-full bg-white shadow-sm transition-transform duration-300 ${
              automaticReminders ? "translate-x-7" : "translate-x-1"
            }`}
          />
        </button>
      </div>

      <p className="mt-4 text-[10px] font-bold uppercase tracking-[0.15em] text-muted-foreground/40">
        Preferencia persistida en metadata segura.
      </p>

      <div aria-live="polite" className="mt-4 min-h-6">
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
