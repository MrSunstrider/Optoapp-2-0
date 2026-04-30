"use client";

import { useState } from "react";

type Branch = {
  id: string;
  nombre: string;
  direccion: string;
  contacto: string;
  activa: boolean;
  principal: boolean;
};

export function SucursalesCard({ initialBranches }: { initialBranches: Branch[] }) {
  const [branches, setBranches] = useState(initialBranches);
  const [newName, setNewName] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function addBranch() {
    if (!newName.trim()) return;
    const next: Branch = {
      id: crypto.randomUUID(),
      nombre: newName.trim(),
      direccion: "",
      contacto: "",
      activa: true,
      principal: branches.length === 0
    };
    setBranches((prev) => [...prev, next]);
    setNewName("");
  }

  function setPrincipal(id: string) {
    setBranches((prev) => prev.map((b) => ({ ...b, principal: b.id === id })));
  }

  function removeBranch(id: string) {
    const target = branches.find((b) => b.id === id);
    if (!target) return;
    if (target.principal && branches.length > 1) {
      setError("Reasigna otra sucursal como principal antes de eliminar.");
      return;
    }
    setError(null);
    setBranches((prev) => prev.filter((b) => b.id !== id));
  }

  async function saveAll() {
    setSaving(true);
    setError(null);
    setMessage(null);
    if (branches.length > 0 && !branches.some((b) => b.principal)) {
      setError("Debe existir una sucursal principal.");
      setSaving(false);
      return;
    }
    try {
      const res = await fetch("/api/config/user-settings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          opticaConfigPatch: {
            branches
          }
        })
      });
      const data = (await res.json().catch(() => ({}))) as { error?: string };
      if (!res.ok) {
        setError(data.error ?? "No se pudo guardar sucursales.");
        return;
      }
      setMessage("Sucursales guardadas.");
    } catch {
      setError("Error de red al guardar sucursales.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <h2 className="mb-4 text-xl font-semibold text-[#8AB4F8]">Sucursales</h2>
      <p className="mb-3 text-xs text-zinc-400">
        Configuración operativa persistida por óptica activa en preferencias seguras.
      </p>

      <div className="mb-3 flex gap-2">
        <input
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder="Nueva sucursal"
          className="flex-1 rounded-xl border border-zinc-500/70 bg-[#4A4756] px-3 py-2 text-sm text-zinc-100"
        />
        <button
          type="button"
          onClick={addBranch}
          className="rounded-xl border border-zinc-600 px-3 py-2 text-sm text-zinc-100"
        >
          Agregar
        </button>
      </div>

      <div className="space-y-2">
        {branches.map((b) => (
          <div key={b.id} className="rounded-xl border border-zinc-700 bg-black/10 p-3">
            <div className="flex items-center justify-between gap-3">
              <p className="font-medium text-zinc-100">{b.nombre}</p>
              <div className="flex gap-2 text-xs">
                <button
                  type="button"
                  onClick={() => setPrincipal(b.id)}
                  className="rounded border border-zinc-600 px-2 py-1 text-zinc-100"
                >
                  {b.principal ? "Principal" : "Hacer principal"}
                </button>
                <button
                  type="button"
                  onClick={() => removeBranch(b.id)}
                  className="rounded border border-red-500/70 px-2 py-1 text-red-200"
                >
                  Eliminar
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={saveAll}
        disabled={saving}
        className="mt-4 rounded-full bg-[#8AB4F8] px-5 py-2.5 text-sm font-semibold text-zinc-900 disabled:opacity-60"
      >
        {saving ? "Guardando..." : "Guardar sucursales"}
      </button>
      <div className="mt-3 min-h-5 text-sm" aria-live="polite">
        {message ? <p className="text-emerald-300">{message}</p> : null}
        {error ? <p className="text-red-300">{error}</p> : null}
      </div>
    </section>
  );
}
