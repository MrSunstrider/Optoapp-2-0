"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

export function DiagnosticoCard({ reportText }: { reportText: string }) {
  const router = useRouter();
  const [msg, setMsg] = useState<string | null>(null);

  async function copyReport() {
    try {
      await navigator.clipboard.writeText(reportText);
      setMsg("Reporte copiado al portapapeles.");
    } catch {
      setMsg("No se pudo copiar el reporte.");
    }
  }

  return (
    <div className="mt-4 flex flex-wrap gap-2">
      <button
        type="button"
        onClick={() => router.refresh()}
        className="rounded-full border border-zinc-600 px-4 py-2 text-sm text-zinc-100 hover:bg-white/5"
      >
        Refrescar diagnóstico
      </button>
      <button
        type="button"
        onClick={() => router.refresh()}
        className="rounded-full border border-zinc-600 px-4 py-2 text-sm text-zinc-100 hover:bg-white/5"
      >
        Reintentar sincronización
      </button>
      <button
        type="button"
        onClick={copyReport}
        className="rounded-full border border-zinc-600 px-4 py-2 text-sm text-zinc-100 hover:bg-white/5"
      >
        Copiar reporte técnico
      </button>
      {msg ? <p className="w-full text-xs text-zinc-300">{msg}</p> : null}
    </div>
  );
}
