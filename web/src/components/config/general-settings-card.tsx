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
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5 shadow-sm">
      <h2 className="mb-4 text-xl font-semibold text-[#8AB4F8]">Ajustes Generales</h2>

      <div className="flex items-start justify-between gap-4 rounded-xl border border-zinc-600/70 bg-black/10 p-3">
        <div>
          <p className="text-sm font-medium text-zinc-100">Recordatorios Automáticos</p>
          <p className="mt-1 text-xs text-zinc-300">
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
          className={`relative h-8 w-14 rounded-full transition-colors ${
            automaticReminders ? "bg-[#8AB4F8]" : "bg-zinc-600"
          }`}
        >
          <span
            className={`absolute top-1 h-6 w-6 rounded-full bg-black transition-transform ${
              automaticReminders ? "translate-x-7" : "translate-x-1"
            }`}
          />
        </button>
      </div>

      <p className="mt-2 text-xs text-zinc-400">
        Preferencia persistida por usuario en metadata (`optoapp_automatic_reminders`).
      </p>

      <div aria-live="polite" className="mt-3 min-h-5 text-sm">
        {message ? <p className="text-emerald-300">{message}</p> : null}
        {error ? <p className="text-red-300">{error}</p> : null}
      </div>
    </section>
  );
}
