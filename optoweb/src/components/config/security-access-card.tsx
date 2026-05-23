"use client";

import { useState } from "react";

type Props = {
  initialPinRequired: boolean;
};

export function SecurityAccessCard({ initialPinRequired }: Props) {
  const [pinRequired, setPinRequired] = useState(initialPinRequired);
  const [savingToggle, setSavingToggle] = useState(false);
  const [savingPin, setSavingPin] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [currentPin, setCurrentPin] = useState("");
  const [newPin, setNewPin] = useState("");
  const [confirmPin, setConfirmPin] = useState("");

  async function onToggle(next: boolean) {
    setSavingToggle(true);
    setError(null);
    setMessage(null);
    try {
      const res = await fetch("/api/config/user-settings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ pinRequired: next })
      });
      const data = (await res.json().catch(() => ({}))) as {
        error?: string;
        pinRequired?: boolean;
      };
      if (!res.ok) {
        setError(data.error ?? "No se pudo actualizar este ajuste.");
        return;
      }
      setPinRequired(data.pinRequired ?? next);
      setMessage("Configuración de PIN actualizada.");
    } catch {
      setError("Error de red al actualizar la configuración.");
    } finally {
      setSavingToggle(false);
    }
  }

  async function onUpdatePin(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setMessage(null);

    if (!/^\d{6}$/.test(currentPin) || !/^\d{6}$/.test(newPin) || !/^\d{6}$/.test(confirmPin)) {
      setError("Todos los campos deben tener 6 dígitos.");
      return;
    }
    if (newPin !== confirmPin) {
      setError("La confirmación del nuevo PIN no coincide.");
      return;
    }

    setSavingPin(true);
    try {
      const res = await fetch("/api/config/change-pin", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ currentPin, newPin, confirmPin })
      });
      const data = (await res.json().catch(() => ({}))) as { error?: string };
      if (!res.ok) {
        setError(data.error ?? "No se pudo actualizar el PIN.");
        return;
      }
      setCurrentPin("");
      setNewPin("");
      setConfirmPin("");
      setMessage("PIN actualizado correctamente.");
    } catch {
      setError("Error de red al actualizar el PIN.");
    } finally {
      setSavingPin(false);
    }
  }

  return (
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">Seguridad y Acceso</h2>

      <div className="mt-6 flex items-start justify-between gap-4 rounded-2xl border border-border bg-foreground/[0.02] p-4 transition-all hover:bg-foreground/[0.04]">
        <div>
          <p className="font-bold text-foreground">Requerir PIN al inicio</p>
          <p className="mt-1 text-xs font-medium text-muted-foreground/80">
            Solicita el PIN de seguridad cada vez que abres la app.
          </p>
        </div>
        <button
          type="button"
          role="switch"
          aria-checked={pinRequired}
          aria-label="Requerir PIN al inicio"
          disabled={savingToggle}
          onClick={() => void onToggle(!pinRequired)}
          className={`relative h-8 w-14 shrink-0 rounded-full transition-all duration-300 ${
            pinRequired ? "bg-primary shadow-lg shadow-primary/30" : "bg-muted"
          }`}
        >
          <span
            className={`absolute top-1 h-6 w-6 rounded-full bg-white shadow-sm transition-transform duration-300 ${
              pinRequired ? "translate-x-7" : "translate-x-1"
            }`}
          />
        </button>
      </div>

      <div className="mt-6 space-y-4">
        <h3 className="text-xs font-bold uppercase tracking-widest text-muted-foreground/60">Actualizar Credenciales</h3>
        <form onSubmit={onUpdatePin} className="space-y-4">
          <PinInput
            label="PIN actual"
            value={currentPin}
            onChange={setCurrentPin}
          />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <PinInput
              label="Nuevo PIN"
              value={newPin}
              onChange={setNewPin}
            />
            <PinInput
              label="Confirmar PIN"
              value={confirmPin}
              onChange={setConfirmPin}
            />
          </div>
          <button
            type="submit"
            disabled={savingPin}
            className="w-full rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:opacity-50"
          >
            {savingPin ? "⌛ Actualizando..." : "🔐 Actualizar PIN de Seguridad"}
          </button>
        </form>
      </div>

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

function PinInput({
  label,
  value,
  onChange
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <label className="block">
      <span className="sr-only">{label}</span>
      <input
        type="password"
        inputMode="numeric"
        autoComplete="off"
        pattern="\d{6}"
        maxLength={6}
        value={value}
        onChange={(e) => onChange(e.target.value.replace(/\D/g, "").slice(0, 6))}
        placeholder={label}
        className="w-full rounded-xl border border-zinc-500/70 bg-[#4A4756] px-4 py-3 text-sm text-zinc-100 placeholder:text-zinc-300/70 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#8AB4F8]"
      />
    </label>
  );
}
