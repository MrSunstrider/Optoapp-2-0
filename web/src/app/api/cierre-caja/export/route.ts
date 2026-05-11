import { NextResponse } from "next/server";
import { PDFDocument, StandardFonts } from "pdf-lib";

import {
  fetchCierreCaja,
  formatFechaLarga,
  money,
  normalizeFechaCierre
} from "@/lib/cierre-caja";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { createClient } from "@/lib/supabase/server";

export async function GET(request: Request) {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();
  if (!user) return NextResponse.json({ error: "No autorizado." }, { status: 401 });

  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) {
    return NextResponse.json({ error: "Sin óptica activa." }, { status: 400 });
  }

  const url = new URL(request.url);
  const fecha = normalizeFechaCierre(url.searchParams.get("fecha") ?? undefined);
  const format = url.searchParams.get("format") === "csv" ? "csv" : "pdf";
  const [fiscal, snapshot] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    fetchCierreCaja(supabase, activeOptica.opticaId, fecha)
  ]);

  if (format === "csv") {
    const lines = [
      "fecha,tipo_operacion,medio_pago,monto,referencia",
      ...snapshot.transacciones.map((t) =>
        toCsv([fecha, t.tipoOperacion, t.medioPago, t.monto.toFixed(2), t.referencia])
      )
    ];
    lines.push(
      toCsv(["TOTAL", "Efectivo", "", snapshot.efectivo.toFixed(2), ""]),
      toCsv(["TOTAL", "Móvil/Trans", "", snapshot.movilTrans.toFixed(2), ""]),
      toCsv(["TOTAL", "Tarjeta", "", snapshot.tarjeta.toFixed(2), ""]),
      toCsv(["TOTAL", "Recaudado", "", snapshot.total.toFixed(2), ""])
    );
    return new NextResponse(lines.join("\n"), {
      status: 200,
      headers: {
        "Content-Type": "text/csv; charset=utf-8",
        "Content-Disposition": `attachment; filename="cierre-caja-monturas-${fecha}.csv"`
      }
    });
  }

  const doc = await PDFDocument.create();
  const page = doc.addPage([595, 842]);
  const font = await doc.embedFont(StandardFonts.Helvetica);
  const bold = await doc.embedFont(StandardFonts.HelveticaBold);
  let y = 800;
  const x = 36;
  page.drawText("Cierre de Caja - Monturas", { x, y, size: 14, font: bold });
  y -= 18;
  page.drawText(formatOpticaActivaLine(activeOptica.nombre, fiscal), { x, y, size: 10, font });
  y -= 14;
  page.drawText(`Fecha de reporte: ${formatFechaLarga(fecha)}`, { x, y, size: 10, font });
  y -= 14;
  page.drawText(
    `Efectivo ${money(snapshot.efectivo)} | Móvil/Trans ${money(snapshot.movilTrans)} | Tarjeta ${money(snapshot.tarjeta)}`,
    { x, y, size: 10, font }
  );
  y -= 14;
  page.drawText(`TOTAL RECAUDADO ${money(snapshot.total)}`, { x, y, size: 11, font: bold });
  y -= 18;
  page.drawText("Detalle de transacciones", { x, y, size: 11, font: bold });
  y -= 12;
  for (const tx of snapshot.transacciones.slice(0, 60)) {
    const line = `${tx.tipoOperacion} | ${tx.medioPago} | ${money(tx.monto)} | ${tx.referencia}`;
    page.drawText(line.slice(0, 110), { x, y, size: 9, font });
    y -= 11;
    if (y < 24) break;
  }

  const bytes = await doc.save();
  return new NextResponse(Buffer.from(bytes), {
    status: 200,
    headers: {
      "Content-Type": "application/pdf",
      "Content-Disposition": `attachment; filename="cierre-caja-monturas-${fecha}.pdf"`
    }
  });
}

function toCsv(values: string[]) {
  return values
    .map((v) => (v.includes(",") || v.includes("\"") ? `"${v.replace(/"/g, "\"\"")}"` : v))
    .join(",");
}
