"use client";

export default function ErrorPage({
  error,
  reset
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="min-h-screen grid place-items-center p-6">
      <div className="max-w-md text-center">
        <h1 className="text-2xl font-semibold mb-2">Error de aplicacion</h1>
        <p className="text-sm text-slate-600 mb-4">{error.message}</p>
        <button
          onClick={reset}
          className="rounded bg-slate-900 text-white px-4 py-2 text-sm"
        >
          Reintentar
        </button>
      </div>
    </div>
  );
}
