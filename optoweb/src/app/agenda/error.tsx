"use client";

export default function AgendaError() {
  return (
    <div className="-m-6 min-h-screen bg-background p-6 text-foreground">
      <div className="mx-auto w-full max-w-4xl rounded-2xl border border-destructive/20 bg-destructive/5 p-6">
        <div className="flex items-start gap-4">
          <div className="h-10 w-10 shrink-0 rounded-xl bg-destructive/10 flex items-center justify-center">
            <svg className="h-5 w-5 text-destructive" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>
            </svg>
          </div>
          <div>
            <h2 className="text-lg font-bold text-foreground">Agenda no disponible</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Ocurrió un error inesperado al cargar la agenda.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
