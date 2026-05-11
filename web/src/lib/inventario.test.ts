import { describe, it, expect } from "vitest";
import {
  computeInventarioSummary,
  soles,
  mapMontura,
} from "@/lib/inventario";
import type { MonturaItem } from "@/lib/inventario";

function makeMonturaItem(overrides?: Partial<MonturaItem>): MonturaItem {
  return {
    id: "test-id",
    sku: "SKU-001",
    marca: "Ray-Ban",
    modelo: "Wayfarer",
    color: "Negro",
    stockActual: 10,
    stockMinimo: 2,
    costoUnitario: 50,
    precioVenta: 150,
    activo: true,
    ...overrides,
  };
}

describe("computeInventarioSummary", () => {
  it("returns zeros for empty array", () => {
    const result = computeInventarioSummary([]);
    expect(result).toEqual({
      listed: 0,
      stockTotal: 0,
      valorCosto: 0,
      valorVenta: 0,
      stockAlerts: 0,
    });
  });

  it("returns correct counts for a single item", () => {
    const result = computeInventarioSummary([makeMonturaItem()]);
    expect(result.listed).toBe(1);
    expect(result.stockTotal).toBe(10);
    expect(result.valorCosto).toBe(500); // 10 * 50
    expect(result.valorVenta).toBe(1500); // 10 * 150
    expect(result.stockAlerts).toBe(0);
  });

  it("aggregates multiple items correctly", () => {
    const items = [
      makeMonturaItem({ stockActual: 5, costoUnitario: 30, precioVenta: 100 }),
      makeMonturaItem({ stockActual: 3, costoUnitario: 40, precioVenta: 120 }),
    ];
    const result = computeInventarioSummary(items);
    expect(result.listed).toBe(2);
    expect(result.stockTotal).toBe(8); // 5 + 3
    expect(result.valorCosto).toBe(270); // 150 + 120
    expect(result.valorVenta).toBe(860); // 500 + 360
  });

  it("identifies stock alerts when stockActual <= stockMinimo", () => {
    const items = [
      makeMonturaItem({ stockActual: 1, stockMinimo: 2 }), // alert
      makeMonturaItem({ stockActual: 5, stockMinimo: 2 }), // ok
      makeMonturaItem({ stockActual: 2, stockMinimo: 2 }), // alert (<=)
    ];
    const result = computeInventarioSummary(items);
    expect(result.stockAlerts).toBe(2);
  });

  it("handles zero stock values", () => {
    const items = [
      makeMonturaItem({ stockActual: 0, costoUnitario: 0, precioVenta: 0 }),
    ];
    const result = computeInventarioSummary(items);
    expect(result.stockTotal).toBe(0);
    expect(result.valorCosto).toBe(0);
    expect(result.valorVenta).toBe(0);
  });
});

describe("soles", () => {
  it('formats 1500 as "S/ 1500.00"', () => {
    expect(soles(1500)).toBe("S/ 1500.00");
  });

  it('formats 0 as "S/ 0.00"', () => {
    expect(soles(0)).toBe("S/ 0.00");
  });

  it('formats negative numbers as "S/ -100.00"', () => {
    expect(soles(-100)).toBe("S/ -100.00");
  });

  it("formats with two decimal places", () => {
    expect(soles(99.5)).toBe("S/ 99.50");
  });

  it("rounds to two decimal places", () => {
    expect(soles(10.999)).toBe("S/ 11.00");
  });

  it("handles very large numbers", () => {
    expect(soles(1_000_000)).toBe("S/ 1000000.00");
  });

  it("handles decimal fractions", () => {
    expect(soles(0.01)).toBe("S/ 0.01");
  });

  it("handles negative fractions", () => {
    expect(soles(-0.5)).toBe("S/ -0.50");
  });
});

