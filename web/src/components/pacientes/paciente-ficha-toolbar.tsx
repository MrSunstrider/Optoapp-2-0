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
    "flex h-10 w-10 items-center justify-center rounded-lg text-zinc-900 transition-colors hover:bg-black/10 disabled:cursor-not-allowed disabled:opacity-40";

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
            <div className="absolute right-0 top-full z-50 mt-1 min-w-[260px] rounded-md border border-zinc-600 bg-zinc-900 py-1 text-sm text-zinc-100 shadow-xl">
              <a
                className="block px-3 py-2 hover:bg-zinc-800"
                href={waUrl(digits, saludo)}
                target="_blank"
                rel="noopener noreferrer"
                onClick={() => setMenuOpen(false)}
              >
                Mensaje Libre (Saludo)
              </a>
              <a
                className="block px-3 py-2 hover:bg-zinc-800"
                href={waUrl(digits, invitacionAnual)}
                target="_blank"
                rel="noopener noreferrer"
                onClick={() => setMenuOpen(false)}
              >
                Invitación Control Anual
              </a>
              {proximaCitaIso && recordatorioCita && (
                <a
                  className="block px-3 py-2 hover:bg-zinc-800"
                  href={waUrl(digits, recordatorioCita)}
                  target="_blank"
                  rel="noopener noreferrer"
                  onClick={() => setMenuOpen(false)}
                >
                  Recordar Próxima Cita ({fechaCitaLabel})
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div
            className="absolute inset-0"
            role="presentation"
            onClick={() => setPdfWarnOpen(false)}
          />
          <div className="relative z-10 w-full max-w-md rounded-xl border border-zinc-700 bg-zinc-900 p-5 text-zinc-100 shadow-xl">
            <h3 className="text-lg font-semibold text-sky-400">Fórmula en PDF</h3>
            <p className="mt-2 text-sm text-zinc-400">
              No hay evaluaciones clínicas registradas para este paciente. No se puede generar la
              fórmula en PDF hasta registrar al menos una evaluación.
            </p>
            <div className="mt-4 flex justify-end">
              <button
                type="button"
                className="rounded-lg border border-zinc-600 px-4 py-2 text-sm hover:bg-zinc-800"
                onClick={() => setPdfWarnOpen(false)}
              >
                Entendido
              </button>
            </div>
          </div>
        </div>
      )}

      {puedeEliminar && deleteOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div
            className="absolute inset-0"
            role="presentation"
            onClick={() => setDeleteOpen(false)}
          />
          <div className="relative z-10 w-full max-w-md rounded-xl border border-zinc-700 bg-zinc-900 p-5 text-zinc-100 shadow-xl">
            <h3 className="text-lg font-semibold text-red-400">Eliminar paciente</h3>
            <p className="mt-2 text-sm text-zinc-400">
              Esta acción elimina el expediente del paciente y, en cascada, sus evaluaciones,
              dispensaciones y servicios vinculados en esta óptica. No se puede deshacer.
            </p>
            <p className="mt-2 text-sm text-zinc-400">
              Por seguridad, cada usuario tiene un límite diario de eliminaciones en la óptica (10
              por día). Si alcanzas el límite, deberás esperar al día siguiente.
            </p>
            <p className="mt-2 text-sm text-zinc-500">
              Solo administradores o gerentes pueden eliminar. Confirma escribiendo{" "}
              <span className="font-mono text-zinc-300">ELIMINAR</span> abajo.
            </p>
            <form action={deletePacienteAction} className="mt-4 space-y-3">
              <input type="hidden" name="id" value={pacienteId} />
              <input
                name="confirm"
                className="w-full rounded-lg border border-zinc-600 bg-black px-3 py-2 text-sm"
                placeholder="Escribe ELIMINAR"
                autoComplete="off"
              />
              <div className="flex gap-2">
                <button
                  type="submit"
                  className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-500"
                >
                  Eliminar
                </button>
                <button
                  type="button"
                  className="rounded-lg border border-zinc-600 px-4 py-2 text-sm hover:bg-zinc-800"
                  onClick={() => setDeleteOpen(false)}
                >
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
