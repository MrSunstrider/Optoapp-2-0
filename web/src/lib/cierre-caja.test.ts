import { describe, it, expect } from "vitest";
import {
  normalizeMoney,
  toCents,
  fromCents,
  mapMedioPago,
  resolveCierrePeriodo,
  canReadCierre,
  canCloseCierre,
  canOverrideCierre,
} from "@/lib/cierre-caja";

describe("normalizeMoney", () => {
  it("returns 0 for null", () => {
    expect(normalizeMoney(null as unknown as number)).toBe(0);
  });

  it("returns 0 for undefined", () => {
    expect(normalizeMoney(undefined as unknown as number)).toBe(0);
  });

  it("returns 0 for NaN", () => {
    expect(normalizeMoney(NaN)).toBe(0);
  });

  it("returns 0 for non-numeric strings", () => {
    expect(normalizeMoney("abc" as unknown as number)).toBe(0);
  });

  it("returns rounded value for valid numbers", () => {
    expect(normalizeMoney(150.5)).toBe(150.5);
  });

  it("rounds to two decimal places", () => {
    expect(normalizeMoney(150.506)).toBe(150.51);
  });

  it("returns 0 for numeric strings since Number.isFinite does not coerce", () => {
    // Number.isFinite does not coerce strings, unlike the global isFinite()
    expect(normalizeMoney("150.50" as unknown as number)).toBe(0);
  });

  it("returns 0 for Infinity", () => {
    expect(normalizeMoney(Infinity)).toBe(0);
  });

  it("returns 0 for -Infinity", () => {
    expect(normalizeMoney(-Infinity)).toBe(0);
  });

  it("returns 0 for zero", () => {
    expect(normalizeMoney(0)).toBe(0);
  });
});

describe("toCents", () => {
  it("converts 150.50 to 15050", () => {
    expect(toCents(150.5)).toBe(15050);
  });

  it("converts 0 to 0", () => {
    expect(toCents(0)).toBe(0);
  });

  it("converts 99.99 to 9999", () => {
    expect(toCents(99.99)).toBe(9999);
  });

  it("handles floating point precision", () => {
    expect(toCents(10.1)).toBe(1010);
  });

  it("converts negative values correctly", () => {
    expect(toCents(-50.25)).toBe(-5025);
  });

  it("rounds to nearest cent", () => {
    expect(toCents(10.999)).toBe(1100);
  });
});

describe("fromCents", () => {
  it("converts 15050 to 150.50", () => {
    expect(fromCents(15050)).toBe(150.5);
  });

  it("converts 0 to 0", () => {
    expect(fromCents(0)).toBe(0);
  });

  it("converts 9999 to 99.99", () => {
    expect(fromCents(9999)).toBe(99.99);
  });

  it("handles negative cents", () => {
    expect(fromCents(-5025)).toBe(-50.25);
  });

  it("rounds to two decimal places", () => {
    expect(fromCents(100)).toBe(1);
  });
});

describe("mapMedioPago", () => {
  it('maps "yape" to "Móvil/Trans"', () => {
    expect(mapMedioPago("yape")).toBe("Móvil/Trans");
  });

  it('maps "plin" to "Móvil/Trans"', () => {
    expect(mapMedioPago("plin")).toBe("Móvil/Trans");
  });

  it('maps "transferencia" to "Móvil/Trans"', () => {
    expect(mapMedioPago("transferencia")).toBe("Móvil/Trans");
  });

  it('maps "EFECTIVO" to "Efectivo"', () => {
    expect(mapMedioPago("EFECTIVO")).toBe("Efectivo");
  });

  it('maps "efectivo" to "Efectivo"', () => {
    expect(mapMedioPago("efectivo")).toBe("Efectivo");
  });

  it('maps "tarjeta credito" to "Tarjeta"', () => {
    expect(mapMedioPago("tarjeta credito")).toBe("Tarjeta");
  });

  it('maps "TARJETA DéBITO" to "Tarjeta"', () => {
    expect(mapMedioPago("TARJETA DéBITO")).toBe("Tarjeta");
  });

  it('maps empty string to "Efectivo" (current behavior: !v short-circuits to efectivo)', () => {
    // NOTE: Current code treats empty string as efectivo via !v || v.includes("efectivo")
    expect(mapMedioPago("")).toBe("Efectivo");
  });

  it('maps null to "Efectivo" (current behavior: null becomes "" same path)', () => {
    expect(mapMedioPago(null)).toBe("Efectivo");
  });

  it('maps unknown method to "Otro"', () => {
    expect(mapMedioPago("desconocido")).toBe("Otro");
  });

  it("trims whitespace from input", () => {
    expect(mapMedioPago("  efectivo  ")).toBe("Efectivo");
  });
});

