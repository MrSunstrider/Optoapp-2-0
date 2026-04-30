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
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5 shadow-sm">
      <h2 className="mb-4 text-xl font-semibold text-[#8AB4F8]">Seguridad y Acceso</h2>

      <div className="flex items-start justify-between gap-4 rounded-xl border border-zinc-600/70 bg-black/10 p-3">
        <div>
          <p className="text-sm font-medium text-zinc-100">Requerir PIN al inicio</p>
          <p className="mt-1 text-xs text-zinc-300">
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
          className={`relative h-8 w-14 rounded-full transition-colors ${
            pinRequired ? "bg-[#8AB4F8]" : "bg-zinc-600"
          }`}
        >
          <span
            className={`absolute top-1 h-6 w-6 rounded-full bg-black transition-transform ${
              pinRequired ? "translate-x-7" : "translate-x-1"
            }`}
          />
        </button>
      </div>

      <form onSubmit={onUpdatePin} className="mt-4 space-y-3">
        <PinInput
          label="PIN actual (6 dígitos)"
          value={currentPin}
          onChange={setCurrentPin}
        />
        <PinInput
          label="Nuevo PIN (6 dígitos)"
          value={newPin}
          onChange={setNewPin}
        />
        <PinInput
          label="Confirmar nuevo PIN"
          value={confirmPin}
          onChange={setConfirmPin}
        />
        <button
          type="submit"
          disabled={savingPin}
          className="w-full rounded-full bg-[#8AB4F8] px-4 py-2.5 text-sm font-semibold text-zinc-900 transition-opacity hover:opacity-90 disabled:opacity-60"
        >
          {savingPin ? "Actualizando..." : "Actualizar PIN"}
        </button>
      </form>

      <div aria-live="polite" className="mt-3 min-h-5 text-sm">
        {message ? <p className="text-emerald-300">{message}</p> : null}
        {error ? <p className="text-red-300">{error}</p> : null}
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
