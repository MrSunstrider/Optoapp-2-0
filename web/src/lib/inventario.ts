import type { SupabaseClient } from "@supabase/supabase-js";
import { assertNoDbError } from "@/lib/supabase/db-error";

type MonturaBaseRow = {
  id: string;
  sku: string | null;
  marca: string | null;
  modelo: string | null;
  color?: string | null;
  stock_actual: number | null;
  stock_minimo?: number | null;
  costo_unitario?: number | null;
  precio_venta?: number | null;
  activo: boolean | null;
};

export type MonturaItem = {
  id: string;
  sku: string;
  marca: string;
  modelo: string;
  color: string;
  stockActual: number;
  stockMinimo: number;
  costoUnitario: number;
  precioVenta: number;
  activo: boolean;
};

export type InventarioSummary = {
  listed: number;
  stockTotal: number;
  valorCosto: number;
  valorVenta: number;
  stockAlerts: number;
};

export async function fetchMonturasInventario(
  supabase: SupabaseClient,
  opticaId: string,
  q: string
): Promise<MonturaItem[]> {
  const term = q.trim().toLowerCase();
  const rows = await queryMonturasSafe(supabase, opticaId);
  const mapped = rows.map(mapMontura);
  if (!term) return mapped;
  return mapped.filter((m) => {
    return (
      m.sku.toLowerCase().includes(term) ||
      m.marca.toLowerCase().includes(term) ||
      m.modelo.toLowerCase().includes(term) ||
      m.color.toLowerCase().includes(term)
    );
  });
}

export function computeInventarioSummary(items: MonturaItem[]): InventarioSummary {
  const stockTotal = items.reduce((acc, item) => acc + item.stockActual, 0);
  const valorCosto = items.reduce(
    (acc, item) => acc + item.stockActual * item.costoUnitario,
    0
  );
  const valorVenta = items.reduce(
    (acc, item) => acc + item.stockActual * item.precioVenta,
    0
  );
  const stockAlerts = items.filter((item) => item.stockActual <= item.stockMinimo).length;
  return {
    listed: items.length,
    stockTotal,
    valorCosto,
    valorVenta,
    stockAlerts
  };
}

export function soles(n: number): string {
  return `S/ ${n.toFixed(2)}`;
}

async function queryMonturasSafe(
  supabase: SupabaseClient,
  opticaId: string
): Promise<MonturaBaseRow[]> {
  const extended = await supabase
    .from("monturas")
    .select("id,sku,marca,modelo,color,stock_actual,stock_minimo,costo_unitario,precio_venta,activo")
    .eq("optica_id", opticaId)
    .order("marca", { ascending: true })
    .order("modelo", { ascending: true })
    .abortSignal(AbortSignal.timeout(12_000));

  if (!extended.error) return (extended.data ?? []) as MonturaBaseRow[];

  const basic = await supabase
    .from("monturas")
    .select("id,sku,marca,modelo,stock_actual,activo")
    .eq("optica_id", opticaId)
    .order("marca", { ascending: true })
    .order("modelo", { ascending: true })
    .abortSignal(AbortSignal.timeout(12_000));
  assertNoDbError(basic.error, "Inventario monturas");
  return (basic.data ?? []) as MonturaBaseRow[];
}

function mapMontura(row: MonturaBaseRow): MonturaItem {
  return {
    id: row.id,
    sku: row.sku?.trim() || row.id.slice(0, 8),
    marca: row.marca?.trim() || "Sin marca",
    modelo: row.modelo?.trim() || "Sin modelo",
    color: row.color?.trim() || "",
    stockActual: Number(row.stock_actual ?? 0),
    stockMinimo:
      row.stock_minimo != null && Number.isFinite(Number(row.stock_minimo))
        ? Number(row.stock_minimo)
        : 2,
    costoUnitario:
      row.costo_unitario != null && Number.isFinite(Number(row.costo_unitario))
        ? Number(row.costo_unitario)
        : 0,
    precioVenta:
      row.precio_venta != null && Number.isFinite(Number(row.precio_venta))
        ? Number(row.precio_venta)
        : 0,
    activo: row.activo !== false
  };
}
