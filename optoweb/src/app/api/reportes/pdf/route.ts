import { NextResponse } from "next/server";
import { PDFDocument, StandardFonts, rgb } from "pdf-lib";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import {
  fetchReporteFinanciero,
  type ReportePeriodo,
} from "@/lib/reportes-financieros";

function money(value: number): string {
  return new Intl.NumberFormat("es-PE", {
    style: "currency",
    currency: "PEN",
    minimumFractionDigits: 2,
  }).format(value);
}

function parsePeriodo(value: string | null): ReportePeriodo {
  if (value === "dia" || value === "semana" || value === "mes" || value === "anio") return value;
  return "mes";
}

export async function GET(request: Request) {
  try {
    const activeOptica = await getActiveOpticaContext();
    if (!activeOptica) {
      return NextResponse.json({ error: "No hay sesión activa" }, { status: 401 });
    }

    const url = new URL(request.url);
    const periodo = parsePeriodo(url.searchParams.get("periodo"));
    const supabase = await createClient();
    const data = await fetchReporteFinanciero(supabase, activeOptica.opticaId, periodo);

    const doc = await PDFDocument.create();
    const page = doc.addPage([595.28, 841.89]);
    const font = await doc.embedFont(StandardFonts.Helvetica);
    const bold = await doc.embedFont(StandardFonts.HelveticaBold);

    const { width, height } = page.getSize();
    let y = height - 50;

    function drawText(text: string, size: number, boldFont = false, x = 50) {
      page.drawText(text, {
        x,
        y,
        size,
        font: boldFont ? bold : font,
        color: rgb(0, 0, 0),
      });
      y -= size + 6;
    }

    drawText("Reporte Financiero", 20, true, 50);
    y -= 6;
    drawText(`Periodo: ${data.periodoLabel}`, 11, false, 50);
    drawText(`${data.fechaInicio} al ${data.fechaFinExclusiva}`, 10, false, 50);
    y -= 8;

    page.drawLine({ start: { x: 50, y }, end: { x: 545, y }, thickness: 1, color: rgb(0.7, 0.7, 0.7) });
    y -= 16;

    drawText("Ingresos Cobrados:", 12, true, 50);
    drawText(money(data.ingresosCobrados), 14, true, 200);
    y -= 4;

    drawText("Ventas Emitidas:", 12, true, 50);
    drawText(money(data.ventasEmitidas), 14, true, 200);
    y -= 4;

    drawText("Saldo Pendiente:", 12, true, 50);
    drawText(money(data.saldoPendiente), 14, true, 200);
    y -= 4;

    drawText("Movimientos:", 12, true, 50);
    drawText(String(data.totalMovimientos), 14, true, 200);
    y -= 4;

    drawText("Ticket Promedio:", 12, true, 50);
    drawText(money(data.ticketPromedio), 14, true, 200);

    if (data.status.overall === "degraded" && data.status.error) {
      y -= 20;
      drawText(`Nota: ${data.status.error}`, 9, false, 50);
    }

    const bytes = await doc.save();
    const filename = `reporte-financiero-${periodo}-${new Date().toISOString().slice(0, 10)}.pdf`;

    return new NextResponse(Buffer.from(bytes), {
      status: 200,
      headers: {
        "Content-Type": "application/pdf",
        "Content-Disposition": `inline; filename="${filename}"`,
      },
    });
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return NextResponse.json({ error: msg }, { status: 500 });
  }
}
