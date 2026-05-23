"use client";

export default function CierreCajaError({ error, reset }: { error: Error; reset: () => void }) {
  return (
    <div className="-m-6 min-h-screen bg-background p-6 text-foreground">
      <div className="mx-auto w-full max-w-4xl rounded-2xl border border-destructive/20 bg-destructive/5 p-6">
        <div className="flex items-start gap-4">
          <div className="h-10 w-10 shrink-0 rounded-xl bg-destructive/10 flex items-center justify-center">
            <svg className="h-5 w-5 text-destructive" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>
            </svg>
          </div>
          <div className="flex-1 space-y-3">
            <div>
              <h2 className="text-lg font-bold text-foreground">Error en cierre de caja</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                No se pudo cargar la información de cierre para esta fecha.
              </p>
            </div>
            <p className="text-xs text-muted-foreground/60 font-mono">
              {error.message}
            </p>
            <button
              onClick={() => reset()}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-bold text-primary-foreground transition-all hover:bg-primary/90 active:scale-95"
            >
              <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 2v6h-6M3 12a9 9 0 0 1 15-6.7L21 8M3 22v-6h6M21 12a9 9 0 0 1-15 6.7L3 16"/></svg>
              Reintentar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
