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
        setError(data.error ?? "No se pudo ejecutar la sincronización.");
        return;
      }
      setMessage(data.message ?? "Sincronización iniciada.");
      router.refresh();
    } catch {
      setError("Error de red al ejecutar la sincronización.");
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
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <h2 className="text-xl font-semibold text-[#8AB4F8]">Acciones</h2>
      <div className="mt-3 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => void run("/api/sync/run")}
          disabled={!canRun || running}
          className="rounded-full bg-[#8AB4F8] px-5 py-2 text-sm font-semibold text-zinc-900 disabled:opacity-50"
          aria-label="Sincronizar ahora"
        >
          {running ? "Sincronizando..." : "Sincronizar ahora"}
        </button>
        <button
          type="button"
          onClick={() => router.refresh()}
          className="rounded-full border border-zinc-600 px-5 py-2 text-sm text-zinc-100"
          aria-label="Refrescar estado"
        >
          Refrescar estado
        </button>
        <button
          type="button"
          onClick={copyDiag}
          className="rounded-full border border-zinc-600 px-5 py-2 text-sm text-zinc-100"
          aria-label="Copiar diagnóstico"
        >
          Copiar diagnóstico
        </button>
        <button
          type="button"
          onClick={() => void run("/api/sync/retry")}
          disabled={!canRun || running}
          className="rounded-full border border-zinc-600 px-5 py-2 text-sm text-zinc-100 disabled:opacity-50"
          aria-label="Reintentar fallidos"
        >
          Reintentar fallidos
        </button>
      </div>
      <div aria-live="polite" className="mt-3 min-h-5 text-sm">
        {message ? <p className="text-emerald-300">{message}</p> : null}
        {error ? <p className="text-red-300">{error}</p> : null}
      </div>
    </section>
  );
}
