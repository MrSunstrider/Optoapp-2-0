import { NextResponse } from "next/server";
import { PDFDocument, StandardFonts } from "pdf-lib";

import { fetchDashboardKpis } from "@/lib/dashboard-kpis";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { createClient } from "@/lib/supabase/server";

type ExportType =
  | "pendientes"
  | "cierre"
  | "inventario-csv"
  | "inventario-pdf";

type PendDisp = {
  id: string;
  fecha: string | null;
  ot: string | null;
  monto_total: number | null;
  monto_pagado: number | null;
  estado_entrega: string | null;
};

type PendServ = {
  id: string;
  fecha: string | null;
  ot: string | null;
  descripcion: string | null;
  monto_total: number | null;
  a_cuenta: number | null;
  estado: string | null;
};

type InvRow = {
  id: string;
  sku: string | null;
  marca: string | null;
  modelo: string | null;
  stock_actual: number | null;
  activo: boolean | null;
};

export async function GET(request: Request) {
  const type = getType(new URL(request.url).searchParams.get("type"));
  if (!type) {
    return NextResponse.json({ error: "Tipo de exportación inválido." }, { status: 400 });
  }

  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();
  if (!user) {
    return NextResponse.json({ error: "No autorizado." }, { status: 401 });
  }

  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) {
    return NextResponse.json({ error: "Sin óptica activa." }, { status: 400 });
  }

  if (type === "pendientes") {
    return exportPendientesCsv(supabase, activeOptica.opticaId);
  }
  if (type === "cierre") {
    return exportCierreCsv(supabase, activeOptica.opticaId);
  }
  if (type === "inventario-csv") {
    return exportInventarioCsv(supabase, activeOptica.opticaId);
  }
  return exportInventarioPdf(supabase, activeOptica.opticaId, activeOptica.nombre);
}

async function exportPendientesCsv(
  supabase: Awaited<ReturnType<typeof createClient>>,
  opticaId: string
) {
  const today = dateOnly(new Date());

  const [dispResp, servResp] = await Promise.all([
    supabase
      .from("dispensaciones")
      .select("id,fecha,ot,monto_total,monto_pagado,estado_entrega")
      .eq("optica_id", opticaId)
      .eq("estado_entrega", "Pendiente")
      .lt("fecha", today)
      .abortSignal(AbortSignal.timeout(12_000)),
    supabase
      .from("servicios_extra")
      .select("id,fecha,ot,descripcion,monto_total,a_cuenta,estado")
      .eq("optica_id", opticaId)
      .abortSignal(AbortSignal.timeout(12_000))
  ]);

  if (dispResp.error || servResp.error) {
    return NextResponse.json(
      { error: "No se pudo exportar pendientes." },
      { status: 500 }
    );
  }

  const lines = ["tipo,id,fecha,ot,descripcion,total,pagado_o_cuenta,saldo,estado"];
  for (const row of (dispResp.data ?? []) as PendDisp[]) {
    const total = row.monto_total ?? 0;
    const pagado = row.monto_pagado ?? 0;
    const saldo = total - pagado;
    lines.push(
      toCsvLine([
        "dispensacion",
        row.id,
        row.fecha ?? "",
        row.ot ?? "",
        "",
        total.toFixed(2),
        pagado.toFixed(2),
        saldo.toFixed(2),
        row.estado_entrega ?? ""
      ])
    );
  }

  for (const row of (servResp.data ?? []) as PendServ[]) {
    const total = row.monto_total ?? 0;
    const aCuenta = row.a_cuenta ?? 0;
    const saldo = total - aCuenta;
    const esPendiente = (row.estado ?? "").toLowerCase() === "pendiente" || saldo > 0.005;
    if (!esPendiente) continue;
    lines.push(
      toCsvLine([
        "servicio_extra",
        row.id,
        row.fecha ?? "",
        row.ot ?? "",
        row.descripcion ?? "",
        total.toFixed(2),
        aCuenta.toFixed(2),
        saldo.toFixed(2),
        row.estado ?? ""
      ])
    );
  }

  return csvResponse(lines.join("\n"), `pendientes-${today}.csv`);
}

async function exportCierreCsv(
  supabase: Awaited<ReturnType<typeof createClient>>,
  opticaId: string
) {
  const snapshot = await fetchDashboardKpis(supabase, opticaId);
  const today = dateOnly(new Date());

  const lines = [
    "fecha,cobros_dia,ventas_mes,pacientes_hoy,saldos_pendientes,citas_hoy,entregas_pendientes,servicios_pendientes,stock_critico,estado_snapshot",
    toCsvLine([
      today,
      snapshot.kpis.cobrosDia.toFixed(2),
      snapshot.kpis.ventasMes.toFixed(2),
      String(snapshot.kpis.pacientesHoy),
      snapshot.kpis.saldosPendientes.toFixed(2),
      String(snapshot.kpis.citasHoy),
      String(snapshot.kpis.entregasPendientes),
      String(snapshot.kpis.serviciosPendientes),
      String(snapshot.kpis.stockCritico),
      snapshot.status.overall
    ])
  ];

  return csvResponse(lines.join("\n"), `cierre-operacion-${today}.csv`);
}

