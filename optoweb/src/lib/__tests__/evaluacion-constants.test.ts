import { describe, it, expect } from "vitest";
import {
  TAB_ITEMS,
  ESTEREOPSIS_OPTIONS,
  LANG_OPTIONS,
  WORTH_OPTIONS,
  FARNSWORTH_OPTIONS,
  SENSIBILIDAD_OPTIONS,
  CAMPO_VISUAL_OPTIONS,
  BASES_PRISMA,
  DIAG_OPTIONS,
  CITA_ESTADOS,
  toEmptyDraft,
  parsePower,
  hasBalanceText,
  classifyRefraccion,
  autoDiagEye,
  autoPresbiciaValue,
  autoAnisometropiaValue,
  snellenToLogMar,
  autoAmbliopiaValue,
} from "@/lib/evaluacion-constants";

describe("constants", () => {
  it("TAB_ITEMS has correct values", () => {
    expect(TAB_ITEMS).toEqual([
      "Anamnesis",
      "Examen Visual",
      "Refraccion",
      "Contactologia",
      "Cierre",
    ]);
  });

  it("ESTEREOPSIS_OPTIONS has correct values", () => {
    expect(ESTEREOPSIS_OPTIONS).toEqual(["Normal", "Reducida", "Ausente"]);
  });

  it("LANG_OPTIONS has correct values", () => {
    expect(LANG_OPTIONS).toEqual(["Positivo", "Negativo"]);
  });

  it("WORTH_OPTIONS has correct values", () => {
    expect(WORTH_OPTIONS).toEqual([
      "Fusion normal",
      "Supresion OD",
      "Supresion OI",
      "Diplopia",
    ]);
  });

  it("FARNSWORTH_OPTIONS has correct values", () => {
    expect(FARNSWORTH_OPTIONS).toEqual(["Normal", "Deutan", "Protan", "Tritan"]);
  });

  it("SENSIBILIDAD_OPTIONS has correct values", () => {
    expect(SENSIBILIDAD_OPTIONS).toEqual(["Normal", "Disminuida"]);
  });

  it("CAMPO_VISUAL_OPTIONS has correct values", () => {
    expect(CAMPO_VISUAL_OPTIONS).toEqual(["Normal", "Anomalia detectada"]);
  });

  it("BASES_PRISMA has correct values", () => {
    expect(BASES_PRISMA).toEqual(["Nasal", "Temporal", "Superior", "Inferior"]);
  });

  it("DIAG_OPTIONS contains common diagnoses", () => {
    expect(DIAG_OPTIONS).toContain("Emetropia");
    expect(DIAG_OPTIONS).toContain("Miopia");
    expect(DIAG_OPTIONS).toContain("Balance");
    expect(DIAG_OPTIONS.length).toBe(9);
  });

  it("CITA_ESTADOS has correct entries", () => {
    expect(CITA_ESTADOS).toEqual([
      { code: "programada", label: "Programada" },
      { code: "confirmada", label: "Confirmada" },
      { code: "asistio", label: "Asistio" },
      { code: "no_asistio", label: "No asistio" },
      { code: "reprogramada", label: "Reprogramada" },
    ]);
  });
});

describe("toEmptyDraft", () => {
  it("returns a draft with the given fecha", () => {
    const draft = toEmptyDraft("2026-05-14");
    expect(draft.fecha).toBe("2026-05-14");
  });

  it("returns all textual fields as empty strings", () => {
    const draft = toEmptyDraft("2026-05-14");
    const emptyExpected = Object.entries(draft).filter(
      ([k, v]) =>
        typeof v === "string" &&
        !["fecha", "citaEstado"].includes(k) &&
        !["true", "false"].includes(v)
    );
    for (const [k, v] of emptyExpected) {
      expect(v, `expected ${k} to be empty`).toBe("");
    }
  });

  it("sets citaEstado to programada", () => {
    const draft = toEmptyDraft("2026-05-14");
    expect(draft.citaEstado).toBe("programada");
  });

  it("sets auto presbicia/anisometropia/ambliopia to true", () => {
    const draft = toEmptyDraft("2026-05-14");
    expect(draft.autoPresbicia).toBe("true");
    expect(draft.autoAnisometropia).toBe("true");
    expect(draft.autoAmbliopia).toBe("true");
  });

  it("sets balance/isAdd fields to false", () => {
    const draft = toEmptyDraft("2026-05-14");
    expect(draft.isAddAo).toBe("false");
    expect(draft.balanceOd).toBe("false");
    expect(draft.balanceOi).toBe("false");
    expect(draft.otrosPresbicia).toBe("false");
    expect(draft.otrosAnisometropia).toBe("false");
    expect(draft.otrosAmbliopia).toBe("false");
  });
});