describe("resolveCierrePeriodo", () => {
  it("returns correct ISO date boundaries for a daily period", () => {
    const result = resolveCierrePeriodo("2026-05-10");
    expect(result.fecha).toBe("2026-05-10");
    expect(result.from).toBe("2026-05-10");
    expect(result.toExclusive).toBe("2026-05-11");
  });

  it("handles month-end correctly", () => {
    const result = resolveCierrePeriodo("2026-01-31");
    expect(result.from).toBe("2026-01-31");
    expect(result.toExclusive).toBe("2026-02-01");
  });

  it("handles year-end correctly", () => {
    const result = resolveCierrePeriodo("2025-12-31");
    expect(result.from).toBe("2025-12-31");
    expect(result.toExclusive).toBe("2026-01-01");
  });

  it("handles different dates correctly", () => {
    const result = resolveCierrePeriodo("2026-03-15");
    expect(result.fecha).toBe("2026-03-15");
    expect(result.from).toBe("2026-03-15");
    expect(result.toExclusive).toBe("2026-03-16");
  });
});

describe("canReadCierre", () => {
  it("returns true for admin", () => {
    expect(canReadCierre("admin")).toBe(true);
  });

  it("returns true for gerente", () => {
    expect(canReadCierre("gerente")).toBe(true);
  });

  it("returns true for especialista", () => {
    expect(canReadCierre("especialista")).toBe(true);
  });

  it("returns true for asesor", () => {
    expect(canReadCierre("asesor")).toBe(true);
  });

  it("returns true for ventas", () => {
    expect(canReadCierre("ventas")).toBe(true);
  });

  it("returns false for invitado", () => {
    expect(canReadCierre("invitado")).toBe(false);
  });

  it("returns false for lectura", () => {
    expect(canReadCierre("lectura")).toBe(false);
  });

  it("is case insensitive", () => {
    expect(canReadCierre("Admin")).toBe(true);
    expect(canReadCierre("INVITADO")).toBe(false);
  });
});

describe("canCloseCierre", () => {
  it("returns true for admin", () => {
    expect(canCloseCierre("admin")).toBe(true);
  });

  it("returns true for gerente", () => {
    expect(canCloseCierre("gerente")).toBe(true);
  });

  it("returns true for cajero", () => {
    expect(canCloseCierre("cajero")).toBe(true);
  });

  it("returns false for especialista", () => {
    expect(canCloseCierre("especialista")).toBe(false);
  });

  it("returns false for asesor", () => {
    expect(canCloseCierre("asesor")).toBe(false);
  });

  it("returns false for invitado", () => {
    expect(canCloseCierre("invitado")).toBe(false);
  });

  it("is case insensitive", () => {
    expect(canCloseCierre("Admin")).toBe(true);
    expect(canCloseCierre("GERENTE")).toBe(true);
  });

  it("handles whitespace", () => {
    expect(canCloseCierre("  admin  ")).toBe(true);
  });
});

describe("canOverrideCierre", () => {
  it("returns true for admin", () => {
    expect(canOverrideCierre("admin")).toBe(true);
  });

  it("returns true for gerente", () => {
    expect(canOverrideCierre("gerente")).toBe(true);
  });

  it("returns false for cajero", () => {
    expect(canOverrideCierre("cajero")).toBe(false);
  });

  it("returns false for especialista", () => {
    expect(canOverrideCierre("especialista")).toBe(false);
  });

  it("returns false for asesor", () => {
    expect(canOverrideCierre("asesor")).toBe(false);
  });

  it("returns false for invitado", () => {
    expect(canOverrideCierre("invitado")).toBe(false);
  });

  it("is case insensitive", () => {
    expect(canOverrideCierre("Admin")).toBe(true);
    expect(canOverrideCierre("gerente")).toBe(true);
  });

  it("handles whitespace", () => {
    expect(canOverrideCierre("  admin  ")).toBe(true);
  });
});
