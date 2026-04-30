import { NextResponse } from "next/server";
import { PDFDocument, StandardFonts } from "pdf-lib";

import { fetchMonturasInventario, computeInventarioSummary, soles } from "@/lib/inventario";
import { formatOpticaActivaLine } from "@/lib/optica-display";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { createClient } from "@/lib/supabase/server";

export async function GET(request: Request) {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();
  if (!user) {
    return NextResponse.json({ error: "No autorizado" }, { status: 401 });
  }

  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) {
    return NextResponse.json({ error: "Sin óptica activa" }, { status: 400 });
  }
  const q = new URL(request.url).searchParams.get("q") ?? "";
  const [fiscal, items] = await Promise.all([
    fetchOpticaFiscal(supabase, activeOptica.opticaId),
    fetchMonturasInventario(supabase, activeOptica.opticaId, q)
  ]);
  const summary = computeInventarioSummary(items);

  const doc = await PDFDocument.create();
  const page = doc.addPage([595, 842]);
  const font = await doc.embedFont(StandardFonts.Helvetica);
  const bold = await doc.embedFont(StandardFonts.HelveticaBold);
  let y = 800;
  const x = 36;
  page.drawText("Inventario de monturas - OptoApp", { x, y, font: bold, size: 14 });
  y -= 18;
  page.drawText(formatOpticaActivaLine(activeOptica.nombre, fiscal), { x, y, font, size: 10 });
  y -= 14;
  page.drawText(`Generado: ${new Date().toLocaleString("es-PE")}`, { x, y, font, size: 10 });
  y -= 18;
  page.drawText(
    `Monturas: ${summary.listed} | Stock: ${summary.stockTotal} | Costo: ${soles(
      summary.valorCosto
    )} | Venta: ${soles(summary.valorVenta)}`,
    { x, y, font, size: 10 }
  );
  y -= 20;
  page.drawText("SKU | Marca | Modelo | Stock | Min", { x, y, font: bold, size: 10 });
  y -= 12;
  for (const item of items.slice(0, 55)) {
    const line = `${item.sku} | ${item.marca} | ${item.modelo} | ${item.stockActual} | ${item.stockMinimo}`;
    page.drawText(line.slice(0, 110), { x, y, font, size: 9 });
    y -= 11;
    if (y < 30) break;
  }

  const bytes = await doc.save();
  const date = new Date();
  const filename = `inventario-monturas-${date.getFullYear()}-${String(
    date.getMonth() + 1
  ).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}.pdf`;

  return new NextResponse(Buffer.from(bytes), {
    status: 200,
    headers: {
      "Content-Type": "application/pdf",
      "Content-Disposition": `attachment; filename="${filename}"`
    }
  });
}
