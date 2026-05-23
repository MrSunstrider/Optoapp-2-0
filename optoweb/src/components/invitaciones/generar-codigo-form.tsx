"use client";

import { useActionState } from "react";

type GenerarCodigoState = { error?: string; codigo?: string } | null;

export function GenerarCodigoForm({
  action,
}: {
  action: (
    prevState: GenerarCodigoState,
    formData: FormData,
  ) => Promise<GenerarCodigoState>;
}) {
  const [state, formAction, isPending] = useActionState(action, null);

  return (
    <div className="mt-8 rounded-2xl border border-dashed border-border p-6 bg-foreground/[0.01]">
      <h3 className="text-xs font-black uppercase tracking-[0.2em] text-muted-foreground/60 mb-4">
        Invitar por código
      </h3>

      {state?.codigo ? (
        <div className="rounded-2xl border-2 border-emerald-500/30 bg-emerald-500/5 p-6 text-center">
          <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 mb-2">
            Código generado
          </p>
          <p className="font-mono text-3xl font-black tracking-widest text-emerald-500">
            {state.codigo}
          </p>
          <p className="mt-3 text-xs text-muted-foreground">
            Compartí este código con el nuevo usuario. Expira en 7 días.
          </p>
          <button
            type="button"
            onClick={() => navigator.clipboard.writeText(state.codigo!)}
            className="mt-3 rounded-xl bg-emerald-500/10 px-4 py-2 text-[10px] font-black uppercase tracking-widest text-emerald-500 transition-all hover:bg-emerald-500/20"
          >
            Copiar código
          </button>
        </div>
      ) : (
        <form action={formAction} className="space-y-4">
          {state?.error && (
            <p className="text-xs font-bold text-destructive">{state.error}</p>
          )}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 items-end">
            <div className="space-y-1">
              <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">
                Email (opcional)
              </span>
              <input
                name="email"
                type="email"
                className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-medium outline-none focus:ring-2 focus:ring-primary/20"
                placeholder="usuario@ejemplo.com"
              />
            </div>
            <div className="space-y-1">
              <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">
                Asignar Rol
              </span>
              <select
                name="rol"
                defaultValue="colaborador"
                className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-medium outline-none focus:ring-2 focus:ring-primary/20"
              >
                <option value="admin">admin</option>
                <option value="gerente">gerente</option>
                <option value="colaborador">colaborador</option>
                <option value="invitado">invitado</option>
              </select>
            </div>
          </div>
          <button
            type="submit"
            disabled={isPending}
            className="w-full rounded-2xl bg-primary py-4 text-xs font-black uppercase tracking-[0.2em] text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.01] active:scale-95 disabled:opacity-50"
          >
            {isPending ? "Generando..." : "Generar código"}
          </button>
        </form>
      )}

      {state?.codigo && (
        <button
          type="button"
          onClick={() => {
            /* reset form — re-render with null state via a key trick */
            window.location.reload();
          }}
          className="mt-4 w-full rounded-xl border border-border bg-background py-3 text-[10px] font-black uppercase tracking-widest text-muted-foreground transition-all hover:bg-accent"
        >
          Generar otro código
        </button>
      )}
    </div>
  );
}
