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
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm transition-all hover:shadow-md">
      <h2 className="font-heading text-xl font-bold text-foreground">Sucursales</h2>
      <p className="mb-6 text-xs font-medium text-muted-foreground/80">
        Configuración operativa persistida por óptica activa en preferencias seguras.
      </p>

      <div className="mb-6 flex gap-3">
        <input
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder="Nombre de la nueva sucursal"
          className="flex-1 rounded-xl border border-border bg-foreground/[0.03] px-4 py-2.5 text-sm font-medium text-foreground placeholder:text-muted-foreground/50 focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
        />
        <button
          type="button"
          onClick={addBranch}
          className="rounded-xl border border-border bg-card px-6 py-2.5 text-sm font-bold text-foreground transition-all hover:bg-foreground/5 active:scale-95"
        >
          Agregar
        </button>
      </div>

      <div className="space-y-3">
        {branches.map((b) => (
          <div key={b.id} className="group rounded-2xl border border-border bg-foreground/[0.02] p-4 transition-all hover:bg-foreground/[0.04]">
            <div className="flex items-center justify-between gap-4">
              <p className="font-bold text-foreground">{b.nombre}</p>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setPrincipal(b.id)}
                  className={`rounded-lg px-3 py-1.5 text-xs font-bold transition-all ${
                    b.principal 
                      ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20" 
                      : "bg-card border border-border text-foreground hover:bg-foreground/5"
                  }`}
                >
                  {b.principal ? "⭐ Principal" : "Hacer principal"}
                </button>
                <button
                  type="button"
                  onClick={() => removeBranch(b.id)}
                  className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-1.5 text-xs font-bold text-destructive transition-all hover:bg-destructive hover:text-white"
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
        className="mt-8 flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:opacity-50"
      >
        {saving ? "⌛ Guardando cambios..." : "💾 Guardar sucursales"}
      </button>
      
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
