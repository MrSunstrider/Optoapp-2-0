import type { SyncEntityStatus } from "@/lib/sync";

export function SyncEntityList({ entities }: { entities: SyncEntityStatus[] }) {
  return (
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">Estado por entidad</h2>
      <div className="mt-4 space-y-3">
        {entities.map((e) => (
          <div key={e.key} className="rounded-2xl border border-border bg-foreground/[0.02] p-4 transition-colors hover:bg-foreground/[0.04]">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="font-bold text-foreground">{e.label}</p>
              <p
                className={`text-xs font-black uppercase tracking-widest ${
                  e.status === "ok"
                    ? "text-primary"
                    : e.status === "pending"
                      ? "text-amber-500"
                      : "text-destructive"
                }`}
              >
                {e.status}
              </p>
            </div>
            <div className="mt-3 grid grid-cols-3 gap-2 text-center">
              <div className="rounded-lg bg-foreground/5 p-2">
                <p className="text-[10px] font-bold uppercase text-muted-foreground/60">Pendientes</p>
                <p className="font-heading text-lg font-black text-foreground">{e.pendingCount}</p>
              </div>
              <div className="rounded-lg bg-foreground/5 p-2">
                <p className="text-[10px] font-bold uppercase text-muted-foreground/60">Sincronizados</p>
                <p className="font-heading text-lg font-black text-foreground">{e.successCount}</p>
              </div>
              <div className="rounded-lg bg-foreground/5 p-2">
                <p className="text-[10px] font-bold uppercase text-muted-foreground/60">Errores</p>
                <p className="font-heading text-lg font-black text-destructive">{e.failedCount}</p>
              </div>
            </div>
            <p className="mt-3 text-[10px] font-medium text-muted-foreground/80">
              <span className="font-bold uppercase tracking-tight">Último intento:</span> {e.lastAttemptAt ?? "—"} · {e.message}
            </p>
          </div>
        ))}
      </div>
    </section>
  );
}