describe("parsePower", () => {
  it("returns 0 for empty string", () => {
    expect(parsePower("")).toBe(0);
  });

  it("returns 0 for whitespace", () => {
    expect(parsePower("  ")).toBe(0);
  });

  it("returns 0 for plano/pl/neutro/nt", () => {
    expect(parsePower("plano")).toBe(0);
    expect(parsePower("PL")).toBe(0);
    expect(parsePower("pl")).toBe(0);
    expect(parsePower("neutro")).toBe(0);
    expect(parsePower("nt")).toBe(0);
  });

  it("parses positive numbers", () => {
    expect(parsePower("2.50")).toBe(2.5);
    expect(parsePower("+1.00")).toBe(1);
  });

  it("parses negative numbers", () => {
    expect(parsePower("-1.50")).toBe(-1.5);
    expect(parsePower("-0.75")).toBe(-0.75);
  });

  it("handles comma as decimal separator", () => {
    expect(parsePower("2,50")).toBe(2.5);
    expect(parsePower("-1,25")).toBe(-1.25);
  });

  it("returns 0 for non-numeric input", () => {
    expect(parsePower("abc")).toBe(0);
  });
});

describe("hasBalanceText", () => {
  it("returns true if any value includes 'bal'", () => {
    expect(hasBalanceText("balance")).toBe(true);
    expect(hasBalanceText("BALANCE")).toBe(true);
    expect(hasBalanceText("-2.00", "bal", "180")).toBe(true);
  });

  it("returns false if no value includes 'bal'", () => {
    expect(hasBalanceText("-2.00")).toBe(false);
    expect(hasBalanceText("-2.00", "-1.00", "180")).toBe(false);
  });

  it("returns false for empty args", () => {
    expect(hasBalanceText()).toBe(false);
  });
});

describe("classifyRefraccion", () => {
  it("classifies Emetropia", () => {
    expect(classifyRefraccion("0", "0")).toBe("Emetropia");
    expect(classifyRefraccion("0.00", "0.00")).toBe("Emetropia");
  });

  it("classifies Miopia", () => {
    expect(classifyRefraccion("-2.00", "0")).toBe("Miopia");
    expect(classifyRefraccion("-1.00", "0")).toBe("Miopia");
  });

  it("classifies Hipermetropia", () => {
    expect(classifyRefraccion("2.00", "0")).toBe("Hipermetropia");
    expect(classifyRefraccion("1.00", "0")).toBe("Hipermetropia");
  });

  it("classifies Astigmatismo miopico compuesto", () => {
    expect(classifyRefraccion("-2.00", "-1.00")).toBe(
      "Astigmatismo miopico compuesto"
    );
  });

  it("classifies Astigmatismo hipermetropico compuesto", () => {
    expect(classifyRefraccion("2.00", "1.00")).toBe(
      "Astigmatismo hipermetropico compuesto"
    );
  });

  it("classifies Astigmatismo mixto", () => {
    expect(classifyRefraccion("1.00", "-2.00")).toBe("Astigmatismo mixto");
  });

  it("normalizes positive cil to negative transposition", () => {
    // +2.00 -1.00 x 180 → transpose to +1.00 +1.00 → which is hypermetropic astigmatism
    const result = classifyRefraccion("2.00", "-1.00");
    expect(result).toBe("Astigmatismo hipermetropico compuesto");
  });
});

