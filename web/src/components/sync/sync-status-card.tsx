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
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <h2 className="text-xl font-semibold text-[#8AB4F8]">Estado de sincronización</h2>
      <p className={`mt-2 text-lg font-medium ${cls}`}>{label}</p>
      <p className="text-xs text-zinc-300">
        Última sincronización exitosa: {snapshot.lastSuccessAt ?? "Sin registros"}
      </p>
      {snapshot.lastError ? (
        <p className="mt-2 text-xs text-red-300">Último error: {snapshot.lastError}</p>
      ) : null}
    </section>
  );
}
