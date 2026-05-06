"use client";

import { useState } from "react";

type Props = {
  initialAutomaticReminders: boolean;
  initialTimeZone?: string;
};

export function GeneralSettingsCard({ initialAutomaticReminders, initialTimeZone }: Props) {
  const [automaticReminders, setAutomaticReminders] = useState(
    initialAutomaticReminders
  );
  const [timeZone, setTimeZone] = useState(initialTimeZone || "auto");
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

  async function onTimeZoneChange(newTz: string) {
    setSaving(true);
    setMessage(null);
    setError(null);
    try {
      const res = await fetch("/api/config/user-settings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ 
          timezone: newTz === "auto" ? null : newTz 
        })
      });
      if (!res.ok) throw new Error();
      setTimeZone(newTz);
      setMessage("Zona horaria actualizada.");
    } catch {
      setError("No se pudo guardar la zona horaria.");
    } finally {
      setSaving(false);
    }
  }

  const zones = [
    { label: "Detectar automáticamente (Navegador)", value: "auto" },
    { label: "America/Lima (Perú)", value: "America/Lima" },
    { label: "America/Bogota (Colombia)", value: "America/Bogota" },
    { label: "America/Mexico_City (México)", value: "America/Mexico_City" },
    { label: "America/Santiago (Chile)", value: "America/Santiago" },
    { label: "America/Argentina/Buenos_Aires (Argentina)", value: "America/Argentina/Buenos_Aires" },
    { label: "America/Guayaquil (Ecuador)", value: "America/Guayaquil" },
    { label: "America/Caracas (Venezuela)", value: "America/Caracas" },
    { label: "Europe/Madrid (España)", value: "Europe/Madrid" }
  ];

  return (
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm transition-all hover:shadow-md">
      <h2 className="font-heading text-xl font-bold text-foreground">Ajustes Generales</h2>

      <div className="mt-6 flex flex-col gap-6">
        <div className="flex items-start justify-between gap-4 rounded-2xl border border-border bg-foreground/[0.02] p-4 transition-all hover:bg-foreground/[0.04]">
          <div>
            <p className="font-bold text-foreground text-sm">Recordatorios Automáticos</p>
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

        <div className="flex flex-col gap-3 rounded-2xl border border-border bg-foreground/[0.02] p-4 transition-all hover:bg-foreground/[0.04]">
          <div>
            <p className="font-bold text-foreground text-sm">Zona Horaria (SaaS)</p>
            <p className="mt-1 text-[11px] font-medium text-muted-foreground/80">
              Si las fechas no coinciden con tu reloj, elige tu ciudad manualmente.
            </p>
          </div>
          <select
            disabled={saving}
            value={timeZone}
            onChange={(e) => void onTimeZoneChange(e.target.value)}
            className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-bold text-foreground outline-none focus:ring-2 focus:ring-primary/20"
          >
            {zones.map((z) => (
              <option key={z.value} value={z.value}>
                {z.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <p className="mt-4 text-[10px] font-bold uppercase tracking-[0.15em] text-muted-foreground/40">
        Preferencia persistida en perfil de usuario.
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
