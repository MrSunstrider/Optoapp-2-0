import { NextResponse } from "next/server";
import { PDFDocument, StandardFonts, rgb } from "pdf-lib";

import { fetchPacienteById } from "@/lib/pacientes";
import {
  fetchUltimaEvaluacionParaPdf,
  formatFormulaResumida
} from "@/lib/paciente-clinico";
import { requireUserAndOptica } from "@/lib/api-auth";

export async function GET(
  _request: Request,
  context: { params: Promise<{ id: string }> }
) {
  const { id: pacienteId } = await context.params;
  const session = await requireUserAndOptica();
  if (session instanceof NextResponse) return session;
  const { supabase, optica: activeOptica } = session;

  const paciente = await fetchPacienteById(
    supabase,
    activeOptica.opticaId,
    pacienteId
  );
  if (!paciente) {
    return NextResponse.json({ error: "Paciente no encontrado" }, { status: 404 });
  }

  const ev = await fetchUltimaEvaluacionParaPdf(
    supabase,
    activeOptica.opticaId,
    pacienteId
  );

  if (!ev) {
    return NextResponse.json(
      {
        error: "sin_evaluaciones",
        message: "No hay evaluaciones registradas para generar la fórmula en PDF."
      },
      { status: 400 }
    );
  }

  const pdfDoc = await PDFDocument.create();
  const page = pdfDoc.addPage([595.28, 841.89]);
  const font = await pdfDoc.embedFont(StandardFonts.Helvetica);
  const fontBold = await pdfDoc.embedFont(StandardFonts.HelveticaBold);
  let y = 780;
  const left = 50;
  const line = 14;

  const draw = (text: string, size = 11, bold = false, color = rgb(0, 0, 0)) => {
    page.drawText(text, {
      x: left,
      y,
      size,
      font: bold ? fontBold : font,
      color
    });
    y -= line;
  };

  draw("OptoApp — Fórmula / receta (última evaluación)", 13, true);
  y -= 6;
  draw(`Paciente: ${paciente.nombre_completo}`);
  draw(`Teléfono: ${paciente.telefono}`);
  draw(`Óptica: ${activeOptica.nombre}`);
  y -= 8;

  draw(`Fecha evaluación: ${ev.fecha ?? "—"}`);
  if (ev.motivo_consulta?.trim()) {
    draw(`Motivo: ${ev.motivo_consulta.trim().slice(0, 120)}`);
  }
  y -= 4;
  draw("Fórmula (lejos)", 12, true);
  draw(formatFormulaResumida(ev));
  if (ev.receta_od_av?.trim() || ev.receta_oi_av?.trim()) {
    draw(
      `AV OD: ${ev.receta_od_av?.trim() || "—"}   AV OI: ${ev.receta_oi_av?.trim() || "—"}`
    );
  }
  y -= 4;
  if (ev.diagnostico?.trim()) {
    draw("Diagnóstico", 12, true);
    const diag = ev.diagnostico.trim();
    const max = 90;
    for (let i = 0; i < diag.length; i += max) {
      draw(diag.slice(i, i + max));
    }
  }
  if (ev.observaciones?.trim()) {
    y -= 4;
    draw("Observaciones", 12, true);
    const obs = ev.observaciones.trim();
    const max = 90;
    for (let i = 0; i < obs.length; i += max) {
      draw(obs.slice(i, i + max));
    }
  }

  draw("", 8);
  draw(
    "Documento generado desde OptoApp Web. Validar datos clínicos en expediente.",
    9,
    false,
    rgb(0.35, 0.35, 0.35)
  );

  const pdfBytes = await pdfDoc.save();
  return new NextResponse(Buffer.from(pdfBytes), {
    status: 200,
    headers: {
      "Content-Type": "application/pdf",
      "Content-Disposition": `attachment; filename="receta-${pacienteId.slice(0, 8)}.pdf"`
    }
  });
}