describe("mapMontura", () => {
  it("maps all DB fields correctly", () => {
    const result = mapMontura({
      id: "abc-123",
      sku: "SKU-X",
      marca: "Marca",
      modelo: "Modelo",
      color: "Rojo",
      stock_actual: 5,
      stock_minimo: 3,
      costo_unitario: 25,
      precio_venta: 80,
      activo: true,
    });
    expect(result.id).toBe("abc-123");
    expect(result.sku).toBe("SKU-X");
    expect(result.marca).toBe("Marca");
    expect(result.modelo).toBe("Modelo");
    expect(result.color).toBe("Rojo");
    expect(result.stockActual).toBe(5);
    expect(result.stockMinimo).toBe(3);
    expect(result.costoUnitario).toBe(25);
    expect(result.precioVenta).toBe(80);
    expect(result.activo).toBe(true);
  });

  it("defaults sku to first 8 chars of id when sku is null", () => {
    const result = mapMontura({
      id: "abcdefgh-1234",
      sku: null,
      marca: "Marca",
      modelo: "Modelo",
      stock_actual: 5,
      activo: true,
    });
    expect(result.sku).toBe("abcdefgh");
  });

  it("defaults sku to first 8 chars of id when sku is empty string", () => {
    const result = mapMontura({
      id: "12345678-xyz",
      sku: "",
      marca: "Marca",
      modelo: "Modelo",
      stock_actual: 5,
      activo: true,
    });
    expect(result.sku).toBe("12345678");
  });

  it('defaults marca to "Sin marca" when null', () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: null,
      modelo: "Modelo",
      stock_actual: 5,
      activo: true,
    });
    expect(result.marca).toBe("Sin marca");
  });

  it('defaults modelo to "Sin modelo" when null', () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: "Marca",
      modelo: null,
      stock_actual: 5,
      activo: true,
    });
    expect(result.modelo).toBe("Sin modelo");
  });

  it("defaults color to empty string when null", () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: "Marca",
      modelo: "Modelo",
      color: null,
      stock_actual: 5,
      activo: true,
    });
    expect(result.color).toBe("");
  });

  it("defaults stockMinimo to 2 when null", () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: "Marca",
      modelo: "Modelo",
      stock_actual: 5,
      stock_minimo: null,
      activo: true,
    });
    expect(result.stockMinimo).toBe(2);
  });

  it("defaults stockMinimo to 2 when stock_minimo is not finite", () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: "Marca",
      modelo: "Modelo",
      stock_actual: 5,
      stock_minimo: NaN,
      activo: true,
    });
    expect(result.stockMinimo).toBe(2);
  });

  it("defaults costoUnitario to 0 when null", () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: "Marca",
      modelo: "Modelo",
      stock_actual: 5,
      costo_unitario: null,
      activo: true,
    });
    expect(result.costoUnitario).toBe(0);
  });

  it("defaults precioVenta to 0 when null", () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: "Marca",
      modelo: "Modelo",
      stock_actual: 5,
      precio_venta: null,
      activo: true,
    });
    expect(result.precioVenta).toBe(0);
  });

  it("defaults activo to true when null", () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: "Marca",
      modelo: "Modelo",
      stock_actual: 5,
      activo: null,
    });
    expect(result.activo).toBe(true);
  });

  it("sets activo to false when activo is explicitly false", () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: "Marca",
      modelo: "Modelo",
      stock_actual: 5,
      activo: false,
    });
    expect(result.activo).toBe(false);
  });

  it("trims whitespace from string fields", () => {
    const result = mapMontura({
      id: "id",
      sku: "  SKU-X  ",
      marca: "  Marca  ",
      modelo: "  Modelo  ",
      color: "  Rojo  ",
      stock_actual: 5,
      activo: true,
    });
    expect(result.sku).toBe("SKU-X");
    expect(result.marca).toBe("Marca");
    expect(result.modelo).toBe("Modelo");
    expect(result.color).toBe("Rojo");
  });

  it("handles missing optional fields", () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: "Marca",
      modelo: "Modelo",
      stock_actual: 5,
      activo: true,
    });
    expect(result.color).toBe("");
    expect(result.stockMinimo).toBe(2);
    expect(result.costoUnitario).toBe(0);
    expect(result.precioVenta).toBe(0);
  });

  it("coerces numeric string stock values to numbers", () => {
    const result = mapMontura({
      id: "id",
      sku: "SKU",
      marca: "Marca",
      modelo: "Modelo",
      stock_actual: "5" as unknown as number,
      activo: true,
    });
    expect(result.stockActual).toBe(5);
  });
});
