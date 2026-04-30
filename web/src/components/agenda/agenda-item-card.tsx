import Link from "next/link";
import { formatAgendaDate, type AgendaItem } from "@/lib/agenda";

const ESTADO_STYLE: Record<string, string> = {
  programada: "bg-sky-500/20 text-sky-200 ring-sky-500/40",
  confirmada: "bg-emerald-500/20 text-emerald-200 ring-emerald-500/40",
  atendida: "bg-zinc-500/20 text-zinc-200 ring-zinc-500/40",
  cancelada: "bg-red-500/20 text-red-200 ring-red-500/40"
};

export function AgendaItemCard({
  item,
  canWrite
}: {
  item: AgendaItem;
  canWrite: boolean;
}) {
  const badge = ESTADO_STYLE[item.estado] ?? ESTADO_STYLE.programada;

  return (
    <li className="rounded-xl border border-zinc-700 bg-[#3F3D4A]/70 p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="space-y-1">
          <p className="text-sm text-zinc-300">{formatAgendaDate(item.fechaIso)}</p>
          <p className="text-base font-semibold text-white">{item.pacienteNombre}</p>
          {item.motivo ? <p className="text-sm text-zinc-200">{item.motivo}</p> : null}
          {item.contacto ? (
            <p className="text-xs text-zinc-400">Contacto: {item.contacto}</p>
          ) : null}
        </div>
        <div className="flex flex-col items-end gap-2">
          {item.hora ? <p className="text-sm font-medium text-zinc-100">{item.hora}</p> : null}
          <span className={`rounded-full px-2.5 py-0.5 text-xs ring-1 ring-inset ${badge}`}>
            {item.estado}
          </span>
        </div>
      </div>
      <div className="mt-3 flex flex-wrap gap-2 text-xs">
        {item.pacienteId ? (
          <Link
            href={`/pacientes/${item.pacienteId}/evaluaciones`}
            className="rounded-lg border border-zinc-600 px-2 py-1 text-zinc-100 hover:bg-white/5"
          >
            Ver detalle
          </Link>
        ) : (
          <button
            type="button"
            disabled
            className="rounded-lg border border-zinc-700 px-2 py-1 text-zinc-500"
          >
            Ver detalle
          </button>
        )}
        <button
          type="button"
          disabled={!canWrite}
          className="rounded-lg border border-zinc-600 px-2 py-1 text-zinc-100 hover:bg-white/5 disabled:opacity-50"
        >
          Editar cita
        </button>
        <button
          type="button"
          disabled={!canWrite}
          className="rounded-lg border border-zinc-600 px-2 py-1 text-zinc-100 hover:bg-white/5 disabled:opacity-50"
        >
          Reprogramar / Cancelar
        </button>
      </div>
    </li>
  );
}
