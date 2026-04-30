import type { SyncEntityStatus } from "@/lib/sync";

export function SyncEntityList({ entities }: { entities: SyncEntityStatus[] }) {
  return (
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <h2 className="text-xl font-semibold text-[#8AB4F8]">Estado por entidad</h2>
      <div className="mt-3 space-y-2">
        {entities.map((e) => (
          <div key={e.key} className="rounded-xl border border-zinc-700 bg-black/15 p-3 text-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="font-medium text-zinc-100">{e.label}</p>
              <p
                className={
                  e.status === "ok"
                    ? "text-emerald-300"
                    : e.status === "pending"
                      ? "text-amber-300"
                      : "text-red-300"
                }
              >
                {e.status}
              </p>
            </div>
            <p className="text-xs text-zinc-300">
              Pendientes: {e.pendingCount} · Exitosos: {e.successCount} · Fallidos:{" "}
              {e.failedCount}
            </p>
            <p className="text-xs text-zinc-400">
              Último intento: {e.lastAttemptAt ?? "—"} · {e.message}
            </p>
          </div>
        ))}
      </div>
    </section>
  );
}
