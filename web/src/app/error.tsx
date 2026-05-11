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
    <div className="min-h-screen grid place-items-center p-6">
      <div className="max-w-lg w-full text-center">
        <h1 className="text-2xl font-semibold mb-2">Error de aplicacion</h1>
        <p className="text-sm text-slate-700 mb-2 whitespace-pre-wrap">
          {error.message}
        </p>
        {error.digest && (
          <p className="text-xs text-slate-500 mb-4">Digest: {error.digest}</p>
        )}
        {isDev && error.stack && (
          <pre className="text-left text-xs bg-slate-100 border rounded p-3 mb-4 overflow-x-auto max-h-64 overflow-y-auto">
            {error.stack}
          </pre>
        )}
        <button
          type="button"
          onClick={reset}
          className="rounded bg-slate-900 text-white px-4 py-2 text-sm"
        >
          Reintentar
        </button>
      </div>
    </div>
  );
}
