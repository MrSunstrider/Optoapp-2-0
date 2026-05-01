"use client";

export default function CierreCajaError({ error, reset }: { error: Error; reset: () => void }) {
  return (
    <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
      <div className="mx-auto w-full max-w-4xl rounded-xl border border-red-700/50 bg-red-950/20 p-4">
        <h2 className="text-lg font-semibold text-red-200">Error en cierre de caja</h2>
        <p className="mt-1 text-sm text-red-100">
          No se pudo cargar la información de cierre para esta fecha.
        </p>
        <p className="mt-3 text-xs text-red-300 font-mono">
          Detalle técnico: {error.message}
        </p>
        <button
          onClick={() => reset()}
          className="mt-4 rounded bg-red-800 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700"
        >
          Reintentar
        </button>
      </div>
    </div>
  );
}
