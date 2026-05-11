"use client";

export default function AgendaError() {
  return (
    <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
      <div className="mx-auto w-full max-w-4xl rounded-xl border border-red-700/50 bg-red-950/20 p-4">
        <h2 className="text-lg font-semibold text-red-200">Agenda no disponible</h2>
        <p className="mt-1 text-sm text-red-100">
          Ocurrió un error inesperado al cargar la agenda.
        </p>
      </div>
    </div>
  );
}
