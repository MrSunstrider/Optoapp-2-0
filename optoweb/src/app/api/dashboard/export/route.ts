import { NextResponse } from "next/server";
import { PDFDocument, StandardFonts } from "pdf-lib";
import { z } from "zod";

import { fetchDashboardKpis } from "@/lib/dashboard-kpis";
import { canViewBiAndReports, canAccessModule } from "@/lib/roles";
import { dateOnly } from "@/lib/date-utils";
import { createClient } from "@/lib/supabase/server";
import { requireUserAndOptica } from "@/lib/api-auth";

const ExportTypeSchema = z.enum([
  "pendientes",
  "cierre",
  "inventario-csv",
  "inventario-pdf"
]);
type ExportType = z.infer<typeof ExportTypeSchema>;

const PendDispSchema = z.object({
  id: z.string(),
  fecha: z.string().nullable(),
  ot: z.string().nullable(),
  monto_total: z.number().nullable(),
  monto_pagado: z.number().nullable(),
  estado_entrega: z.string().nullable()
});

const PendServSchema = z.object({
  id: z.string(),
  fecha: z.string().nullable(),
  ot: z.string().nullable(),
  descripcion: z.string().nullable(),
  monto_total: z.number().nullable(),
  a_cuenta: z.number().nullable(),
  estado: z.string().nullable()
});

const InvRowSchema = z.object({
  id: z.string(),
  sku: z.string().nullable(),
  marca: z.string().nullable(),
  modelo: z.string().nullable(),
  stock_actual: z.number().nullable(),
  activo: z.boolean().nullable()
});

type PendDisp = z.infer<typeof PendDispSchema>;
type PendServ = z.infer<typeof PendServSchema>;
type InvRow = z.infer<typeof InvRowSchema>;

export async function GET(request: Request) {
  const typeResult = ExportTypeSchema.safeParse(
    new URL(request.url).searchParams.get("type")
  );
  if (!typeResult.success) {
    return NextResponse.json({ error: "Tipo de exportación inválido." }, { status: 400 });
  }
  const type = typeResult.data;

  const session = await requireUserAndOptica();
  if (session instanceof NextResponse) return session;
  const { supabase, user, optica: activeOptica } = session;

  if (
    (type === "pendientes" || type === "cierre") &&
    !canViewBiAndReports(activeOptica.rol)
  ) {
    return NextResponse.json(
      { error: "No autorizado para exportar reportes financieros." },
      { status: 403 }
    );
  }

  if (
    (type === "inventario-csv" || type === "inventario-pdf") &&
    !canAccessModule(activeOptica.rol, "inventario")
  ) {
    return NextResponse.json(
      { error: "No autorizado para exportar inventario." },
      { status: 403 }
    );
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
      .limit(5000)
      .abortSignal(AbortSignal.timeout(12_000)),
    supabase
      .from("servicios_extra")
      .select("id,fecha,ot,descripcion,monto_total,a_cuenta,estado")
      .eq("optica_id", opticaId)
      .limit(5000)
      .abortSignal(AbortSignal.timeout(12_000))
  ]);

  if (dispResp.error || servResp.error) {
    return NextResponse.json(
      { error: "No se pudo exportar pendientes." },
      { status: 500 }
    );
  }

  const dispRows = z.array(PendDispSchema).parse(dispResp.data ?? []);
  const servRows = z.array(PendServSchema).parse(servResp.data ?? []);

  const lines = ["tipo,id,fecha,ot,descripcion,total,pagado_o_cuenta,saldo,estado"];
  for (const row of dispRows) {
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

  for (const row of servRows) {
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
  opticaId: string,
  limit = 10000
) {
  const { data, error } = await supabase
    .from("monturas")
    .select("id,sku,marca,modelo,stock_actual,activo")
    .eq("optica_id", opticaId)
    .order("marca", { ascending: true })
    .order("modelo", { ascending: true })
    .limit(limit)
    .abortSignal(AbortSignal.timeout(12_000));

  if (error) {
    return NextResponse.json(
      { error: "No se pudo exportar inventario." },
      { status: 500 }
    );
  }

  const rows = z.array(InvRowSchema).parse(data ?? []);
  const lines = ["id,sku,marca,modelo,stock_actual,activo"];
  for (const row of rows) {
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

  const rows = z.array(InvRowSchema).parse(data ?? []);
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
      "Content-Disposition": `inline; filename="inventario-${dateOnly(new Date())}.pdf"`
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