async function exportInventarioCsv(
  supabase: Awaited<ReturnType<typeof createClient>>,
  opticaId: string
) {
  const { data, error } = await supabase
    .from("monturas")
    .select("id,sku,marca,modelo,stock_actual,activo")
    .eq("optica_id", opticaId)
    .order("marca", { ascending: true })
    .order("modelo", { ascending: true })
    .abortSignal(AbortSignal.timeout(12_000));

  if (error) {
    return NextResponse.json(
      { error: "No se pudo exportar inventario." },
      { status: 500 }
    );
  }

  const lines = ["id,sku,marca,modelo,stock_actual,activo"];
  for (const row of (data ?? []) as InvRow[]) {
    lines.push(
      toCsvLine([
        row.id,
        row.sku ?? "",
        row.marca ?? "",
        row.modelo ?? "",
        String(row.stock_actual ?? 0),
        row.activo ? "si" : "no"
      ])
    );
  }
  return csvResponse(lines.join("\n"), `inventario-${dateOnly(new Date())}.csv`);
}

async function exportInventarioPdf(
  supabase: Awaited<ReturnType<typeof createClient>>,
  opticaId: string,
  opticaNombre: string
) {
  const { data, error } = await supabase
    .from("monturas")
    .select("id,sku,marca,modelo,stock_actual,activo")
    .eq("optica_id", opticaId)
    .order("marca", { ascending: true })
    .order("modelo", { ascending: true })
    .abortSignal(AbortSignal.timeout(12_000));

  if (error) {
    return NextResponse.json(
      { error: "No se pudo exportar inventario PDF." },
      { status: 500 }
    );
  }

  const rows = (data ?? []) as InvRow[];
  const pdfDoc = await PDFDocument.create();
  const page = pdfDoc.addPage([595.28, 841.89]);
  const font = await pdfDoc.embedFont(StandardFonts.Helvetica);
  const bold = await pdfDoc.embedFont(StandardFonts.HelveticaBold);
  let y = 800;
  const left = 36;

  page.drawText("OptoApp - Inventario de monturas", { x: left, y, size: 14, font: bold });
  y -= 18;
  page.drawText(`Óptica: ${opticaNombre}`, { x: left, y, size: 10, font });
  y -= 14;
  page.drawText(`Fecha: ${dateOnly(new Date())}`, { x: left, y, size: 10, font });
  y -= 18;
  page.drawText("SKU | Marca | Modelo | Stock | Activo", { x: left, y, size: 10, font: bold });
  y -= 12;

  for (const row of rows.slice(0, 55)) {
    const line = `${row.sku ?? "-"} | ${row.marca ?? "-"} | ${row.modelo ?? "-"} | ${
      row.stock_actual ?? 0
    } | ${row.activo ? "SI" : "NO"}`;
    page.drawText(line.slice(0, 110), { x: left, y, size: 9, font });
    y -= 11;
    if (y < 36) break;
  }

  if (rows.length > 55) {
    page.drawText(
      `... (${rows.length - 55} registros adicionales no mostrados en esta página)`,
      { x: left, y: 24, size: 8, font }
    );
  }

  const bytes = await pdfDoc.save();
  return new NextResponse(Buffer.from(bytes), {
    status: 200,
    headers: {
      "Content-Type": "application/pdf",
      "Content-Disposition": `attachment; filename="inventario-${dateOnly(new Date())}.pdf"`
    }
  });
}

function csvResponse(csv: string, filename: string) {
  return new NextResponse(csv, {
    status: 200,
    headers: {
      "Content-Type": "text/csv; charset=utf-8",
      "Content-Disposition": `attachment; filename="${filename}"`,
      "Cache-Control": "no-store"
    }
  });
}

function toCsvLine(values: Array<string>) {
  return values
    .map((value) => {
      const normalized = String(value ?? "");
      if (
        normalized.includes(",") ||
        normalized.includes("\"") ||
        normalized.includes("\n")
      ) {
        return `"${normalized.replace(/"/g, "\"\"")}"`;
      }
      return normalized;
    })
    .join(",");
}

function getType(value: string | null): ExportType | null {
  if (
    value === "pendientes" ||
    value === "cierre" ||
    value === "inventario-csv" ||
    value === "inventario-pdf"
  ) {
    return value;
  }
  return null;
}

function dateOnly(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}
