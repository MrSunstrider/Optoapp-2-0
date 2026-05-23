import { NextResponse } from "next/server";
import { PDFDocument, StandardFonts, rgb } from "pdf-lib";
import { fetchPacienteById } from "@/lib/pacientes";
import { fetchUltimaEvaluacionParaPdf } from "@/lib/paciente-clinico";
import { requireUserAndOptica } from "@/lib/api-auth";

export async function GET(
  _request: Request,
  context: { params: Promise<{ id: string }> }
) {
  const { id: pacienteId } = await context.params;
  const session = await requireUserAndOptica();
  if (session instanceof NextResponse) return session;
  const { supabase, optica: activeOptica } = session;

  const paciente = await fetchPacienteById(supabase, activeOptica.opticaId, pacienteId);
  if (!paciente) return NextResponse.json({ error: "Paciente no encontrado" }, { status: 404 });

  const ev = await fetchUltimaEvaluacionParaPdf(supabase, activeOptica.opticaId, pacienteId);
  if (!ev) return NextResponse.json({ error: "sin_evaluaciones", message: "No hay evaluaciones registradas." }, { status: 400 });

  const pdfDoc = await PDFDocument.create();
  const page = pdfDoc.addPage([595.28, 841.89]);
  const font = await pdfDoc.embedFont(StandardFonts.Helvetica);
  const fontB = await pdfDoc.embedFont(StandardFonts.HelveticaBold);
  let y = 800;
  const M = 50;
  const CW = 595.28 - 2 * M;
  const d = (s: string | null | undefined) => (s?.trim() ? s.trim() : "—");

  const txt = (text: string, size = 10, bold = false, color = rgb(0, 0, 0), center = false) => {
    const x = center ? M + (CW - font.widthOfTextAtSize(text, size)) / 2 : M;
    page.drawText(text, { x, y, size, font: bold ? fontB : font, color });
    y -= size + 4;
  };

  txt("FÓRMULA OPTOMÉTRICA", 18, true, rgb(0, 0, 0), true);
  txt(activeOptica.nombre, 12, true, rgb(0.35, 0.35, 0.35), true);
  y -= 4;

  // ── Patient info card ──
  txt("Paciente", 13, true, rgb(0.12, 0.36, 0.55), true);
  const col2 = M + CW / 2;
  const bold = (label: string, value: string, x: number) => {
    page.drawText(label, { x, y, size: 10, font: fontB, color: rgb(0, 0, 0) });
    page.drawText(value, { x: x + font.widthOfTextAtSize(label, 10), y, size: 10, font, color: rgb(0, 0, 0) });
  };

  bold("Nombre: ", paciente.nombre_completo, M + 8);
  bold("Edad: ", `${paciente.edad ?? "—"} años`, col2 + 8);
  y -= 16;

  const ho = paciente.historia_optometrica?.trim() || "—";
  const dni = paciente.dni?.trim() || "—";
  bold("HO: ", ho, M + 8);
  bold("DNI: ", dni, col2 + 8);
  y -= 16;

  const cel = paciente.telefono?.trim() || "—";
  const dis = paciente.distrito?.trim() || "—";
  bold("Celular: ", cel, M + 8);
  bold("Distrito: ", dis, col2 + 8);
  y -= 16;

  bold("Evaluacion: ", ev.fecha ?? "—", M + 8);
  y -= 4;

  const lw = 30, uw = (CW - lw) / 7;
  const xE = M + lw, xC = xE + uw, xJ = xC + uw, xD = xJ + uw, xA = xD + uw, xAA = xA + uw, xR = M + CW;
  const cw = [lw, uw, uw, uw, uw, uw, uw * 2];

  function hdr(cells: string[], xs: number[], ws: number[]) {
    page.drawRectangle({ x: M, y: y - 2, width: CW, height: 16, color: rgb(0.9, 0.93, 0.97) });
    cells.forEach((c, i) => page.drawText(c, { x: xs[i] + (ws[i] - font.widthOfTextAtSize(c, 9)) / 2, y: y + 1, size: 9, font: fontB }));
    y -= 16;
  }
  function row(cells: string[], xs: number[], ws: number[], alt = false) {
    if (alt) page.drawRectangle({ x: M, y: y - 2, width: CW, height: 16, color: rgb(0.97, 0.97, 0.97) });
    cells.forEach((c, i) => page.drawText(c, { x: xs[i] + (ws[i] - font.widthOfTextAtSize(c, 9)) / 2, y: y + 1, size: 9, font }));
    y -= 16;
  }
  function grid(rows: number, divs: number[]) {
    for (let r = 0; r < rows; r++) {
      const ry = y + r * 16;
      page.drawLine({ start: { x: divs[0], y: ry }, end: { x: M + CW, y: ry }, thickness: 0.5, color: rgb(0.8, 0.8, 0.8) });
      divs.forEach(dx => page.drawLine({ start: { x: dx, y: ry }, end: { x: dx, y: ry + 16 }, thickness: 0.5, color: rgb(0.8, 0.8, 0.8) }));
    }
  }

  // ── 1. Visión Lejana ──
  txt("Visión Lejana", 11, true, rgb(0.12, 0.36, 0.55), true);
  hdr(["VL", "Esfera", "Cilindro", "Eje", "DIP/DNP", "AV", "AV / AO"], [M, xE, xC, xJ, xD, xA, xAA], cw);
  [
    ["OD", d(ev.receta_od_esf), d(ev.receta_od_cil), d(ev.receta_od_eje), d(ev.dip_lejos), d(ev.receta_od_av), d(ev.av_cc_ao_px)],
    ["OI", d(ev.receta_oi_esf), d(ev.receta_oi_cil), d(ev.receta_oi_eje), d(ev.dip_cerca), d(ev.receta_oi_av), d(ev.av_cc_ao_px)],
  ].forEach((r, i) => row(r, [M, xE, xC, xJ, xD, xA, xAA], cw, i > 0));
  grid(3, [M, xE, xC, xJ, xD, xA, xAA, xR]);
  y -= 4;

  // ── 2. Visión Próxima ──
  txt("Visión Próxima", 11, true, rgb(0.12, 0.36, 0.55), true);
  const nc = (uw * 3) / 2;
  hdr(["VP", "Cerca", "Intermedia", "DIP/DNP", "AV", "AV / AO"], [M, xE, xE + nc, xD, xA, xAA], [lw, nc, nc, uw, uw, uw * 2]);
  [
    ["OD", d(ev.add_cerca_od), d(ev.add_intermedia_od), d(ev.dip_cerca), d(ev.av_cc_od_cerca), d(ev.av_cc_ao_cerca)],
    ["OI", d(ev.add_cerca_oi), d(ev.add_intermedia_oi), d(ev.dip_cerca), d(ev.av_cc_oi_cerca), d(ev.av_cc_ao_cerca)],
  ].forEach((r, i) => row(r, [M, xE, xE + nc, xD, xA, xAA], [lw, nc, nc, uw, uw, uw * 2], i > 0));
  grid(3, [M, xE, xE + nc, xD, xA, xAA, xR]);
  y -= 4;

  // ── 3. Prisma ──
  const pw = uw * 4, pD = M + uw, pB = pD + uw, pJ = pB + uw, pR = pJ + uw;
  txt("Prisma", 11, true, rgb(0.12, 0.36, 0.55), true);
  const tX = M + uw / 2, tY = y + 12, tS = 7;
  page.drawSvgPath(`M${tX},${tY - tS} L${tX + tS},${tY + tS} L${tX - tS},${tY + tS} Z`, { color: rgb(0.12, 0.36, 0.55), opacity: 0.4 });
  page.drawRectangle({ x: M, y: y - 2, width: pw, height: 16, color: rgb(0.9, 0.93, 0.97) });
  ["", "PD", "Base", "Eje"].forEach((c, i) => {
    const px = [M, pD, pB, pJ][i];
    page.drawText(c, { x: px + (uw - font.widthOfTextAtSize(c, 9)) / 2, y: y + 1, size: 9, font: fontB });
  });
  y -= 16;
  [
    ["OD", d(ev.prisma_od_valor), d(ev.prisma_od_base), "—"],
    ["OI", d(ev.prisma_oi_valor), d(ev.prisma_oi_base), "—"],
  ].forEach((r, i) => {
    if (i > 0) page.drawRectangle({ x: M, y: y - 2, width: pw, height: 16, color: rgb(0.97, 0.97, 0.97) });
    r.forEach((c, j) => { const px = [M, pD, pB, pJ][j]; page.drawText(c, { x: px + (uw - font.widthOfTextAtSize(c, 9)) / 2, y: y + 1, size: 9, font }); });
    y -= 16;
  });
  for (let r = 0; r < 3; r++) {
    const ry = y + r * 16;
    page.drawLine({ start: { x: M, y: ry }, end: { x: pR, y: ry }, thickness: 0.5, color: rgb(0.8, 0.8, 0.8) });
    [M, pD, pB, pJ, pR].forEach(dx => page.drawLine({ start: { x: dx, y: ry }, end: { x: dx, y: ry + 16 }, thickness: 0.5, color: rgb(0.8, 0.8, 0.8) }));
  }
  page.drawLine({ start: { x: M, y }, end: { x: pR, y }, thickness: 0.5, color: rgb(0.8, 0.8, 0.8) });
  y -= 4;

  // ── Diagnóstico ──
  const diag: string[] = [];
  if (ev.diagnostico_od?.trim()) diag.push(`OD: ${ev.diagnostico_od.trim()}`);
  if (ev.diagnostico_oi?.trim()) diag.push(`OI: ${ev.diagnostico_oi.trim()}`);
  if (ev.otros_presbicia) diag.push("Presbicia");
  diag.push("Tratamiento: Uso de lentes correctores.");
  txt("Diagnóstico", 11, true, rgb(0.12, 0.36, 0.55));
  diag.forEach(d => txt(d, 10, ["OD:", "OI:"].some(p => d.startsWith(p))));
  y -= 4;

  txt("", 4);
  txt("Documento generado desde OptoApp Web. Validar datos clínicos en expediente.", 8, false, rgb(0.35, 0.35, 0.35), true);

  const pdfBytes = await pdfDoc.save();
  return new NextResponse(Buffer.from(pdfBytes), {
    status: 200,
    headers: { "Content-Type": "application/pdf", "Content-Disposition": `inline; filename="receta-${pacienteId.slice(0, 8)}.pdf"` }
  });
}
