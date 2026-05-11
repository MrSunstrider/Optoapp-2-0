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
        className="flex w-full items-center justify-center gap-3 rounded-xl px-4 py-3 text-sm font-bold text-muted-foreground transition-all hover:bg-foreground/[0.05] hover:text-foreground active:scale-95"
      >
        <IconLogout className="shrink-0" />
        Cerrar Sesión
      </button>
      {open && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-background/80 backdrop-blur-sm p-4 animate-in fade-in">
          <div
            className="absolute inset-0"
            role="presentation"
            onClick={() => !loading && setOpen(false)}
          />
          <div className="relative z-10 w-full max-w-sm rounded-[2.5rem] border border-border bg-card p-8 shadow-2xl">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-destructive/10 text-destructive">
              <IconLogout className="h-8 w-8" />
            </div>
            <h3 className="text-center font-heading text-2xl font-black text-foreground">¿Finalizar Sesión?</h3>
            <p className="mt-4 text-center text-sm font-medium text-muted-foreground leading-relaxed">
              Asegúrate de haber guardado tus cambios. La sincronización pendiente continuará en el servidor.
            </p>
            
            {error ? (
              <div className="mt-4 rounded-xl bg-destructive/10 p-3 text-center text-[10px] font-bold text-destructive uppercase tracking-widest">
                {error}
              </div>
            ) : null}

            <div className="mt-8 flex flex-col gap-2">
              <button
                type="button"
                className="w-full rounded-2xl bg-destructive py-4 font-heading text-base font-black text-white shadow-xl shadow-destructive/20 transition-all hover:scale-[1.02] active:scale-95 disabled:opacity-30"
                onClick={() => void onLogout()}
                disabled={loading}
              >
                {loading ? "CERRANDO..." : "SÍ, CERRAR SESIÓN"}
              </button>
              <button
                type="button"
                className="w-full rounded-2xl border border-border py-3 text-sm font-bold text-muted-foreground hover:bg-muted/50 transition-all"
                onClick={() => setOpen(false)}
                disabled={loading}
              >
                CANCELAR
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
