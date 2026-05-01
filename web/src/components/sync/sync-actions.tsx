"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import type { SyncSnapshot } from "@/lib/sync";

export function SyncActions({
  canRun,
  snapshot
}: {
  canRun: boolean;
  snapshot: SyncSnapshot;
}) {
  const router = useRouter();
  const [running, setRunning] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function run(path: "/api/sync/run" | "/api/sync/retry") {
    setRunning(true);
    setMessage(null);
    setError(null);
    try {
      const res = await fetch(path, { method: "POST" });
      const data = (await res.json().catch(() => ({}))) as { error?: string; message?: string };
      if (!res.ok) {
        setError(data.error ?? "No se pudo ejecutar la verificación.");
        return;
      }
      setMessage(data.message ?? "Verificación iniciada.");
      router.refresh();
    } catch {
      setError("Error de red al ejecutar la verificación.");
    } finally {
      setRunning(false);
    }
  }

  async function copyDiag() {
    const text = JSON.stringify(snapshot, null, 2);
    try {
      await navigator.clipboard.writeText(text);
      setMessage("Diagnóstico copiado.");
    } catch {
      setError("No se pudo copiar diagnóstico.");
    }
  }

  return (
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">Acciones</h2>
      <div className="mt-4 flex flex-wrap gap-3">
        <button
          type="button"
          onClick={() => void run("/api/sync/run")}
          disabled={!canRun || running}
          className="flex items-center gap-2 rounded-xl bg-primary px-6 py-2.5 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-105 active:scale-95 disabled:opacity-50"
          aria-label="Verificar estado"
        >
          {running ? "⌛ Verificando..." : "🔍 Verificar estado"}
        </button>
        <button
          type="button"
          onClick={() => void run("/api/sync/retry")}
          disabled={!canRun || running}
          className="flex items-center gap-2 rounded-xl border border-border bg-card px-6 py-2.5 text-sm font-bold text-foreground transition-all hover:bg-foreground/5 disabled:opacity-50"
          aria-label="Reintentar fallidos"
        >
          🔄 Reintentar fallidos
        </button>
        <button
          type="button"
          onClick={copyDiag}
          className="flex items-center gap-2 rounded-xl border border-border bg-card px-6 py-2.5 text-sm font-bold text-foreground transition-all hover:bg-foreground/5"
          aria-label="Copiar diagnóstico"
        >
          📋 Copiar diagnóstico
        </button>
        <button
          type="button"
          onClick={() => router.refresh()}
          className="flex items-center gap-2 rounded-xl border border-border bg-card px-6 py-2.5 text-sm font-bold text-foreground transition-all hover:bg-foreground/5"
          aria-label="Refrescar estado"
        >
          ⚡ Refrescar
        </button>
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
