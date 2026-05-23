"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

const BUTTON_CLASS =
  "rounded-full border border-zinc-500/70 bg-zinc-900/40 px-4 py-2 text-sm font-medium text-zinc-100 transition-colors hover:bg-zinc-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-400 disabled:opacity-60";

export function DashboardExportActions() {
  const router = useRouter();
  const [refreshing, setRefreshing] = useState(false);

  function download(type: "inventario-pdf") {
    window.location.assign(`/api/dashboard/export?type=${type}`);
  }

  async function onRefresh() {
    setRefreshing(true);
    router.refresh();
    setTimeout(() => setRefreshing(false), 500);
  }

  return (
    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
      <button
        type="button"
        className={BUTTON_CLASS}
        onClick={() => download("inventario-pdf")}
      >
        Inventario PDF
      </button>
      <button
        type="button"
        onClick={onRefresh}
        disabled={refreshing}
        className={BUTTON_CLASS + " sm:col-span-2"}
      >
        {refreshing ? "Actualizando..." : "Actualizar"}
      </button>
    </div>
  );
}
