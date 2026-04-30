"use client";

import { useState } from "react";
import { IconLogout } from "@/components/nav-icons";

export function LogoutButton() {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onLogout() {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/auth/logout", { method: "POST" });
      if (!res.ok) {
        setError("No se pudo cerrar sesión completamente. Intenta nuevamente.");
      }
      window.location.assign("/login");
    } catch {
      setError("No se pudo cerrar sesión completamente. Intenta nuevamente.");
      window.location.assign("/login");
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="flex w-full items-center justify-center gap-2 rounded-lg px-3 py-2.5 text-sm font-medium text-[#B0B0B0] transition-colors hover:bg-white/[0.06] hover:text-white"
      >
        <IconLogout className="shrink-0" />
        Cerrar Sesión
      </button>
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-md rounded-xl border border-zinc-700 bg-zinc-900 p-5">
            <h3 className="text-lg font-semibold text-zinc-100">¿Cerrar sesión?</h3>
            <p className="mt-2 text-sm text-zinc-300">
              Si hay sincronización en curso, continuará en servidor según estado actual.
            </p>
            {error ? <p className="mt-2 text-sm text-red-300">{error}</p> : null}
            <div className="mt-4 flex justify-end gap-2">
              <button
                type="button"
                className="rounded-md border border-zinc-600 px-4 py-2 text-sm text-zinc-100"
                onClick={() => setOpen(false)}
                disabled={loading}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="rounded-md bg-red-600 px-4 py-2 text-sm font-semibold text-white"
                onClick={() => void onLogout()}
                disabled={loading}
              >
                {loading ? "Cerrando..." : "Cerrar sesión"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
