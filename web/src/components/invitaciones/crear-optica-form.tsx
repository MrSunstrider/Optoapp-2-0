"use client";

import { useActionState } from "react";

type CrearOpticaState = { error?: string } | null;

export function CrearOpticaForm({
  action,
}: {
  action: (
    prevState: CrearOpticaState,
    formData: FormData,
  ) => Promise<CrearOpticaState>;
}) {
  const [state, formAction, isPending] = useActionState(action, null);

  return (
    <div className="rounded-2xl border border-border bg-card p-6 shadow-sm">
      <h3 className="font-heading text-lg font-bold text-foreground">
        Crear mi óptica
      </h3>
      <p className="mb-4 text-sm text-muted-foreground">
        Registrate como nuevo profesional creando tu propia óptica.
      </p>
      <form action={formAction} className="space-y-4">
        <div className="space-y-1">
          <label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">
            Nombre de la óptica
          </label>
          <input
            name="nombre"
            required
            className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-medium outline-none focus:ring-2 focus:ring-primary/20"
            placeholder="Ej: Optica Central"
          />
        </div>
        {state?.error && (
          <p className="text-xs font-bold text-destructive">{state.error}</p>
        )}
        <button
          type="submit"
          disabled={isPending}
          className="w-full rounded-2xl bg-primary py-4 text-xs font-black uppercase tracking-[0.2em] text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.01] active:scale-95 disabled:opacity-50"
        >
          {isPending ? "Creando..." : "Crear óptica"}
        </button>
      </form>
    </div>
  );
}