describe("autoDiagEye", () => {
  it("returns Balance if balanceChecked is true", () => {
    expect(autoDiagEye("", "", "", true)).toBe("Balance");
  });

  it("returns Balance if any field contains 'bal'", () => {
    expect(autoDiagEye("balance", "", "", false)).toBe("Balance");
    expect(autoDiagEye("", "bal", "", false)).toBe("Balance");
    expect(autoDiagEye("", "", "180", false)).not.toBe("Balance");
  });

  it("returns empty string if no data", () => {
    expect(autoDiagEye("", "", "", false)).toBe("");
  });

  it("delegates to classifyRefraccion when there is data", () => {
    expect(autoDiagEye("-2.00", "0", "180", false)).toBe("Miopia");
    expect(autoDiagEye("0", "0", "0", false)).toBe("Emetropia");
  });
});

describe("autoPresbiciaValue", () => {
  it("returns true if addOd has a positive value", () => {
    expect(autoPresbiciaValue("2.00", "")).toBe(true);
  });

  it("falls back to addOi if addOd is empty", () => {
    expect(autoPresbiciaValue("", "1.50")).toBe(true);
  });

  it("returns false if both values are empty or zero", () => {
    expect(autoPresbiciaValue("", "")).toBe(false);
    expect(autoPresbiciaValue("0", "0")).toBe(false);
  });

  it("returns false for negative add values", () => {
    expect(autoPresbiciaValue("-0.50", "")).toBe(false);
  });
});

describe("autoAnisometropiaValue", () => {
  it("returns false if either eye is balance", () => {
    expect(autoAnisometropiaValue("-2.00", "0", "-0.50", "0", true, false)).toBe(
      false
    );
    expect(autoAnisometropiaValue("-2.00", "0", "-0.50", "0", false, true)).toBe(
      false
    );
  });

  it("returns false if either esf is empty", () => {
    expect(autoAnisometropiaValue("", "0", "-0.50", "0", false, false)).toBe(
      false
    );
    expect(autoAnisometropiaValue("-2.00", "0", "", "0", false, false)).toBe(
      false
    );
  });

  it("returns true when spherical equivalent difference >= 2D", () => {
    // OD EE = -4.00 + 0/2 = -4.00, OI EE = -0.50 + 0/2 = -0.50, diff = 3.5
    expect(
      autoAnisometropiaValue("-4.00", "0", "-0.50", "0", false, false)
    ).toBe(true);
  });

  it("returns false when spherical equivalent difference < 2D", () => {
    expect(
      autoAnisometropiaValue("-2.00", "0", "-1.00", "0", false, false)
    ).toBe(false);
  });
});

describe("snellenToLogMar", () => {
  it("returns null for non-matching input", () => {
    expect(snellenToLogMar("")).toBeNull();
    expect(snellenToLogMar("abc")).toBeNull();
  });

  it("converts 20/20 to 0", () => {
    const result = snellenToLogMar("20/20");
    expect(result).toBeCloseTo(0, 5);
  });

  it("converts 20/40 to ~0.301", () => {
    const result = snellenToLogMar("20/40");
    expect(result).toBeCloseTo(0.301, 2);
  });

  it("converts 20/200 to 1.0", () => {
    const result = snellenToLogMar("20/200");
    expect(result).toBeCloseTo(1.0, 2);
  });

  it("handles whitespace", () => {
    const result = snellenToLogMar("  20 / 25  ");
    expect(result).toBeCloseTo(0.097, 2);
  });

  it("returns null for invalid denominator", () => {
    expect(snellenToLogMar("20/0")).toBeNull();
  });
});

describe("autoAmbliopiaValue", () => {
  it("returns null if either AV is not a valid snellen", () => {
    expect(autoAmbliopiaValue("", "20/20")).toBeNull();
    expect(autoAmbliopiaValue("20/20", "")).toBeNull();
  });

  it("returns true if logMAR difference >= 0.19", () => {
    // 20/20 = 0, 20/40 ≈ 0.301 → diff ≈ 0.301 ≥ 0.19
    expect(autoAmbliopiaValue("20/20", "20/40")).toBe(true);
  });

  it("returns false if logMAR difference < 0.19", () => {
    // 20/20 = 0, 20/25 ≈ 0.097 → diff ≈ 0.097 < 0.19
    expect(autoAmbliopiaValue("20/20", "20/25")).toBe(false);
  });
});
