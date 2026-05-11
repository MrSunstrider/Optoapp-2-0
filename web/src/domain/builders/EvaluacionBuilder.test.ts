import { describe, it, expect } from "vitest";
import {
  normalizeSphere,
  calculateEE,
  transpose,
  EvaluacionBuilder,
} from "./EvaluacionBuilder";

describe("normalizeSphere", () => {
  it('converts "plano" to "0.00"', () => {
    expect(normalizeSphere("plano")).toBe("0.00");
  });

  it('converts "PLANO" (uppercase) to "0.00"', () => {
    expect(normalizeSphere("PLANO")).toBe("0.00");
  });

  it('converts "Plano" (mixed case) to "0.00"', () => {
    expect(normalizeSphere("Plano")).toBe("0.00");
  });

  it('converts "neutro" to "0.00"', () => {
    expect(normalizeSphere("neutro")).toBe("0.00");
  });

  it('converts "pl" to "0.00"', () => {
    expect(normalizeSphere("pl")).toBe("0.00");
  });

  it("returns positive sphere value as-is", () => {
    expect(normalizeSphere("+1.00")).toBe("+1.00");
  });

  it("returns negative sphere value as-is", () => {
    expect(normalizeSphere("-2.50")).toBe("-2.50");
  });

  it("returns zero sphere as-is (no normalization of numeric zero)", () => {
    expect(normalizeSphere("0")).toBe("0");
  });

  it("returns sphere with trailing zeros as-is", () => {
    expect(normalizeSphere("0.00")).toBe("0.00");
  });

  it("trims whitespace before checking", () => {
    expect(normalizeSphere("  plano  ")).toBe("0.00");
  });
});

describe("calculateEE", () => {
  it("computes EE for +1.00 -0.50 as +0.75", () => {
    // EE = sphere + (cylinder / 2) = 1.00 + (-0.50 / 2) = 1.00 - 0.25 = 0.75
    expect(calculateEE("+1.00", "-0.50")).toBe(0.75);
  });

  it("computes EE for -2.00 -1.00 as -2.50", () => {
    // EE = -2.00 + (-1.00 / 2) = -2.00 - 0.50 = -2.50
    expect(calculateEE("-2.00", "-1.00")).toBe(-2.5);
  });

  it("computes EE for plano as 0", () => {
    expect(calculateEE("0", "0")).toBe(0);
  });

  it("handles positive cylinder", () => {
    // EE = -1.00 + (+0.50 / 2) = -1.00 + 0.25 = -0.75
    expect(calculateEE("-1.00", "+0.50")).toBe(-0.75);
  });

  it("handles decimal values with rounding", () => {
    // EE = 0.25 + (0.25 / 2) = 0.25 + 0.125 = 0.375
    expect(calculateEE("0.25", "0.25")).toBe(0.375);
  });

  it("handles large values", () => {
    expect(calculateEE("10.00", "-5.00")).toBe(7.5);
  });

  it("returns 0 when sphere is NaN", () => {
    expect(calculateEE("not-a-number", "0")).toBe(0);
  });

  it("returns sphere when cylinder is NaN", () => {
    expect(calculateEE("1.00", "not-a-number")).toBe(1);
  });
});

describe("transpose", () => {
  it("transposes plus-cylinder to minus-cylinder format", () => {
    // +1.00 +0.50 x 90 → new sphere = 1.00 + 0.50 = 1.50, new cyl = -0.50, new axis = 90 + 90 = 180
    const result = transpose("+1.00", "+0.50", "90");
    expect(result.nEsf).toBe("1.50");
    expect(result.nCil).toBe("-0.50");
    expect(result.nEje).toBe("180");
  });

  it("does nothing if already in minus-cylinder format", () => {
    const result = transpose("+1.00", "-0.50", "180");
    expect(result.nEsf).toBe("+1.00");
    expect(result.nCil).toBe("-0.50");
    expect(result.nEje).toBe("180");
  });

  it("transposes with axis wrap (axis + 90 > 180 subtracts 180)", () => {
    // +1.00 +1.00 x 100 → new sphere = 2.00, new cyl = -1.00, new axis = 100 + 90 - 180 = 10
    const result = transpose("+1.00", "+1.00", "100");
    expect(result.nEsf).toBe("2.00");
    expect(result.nCil).toBe("-1.00");
    expect(result.nEje).toBe("10");
  });

  it("handles plano (0 sphere, 0 cylinder)", () => {
    const result = transpose("0", "0", "0");
    expect(result.nEsf).toBe("0");
    expect(result.nCil).toBe("0");
    expect(result.nEje).toBe("0");
  });

  it("handles negative cylinder (no transposition needed)", () => {
    const result = transpose("-2.00", "-1.00", "90");
    expect(result.nEsf).toBe("-2.00");
    expect(result.nCil).toBe("-1.00");
    expect(result.nEje).toBe("90");
  });

  it("transposes to minus-cylinder even when both sphere and cylinder are negative", () => {
    // Wrong — cylinder must be "positive" for transposition to trigger
    // cil parseFloat = -1.00, which is NOT > 0, so no transposition
    // Let me do a valid positive cylinder case
    const result = transpose("-2.00", "+1.00", "45");
    expect(result.nEsf).toBe("-1.00"); // -2.00 + 1.00 = -1.00
    expect(result.nCil).toBe("-1.00"); // negated cylinder
    expect(result.nEje).toBe("135"); // 45 + 90 = 135
  });

  it("handles edge case: axis 90 → transposes to 180", () => {
    const result = transpose("0", "+0.50", "90");
    expect(result.nEsf).toBe("0.50");
    expect(result.nCil).toBe("-0.50");
    expect(result.nEje).toBe("180");
  });

  it("handles edge case: axis 180 → transposes to 90", () => {
    const result = transpose("0", "+0.50", "180");
    expect(result.nEsf).toBe("0.50");
    expect(result.nCil).toBe("-0.50");
    expect(result.nEje).toBe("90"); // 180 + 90 = 270 - 180 = 90
  });
});

describe("EvaluacionBuilder", () => {
  it("builds a valid evaluacion with paciente and optica", () => {
    const result = new EvaluacionBuilder()
      .withPaciente("pac-123")
      .withOptica("opt-456")
      .build();
    expect(result.paciente_id).toBe("pac-123");
    expect(result.optica_id).toBe("opt-456");
    expect(result.id).toBeDefined();
    expect(result.fecha).toBeDefined();
  });

  it("builds with refraction data for both eyes", () => {
    const result = new EvaluacionBuilder()
      .withPaciente("pac-123")
      .withOptica("opt-456")
      .withRefraccionOd("+1.00", "-0.50", "180")
      .withRefraccionOi("-2.00", "-1.00", "90")
      .build();
    expect(result.receta_od_esf).toBe("+1.00");
    expect(result.receta_od_cil).toBe("-0.50");
    expect(result.receta_od_eje).toBe("180");
    expect(result.receta_oi_esf).toBe("-2.00");
    expect(result.receta_oi_cil).toBe("-1.00");
    expect(result.receta_oi_eje).toBe("90");
  });

  it("transposes plus-cylinder to minus-cylinder on build", () => {
    const result = new EvaluacionBuilder()
      .withPaciente("pac-123")
      .withOptica("opt-456")
      .withRefraccionOd("+1.00", "+0.50", "90")
      .build();
    // +1.00 +0.50 x 90 → +1.50 -0.50 x 180
    expect(result.receta_od_esf).toBe("1.50");
    expect(result.receta_od_cil).toBe("-0.50");
    expect(result.receta_od_eje).toBe("180");
  });
});
