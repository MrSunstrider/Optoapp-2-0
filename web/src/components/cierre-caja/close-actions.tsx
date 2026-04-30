"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

export function CloseActions({
  fecha,
  canClose,
  canOverride,
  isClosed,
  featureEnabled
}: {
  fecha: string;
  canClose: boolean;
  canOverride: boolean;
  isClosed: boolean;
  featureEnabled: boolean;
}) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  async function run(action: "close" | "reopen") {
    setLoading(true);
    setErr(null);
    setMsg(null);
    try {
      const res = await fetch("/api/cierre-caja/close", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fecha, action })
      });
      const data = (await res.json().catch(() => ({}))) as { error?: string; message?: string };
      if (!res.ok) {
        setErr(data.error ?? "No se pudo procesar el cierre.");
        return;
      }
      setMsg(data.message ?? "Acción realizada.");
      router.refresh();
    } catch {
      setErr("Error de red al ejecutar la acción.");
    } finally {
      setLoading(false);
    }
  }

  if (!featureEnabled) {
    return (
      <p className="text-xs text-zinc-400">
        Cierre formal no habilitado en base de datos (tabla `cierres_caja` no disponible).
      </p>
    );
  }

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          disabled={!canClose || loading || isClosed}
          onClick={() => void run("close")}
          className="rounded-full border border-zinc-500 px-5 py-2 text-sm text-zinc-100 disabled:opacity-50"
          aria-label="Cerrar caja"
        >
          {loading ? "Procesando..." : "Cerrar caja"}
        </button>
        <button
          type="button"
          disabled={!canOverride || loading || !isClosed}
          onClick={() => void run("reopen")}
          className="rounded-full border border-zinc-500 px-5 py-2 text-sm text-zinc-100 disabled:opacity-50"
          aria-label="Reabrir caja"
        >
          Reabrir caja
        </button>
      </div>
      <div aria-live="polite" className="min-h-5 text-xs">
        {msg ? <p className="text-emerald-300">{msg}</p> : null}
        {err ? <p className="text-red-300">{err}</p> : null}
      </div>
    </div>
  );
}
