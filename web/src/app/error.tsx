"use client";

export default function ErrorPage({
  error,
  reset
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const isDev = process.env.NODE_ENV === "development";

  return (
    <div className="min-h-screen grid place-items-center p-6 bg-background">
      <div className="max-w-lg w-full text-center space-y-4">
        <div className="mx-auto h-16 w-16 rounded-2xl bg-destructive/10 flex items-center justify-center">
          <svg className="h-8 w-8 text-destructive" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>
          </svg>
        </div>
        <h1 className="font-heading text-2xl font-bold text-foreground">Error de aplicación</h1>
        <p className="text-sm text-muted-foreground whitespace-pre-wrap">
          {error.message}
        </p>
        {error.digest && (
          <p className="text-xs text-muted-foreground/60">Digest: {error.digest}</p>
        )}
        {isDev && error.stack && (
          <pre className="text-left text-xs bg-muted border border-border rounded-2xl p-4 overflow-x-auto max-h-64 overflow-y-auto text-muted-foreground">
            {error.stack}
          </pre>
        )}
        <button
          type="button"
          onClick={reset}
          className="inline-flex items-center gap-2 rounded-2xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
        >
          <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 2v6h-6M3 12a9 9 0 0 1 15-6.7L21 8M3 22v-6h6M21 12a9 9 0 0 1-15 6.7L3 16"/></svg>
          Reintentar
        </button>
      </div>
    </div>
  );
}
