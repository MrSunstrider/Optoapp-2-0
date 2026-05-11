import type { SyncSnapshot } from "@/lib/sync";

export function SyncStatusCard({ snapshot }: { snapshot: SyncSnapshot }) {
  const cls =
    snapshot.globalStatus === "ok"
      ? "text-emerald-300"
      : snapshot.globalStatus === "pending"
        ? "text-amber-300"
        : snapshot.globalStatus === "degraded"
          ? "text-orange-300"
          : "text-red-300";

  const label =
    snapshot.globalStatus === "ok"
      ? "OK"
      : snapshot.globalStatus === "pending"
        ? "Pendiente"
        : snapshot.globalStatus === "degraded"
          ? "Degradado"
          : "Error";

  return (
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm transition-all hover:shadow-md">
      <h2 className="font-heading text-xl font-bold text-foreground">Estado de sincronización</h2>
      <div className="mt-4 flex items-center gap-4">
        <div className={`flex h-12 w-12 items-center justify-center rounded-2xl bg-foreground/5 text-2xl`}>
          {snapshot.globalStatus === "ok" ? "✅" : "⏳"}
        </div>
        <div>
          <p className={`text-2xl font-black tracking-tight ${cls}`}>{label}</p>
          <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/60">
            {snapshot.lastSuccessAt ? `Última sincronización: ${snapshot.lastSuccessAt}` : "Sin registros"}
          </p>
        </div>
      </div>
      {snapshot.lastError ? (
        <div className="mt-4 rounded-xl bg-destructive/10 p-3">
          <p className="text-xs font-bold text-destructive">Último error: {snapshot.lastError}</p>
        </div>
      ) : null}
    </section>
  );
}
