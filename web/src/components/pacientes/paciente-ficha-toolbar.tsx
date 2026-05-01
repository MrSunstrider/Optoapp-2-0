"use client";

import type { SVGProps } from "react";
import Link from "next/link";
import { useMemo, useState } from "react";

import { canDeletePaciente } from "@/lib/roles";

import { deletePacienteAction } from "@/app/pacientes/_actions/paciente-crud";

function digitsPhone(raw: string): string {
  return raw.replace(/\D/g, "");
}

function firstTokenName(nombreCompleto: string): string {
  const first = nombreCompleto.split(/\s+/).filter(Boolean)[0] ?? "";
  return first || "paciente";
}

function waUrl(phoneDigits: string, text: string): string {
  const core = phoneDigits.replace(/^0+/, "");
  const e164 = core.startsWith("51") ? core : `51${core}`;
  return `https://wa.me/${e164}?text=${encodeURIComponent(text)}`;
}

function IconChat(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={22} height={22} fill="none" stroke="currentColor" strokeWidth={2} {...props}>
      <path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function IconPdf(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={22} height={22} fill="none" stroke="currentColor" strokeWidth={2} {...props}>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function IconPencil(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={22} height={22} fill="none" stroke="currentColor" strokeWidth={2} {...props}>
      <path d="M12 20h9M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function IconTrash(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={22} height={22} fill="none" stroke="currentColor" strokeWidth={2} {...props}>
      <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M10 11v6M14 11v6" strokeLinecap="round" />
    </svg>
  );
}

export function PacienteFichaToolbar({
  pacienteId,
  telefono,
  nombrePaciente,
  proximaCitaIso,
  hasEvaluaciones,
  pdfHref,
  editarHref,
  rol
}: {
  pacienteId: string;
  telefono: string;
  nombrePaciente: string;
  proximaCitaIso: string | null;
  hasEvaluaciones: boolean;
  pdfHref: string;
  editarHref: string;
  rol: string;
}) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [pdfWarnOpen, setPdfWarnOpen] = useState(false);
  const digits = useMemo(() => digitsPhone(telefono), [telefono]);
  const puedeWa = digits.length >= 9;
  const puedeEliminar = canDeletePaciente(rol);

  const { saludo, invitacionAnual, recordatorioCita, fechaCitaLabel } = useMemo(() => {
    const first = firstTokenName(nombrePaciente);
    const saludoText = `Hola ${first}, te saludamos desde la óptica. ¿En qué podemos ayudarte?`;
    const invitacionAnualText = `Hola ${first},

Te escribimos desde la óptica para recordarte la importancia del control optométrico anual. Un chequeo periódico permite detectar a tiempo cambios en tu visión y mantener la salud de tus ojos.

Si deseas agendar tu cita de control, responde a este mensaje y con gusto te coordinamos.

¡Saludos cordiales!`;

    let fechaCita = "";
    let recordatorio = "";
    if (proximaCitaIso) {
      try {
        const d = new Date(proximaCitaIso + "T12:00:00");
        if (!Number.isNaN(d.getTime())) {
          fechaCita = d.toLocaleDateString("es-PE", {
            day: "numeric",
            month: "long",
            year: "numeric"
          });
          recordatorio = `Hola ${first}, te recordamos tu próxima cita en la óptica programada para el ${fechaCita}.`;
        }
      } catch {
        recordatorio = `Hola ${first}, te recordamos tu próxima cita en la óptica. Fecha referencial: ${proximaCitaIso}.`;
        fechaCita = proximaCitaIso;
      }
    }

    return {
      saludo: saludoText,
      invitacionAnual: invitacionAnualText,
      recordatorioCita: recordatorio,
      fechaCitaLabel: fechaCita || proximaCitaIso || ""
    };
  }, [nombrePaciente, proximaCitaIso]);

  const btn =
    "flex h-10 w-10 items-center justify-center rounded-xl text-foreground/70 transition-all hover:bg-muted active:scale-95 disabled:cursor-not-allowed disabled:opacity-40";

  return (
    <div className="flex shrink-0 items-center gap-0.5">
      <div className="relative">
        <button
          type="button"
          onClick={() => setMenuOpen((v) => !v)}
          disabled={!puedeWa}
          className={btn}
          title={puedeWa ? "WhatsApp" : "Teléfono insuficiente para WhatsApp"}
          aria-label="WhatsApp"
        >
          <IconChat />
        </button>
        {menuOpen && puedeWa && (
          <>
            <button
              type="button"
              className="fixed inset-0 z-40 cursor-default"
              aria-label="Cerrar menú"
              onClick={() => setMenuOpen(false)}
            />
            <div className="absolute right-0 top-full z-50 mt-2 min-w-[280px] rounded-2xl border border-border bg-card py-2 text-sm text-foreground shadow-2xl animate-in fade-in slide-in-from-top-2">
              <div className="px-4 py-2 border-b border-border/50 mb-1">
                <p className="text-[10px] font-black uppercase tracking-widest text-primary">Acciones de WhatsApp</p>
              </div>
              <a
                className="block px-4 py-2.5 font-medium hover:bg-primary/10 hover:text-primary transition-colors"
                href={waUrl(digits, saludo)}
                target="_blank"
                rel="noopener noreferrer"
                onClick={() => setMenuOpen(false)}
              >
                💬 Mensaje Libre (Saludo)
              </a>
              <a
                className="block px-4 py-2.5 font-medium hover:bg-primary/10 hover:text-primary transition-colors"
                href={waUrl(digits, invitacionAnual)}
                target="_blank"
                rel="noopener noreferrer"
                onClick={() => setMenuOpen(false)}
              >
                📅 Invitación Control Anual
              </a>
              {proximaCitaIso && recordatorioCita && (
                <a
                  className="block px-4 py-2.5 font-medium hover:bg-primary/10 hover:text-primary transition-colors"
                  href={waUrl(digits, recordatorioCita)}
                  target="_blank"
                  rel="noopener noreferrer"
                  onClick={() => setMenuOpen(false)}
                >
                  ⏰ Recordar Cita ({fechaCitaLabel})
                </a>
              )}
            </div>
          </>
        )}
      </div>

      {hasEvaluaciones ? (
        <a
          href={pdfHref}
          target="_blank"
          rel="noopener noreferrer"
          className={btn}
          title="Exportar fórmula PDF"
          aria-label="Exportar fórmula PDF"
        >
          <IconPdf />
        </a>
      ) : (
        <button
          type="button"
          className={btn}
          title="Sin evaluaciones para PDF"
          aria-label="Sin evaluaciones para PDF"
          onClick={() => setPdfWarnOpen(true)}
        >
          <IconPdf />
        </button>
      )}

      <Link
        href={editarHref}
        className={btn}
        title="Editar perfil"
        aria-label="Editar perfil"
      >
        <IconPencil />
      </Link>

      {puedeEliminar && (
        <button
          type="button"
          className={btn}
          title="Eliminar paciente"
          aria-label="Eliminar paciente"
          onClick={() => setDeleteOpen(true)}
        >
          <IconTrash />
        </button>
      )}

      {pdfWarnOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm p-4">
          <div
            className="absolute inset-0"
            role="presentation"
            onClick={() => setPdfWarnOpen(false)}
          />
          <div className="relative z-10 w-full max-w-md rounded-3xl border border-border bg-card p-6 text-foreground shadow-2xl">
            <h3 className="font-heading text-xl font-bold text-primary">Generar Receta PDF</h3>
            <p className="mt-3 text-sm font-medium text-muted-foreground leading-relaxed">
              No hay evaluaciones clínicas registradas para este paciente. Debes registrar al menos una evaluación antes de poder exportar la fórmula médica.
            </p>
            <div className="mt-6">
              <button
                type="button"
                className="w-full rounded-xl bg-primary px-4 py-3 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-all"
                onClick={() => setPdfWarnOpen(false)}
              >
                ENTENDIDO
              </button>
            </div>
          </div>
        </div>
      )}

      {puedeEliminar && deleteOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div
            className="absolute inset-0"
            role="presentation"
            onClick={() => setDeleteOpen(false)}
          />
          <div className="relative z-10 w-full max-w-md rounded-3xl border border-destructive/20 bg-card p-8 text-foreground shadow-2xl">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-destructive/10 text-destructive">
              <IconTrash width={32} height={32} />
            </div>
            <h3 className="text-center font-heading text-2xl font-black text-destructive">Eliminar Paciente</h3>
            <div className="mt-4 space-y-3 text-center text-sm font-medium text-muted-foreground">
              <p>Esta acción es <span className="text-destructive font-black underline">irreversible</span>.</p>
              <p>Se eliminará el expediente completo, incluyendo evaluaciones y dispensaciones.</p>
            </div>

            <form action={deletePacienteAction} className="mt-8 space-y-4">
              <input type="hidden" name="id" value={pacienteId} />
              <div className="space-y-1">
                <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 text-center mb-2">
                  Escribe <span className="text-foreground">ELIMINAR</span> para confirmar
                </p>
                <input
                  name="confirm"
                  className="w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-center font-bold text-foreground outline-none focus:ring-2 focus:ring-destructive/20 transition-all"
                  placeholder="ELIMINAR"
                  autoComplete="off"
                />
              </div>
              <div className="flex flex-col gap-2">
                <button
                  type="submit"
                  className="rounded-xl bg-destructive px-6 py-4 font-heading text-base font-black text-white shadow-xl shadow-destructive/20 transition-all hover:scale-[1.02] active:scale-95"
                >
                  ELIMINAR PERMANENTEMENTE
                </button>
                <button
                  type="button"
                  className="rounded-xl border border-border px-6 py-3 text-sm font-bold text-muted-foreground hover:bg-muted/50 transition-all"
                  onClick={() => setDeleteOpen(false)}
                >
                  CANCELAR
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
