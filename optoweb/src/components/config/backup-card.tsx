"use client";

import { useState } from "react";

type Props = { role: string };

const btn =
  "flex items-center justify-center gap-2 rounded-xl border border-border bg-card px-4 py-2.5 text-sm font-bold text-foreground transition-all hover:bg-foreground/5 active:scale-95";

export function DataManagementSection({ role }: Props) {
  const canManage = role === "admin";
  const [backupMsg, setBackupMsg] = useState<string | null>(null);
  const [backupErr, setBackupErr] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);
  const [importing, setImporting] = useState(false);

  async function handleExport() {
    setExporting(true);
    setBackupMsg(null);
    setBackupErr(null);
    try {
      const res = await fetch("/api/config/backup/export");
      if (!res.ok) {
        const err = await res.json().catch(() => ({ error: "Error de red" }));
        setBackupErr(err.error ?? "Error al exportar");
        return;
      }
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `respaldo_${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
      setBackupMsg("Respaldo descargado correctamente.");
    } catch {
      setBackupErr("No se pudo exportar el respaldo.");
    } finally {
      setExporting(false);
    }
  }

  async function handleImport(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setImporting(true);
    setBackupMsg(null);
    setBackupErr(null);
    try {
      const text = await file.text();
      const json = JSON.parse(text);
      const res = await fetch("/api/config/backup/import", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(json)
      });
      const data = await res.json().catch(() => ({ error: "Error de red" }));
      if (!res.ok) {
        setBackupErr(data.error ?? "Error al restaurar");
      } else {
        setBackupMsg(data.message ?? "Restaurado correctamente.");
      }
    } catch {
      setBackupErr("No se pudo procesar el archivo. Asegúrate de que sea un JSON válido.");
    } finally {
      setImporting(false);
      e.target.value = "";
    }
  }

  return (
    <section className="rounded-3xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">Gestión de Datos Maestros</h2>
      <p className="mt-2 text-sm font-medium text-muted-foreground">
        Exportaciones seguras de la base de datos operativa.
      </p>


      <hr className="my-6 border-border" />

      <h3 className="font-heading text-base font-bold text-foreground">Respaldo Completo (JSON)</h3>
      <p className="text-xs font-medium text-muted-foreground mt-1">
        Exporta o importa un respaldo completo de todos los datos de la óptica.
        Solo para administradores.
      </p>

      {backupErr && (
        <p className="mt-4 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {backupErr}
        </p>
      )}
      {backupMsg && (
        <p className="mt-4 rounded-lg border border-emerald-900/50 bg-emerald-950/40 px-3 py-2 text-sm text-emerald-300">
          {backupMsg}
        </p>
      )}

      <div className="mt-4 flex flex-wrap gap-3">
        <button
          onClick={handleExport}
          disabled={exporting || !canManage}
          className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:opacity-50"
        >
          {exporting ? "Exportando…" : "📤 Exportar respaldo"}
        </button>

        <label className="cursor-pointer rounded-xl border border-border bg-card px-5 py-2.5 text-sm font-bold text-foreground transition-all hover:bg-foreground/5 active:scale-95 disabled:opacity-50">
          {importing ? "Importando…" : "📥 Importar respaldo"}
          <input
            type="file"
            accept=".json"
            onChange={handleImport}
            disabled={importing || !canManage}
            className="hidden"
          />
        </label>
      </div>

      {!canManage && (
        <p className="mt-6 text-center text-[10px] font-black uppercase tracking-widest text-amber-500">
          Tu rol actual tiene restricciones en acciones destructivas.
        </p>
      )}
    </section>
  );
}
