import { describe, it, expect } from "vitest";
import {
  TIPO_LENTE,
  SUB_BIFOCAL,
  DISTANCIA,
  MATERIAL_LENTE,
  ORIGEN_MONTURA,
  TIPO_ARO,
  MATERIAL_MONTURA,
  METODO_PAGO,
  ESTADO_ENTREGA,
  TRATAMIENTOS,
  parseNum,
} from "@/lib/dispensacion-types";

describe("constants", () => {
  it("TIPO_LENTE has correct values", () => {
    expect(TIPO_LENTE).toEqual([
      "Monofocal",
      "Bifocal",
      "Progresivo",
      "Ocupacional",
    ]);
  });

  it("SUB_BIFOCAL has correct values", () => {
    expect(SUB_BIFOCAL).toEqual(["Flaptop", "Invisible"]);
  });

  it("DISTANCIA has correct values", () => {
    expect(DISTANCIA).toEqual(["Lejos", "Intermedia", "Cerca"]);
  });

  it("MATERIAL_LENTE has correct values", () => {
    expect(MATERIAL_LENTE).toEqual([
      "Resina",
      "Policarbonato",
      "Cristal",
      "Trivex",
    ]);
  });

  it("ORIGEN_MONTURA has correct values", () => {
    expect(ORIGEN_MONTURA).toEqual(["Tienda", "Paciente"]);
  });

  it("TIPO_ARO has correct values", () => {
    expect(TIPO_ARO).toEqual(["Aro Completo", "Semi al aire", "Al aire"]);
  });

  it("MATERIAL_MONTURA has correct values", () => {
    expect(MATERIAL_MONTURA).toEqual(["Acetato", "Metal", "Carey", "Econ"]);
  });

  it("METODO_PAGO has correct values", () => {
    expect(METODO_PAGO).toEqual(["Efectivo", "Tarjeta", "Transferencia"]);
  });

  it("ESTADO_ENTREGA has correct values", () => {
    expect(ESTADO_ENTREGA).toEqual(["Pendiente", "Entregado"]);
  });

  it("TRATAMIENTOS has correct values", () => {
    expect(TRATAMIENTOS).toEqual([
      "Ninguno",
      "Antireflejo",
      "Antirayas",
      "Filtro UV 400",
      "Fotocromático",
      "AR Blue Defense",
    ]);
  });
});

describe("parseNum", () => {
  it("parses an integer string", () => {
    expect(parseNum("42")).toBe(42);
  });

  it("parses a decimal string with dot", () => {
    expect(parseNum("3.14")).toBe(3.14);
  });

  it("handles comma as decimal separator", () => {
    expect(parseNum("2,50")).toBe(2.5);
    expect(parseNum("1,75")).toBe(1.75);
  });

  it("returns 0 for empty string", () => {
    expect(parseNum("")).toBe(0);
  });

  it("returns 0 for non-numeric input", () => {
    expect(parseNum("abc")).toBe(0);
  });

  it("returns 0 for whitespace", () => {
    expect(parseNum("   ")).toBe(0);
  });

  it("parses negative numbers", () => {
    expect(parseNum("-1.50")).toBe(-1.5);
  });

  it("handles leading/trailing whitespace", () => {
    expect(parseNum("  4.50  ")).toBe(4.5);
  });
});
