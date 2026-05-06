import Link from "next/link";
import { formatAgendaDate, type AgendaItem } from "@/lib/agenda";
import { Calendar, Clock, Phone, User } from "lucide-react";

const ESTADO_STYLE: Record<string, string> = {
  programada: "bg-primary/10 text-primary ring-primary/30",
  confirmada: "bg-emerald-500/10 text-emerald-500 ring-emerald-500/30",
  atendida: "bg-foreground/10 text-muted-foreground ring-foreground/20",
  cancelada: "bg-destructive/10 text-destructive ring-destructive/30"
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
    <li className="rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:shadow-md">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0 flex-1 space-y-2">
          <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">
            <Calendar className="h-3 w-3" />
            {formatAgendaDate(item.fechaIso)}
          </div>
          <p className="font-heading text-lg font-bold text-foreground truncate flex items-center gap-2">
            <User className="h-4 w-4 text-primary" />
            {item.pacienteNombre}
          </p>
          {item.motivo ? (
            <p className="text-sm font-medium text-foreground/70 italic">&quot;{item.motivo}&quot;</p>
          ) : null}
          {item.contacto ? (
            <p className="text-xs font-medium text-muted-foreground flex items-center gap-2">
              <Phone className="h-3 w-3" />
              {item.contacto}
            </p>
          ) : null}
        </div>
        <div className="flex flex-col items-end gap-2 shrink-0">
          {item.hora ? (
            <div className="flex items-center gap-1.5 font-heading text-xl font-black text-primary">
              <Clock className="h-5 w-5" />
              {item.hora}
            </div>
          ) : null}
          <span className={`rounded-full px-3 py-1 text-[10px] font-black uppercase tracking-widest ring-1 ring-inset ${badge}`}>
            {item.estado}
          </span>
        </div>
      </div>
      <div className="mt-5 flex flex-wrap gap-2">
        {item.pacienteId ? (
          <Link
            href={`/pacientes/${item.pacienteId}/evaluaciones`}
            className="rounded-xl border border-border bg-foreground/[0.03] px-3 py-1.5 text-[10px] font-bold text-foreground hover:bg-primary hover:text-primary-foreground hover:border-primary transition-all"
          >
            VER DETALLE
          </Link>
        ) : (
          <button
            type="button"
            disabled
            className="rounded-xl border border-border bg-muted/30 px-3 py-1.5 text-[10px] font-bold text-muted-foreground/40"
          >
            VER DETALLE
          </button>
        )}
        <button
          type="button"
          disabled={!canWrite}
          className="rounded-xl border border-border bg-foreground/[0.03] px-3 py-1.5 text-[10px] font-bold text-foreground hover:bg-foreground/10 transition-all disabled:opacity-20"
        >
          EDITAR CITA
        </button>
        <button
          type="button"
          disabled={!canWrite}
          className="rounded-xl border border-border bg-foreground/[0.03] px-3 py-1.5 text-[10px] font-bold text-foreground hover:bg-destructive hover:text-destructive-foreground hover:border-destructive transition-all disabled:opacity-20"
        >
          REPROGRAMAR
        </button>
      </div>
    </li>
  );
}
