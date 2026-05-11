"use client";

import { useActionState } from "react";

type CanjearCodigoState = { error?: string } | null;

export function CanjearCodigoForm({
  action,
}: {
  action: (
    prevState: CanjearCodigoState,
    formData: FormData,
  ) => Promise<CanjearCodigoState>;
}) {
  const [state, formAction, isPending] = useActionState(action, null);

  return (
    <div className="rounded-2xl border border-border bg-card p-6 shadow-sm">
      <h3 className="font-heading text-lg font-bold text-foreground">
        Tengo un código de invitación
      </h3>
      <p className="mb-4 text-sm text-muted-foreground">
        Si ya recibiste un código de parte de una óptica, ingresalo aquí.
      </p>
      <form action={formAction} className="space-y-4">
        <div className="space-y-1">
          <label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">
            Código de invitación
          </label>
          <input
            name="codigo"
            required
            className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-medium uppercase outline-none focus:ring-2 focus:ring-primary/20"
            placeholder="OPT-A7K2B"
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
          {isPending ? "Validando..." : "Canjear código"}
        </button>
      </form>
    </div>
  );
}
