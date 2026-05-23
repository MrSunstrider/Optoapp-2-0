import { describe, it, expect } from "vitest";
import {
  parseChip,
  parsePacientesSearchParams,
  baseParamsWithoutChip,
  baseParamsFull,
  hrefPage,
  hrefChipToggle,
  shortIdPreview,
} from "@/lib/pacientes-utils";

describe("parseChip", () => {
  it('returns "saldo" for "saldo"', () => {
    expect(parseChip("saldo")).toBe("saldo");
  });

  it('returns "entrega" for "entrega"', () => {
    expect(parseChip("entrega")).toBe("entrega");
  });

  it('returns "none" for undefined', () => {
    expect(parseChip(undefined)).toBe("none");
  });

  it('returns "none" for other values', () => {
    expect(parseChip("foo")).toBe("none");
    expect(parseChip("")).toBe("none");
  });
});

describe("parsePacientesSearchParams", () => {
  it("returns default values when no params provided", () => {
    const result = parsePacientesSearchParams({});
    expect(result).toEqual({
      q: "",
      page: 1,
      minEdad: undefined,
      maxEdad: undefined,
      chip: "none",
      msg: undefined,
      restantesValid: null,
    });
  });

  it("parses q param", () => {
    const result = parsePacientesSearchParams({ q: "Juan" });
    expect(result.q).toBe("Juan");
  });

  it("parses page param ensuring minimum of 1", () => {
    expect(parsePacientesSearchParams({ page: "3" }).page).toBe(3);
    expect(parsePacientesSearchParams({ page: "0" }).page).toBe(1);
    expect(parsePacientesSearchParams({ page: "-5" }).page).toBe(1);
  });

  it("defaults page to 1 for invalid page", () => {
    expect(parsePacientesSearchParams({ page: "abc" }).page).toBe(1);
  });

  it("parses minEdad and maxEdad", () => {
    const result = parsePacientesSearchParams({
      minEdad: "18",
      maxEdad: "65",
    });
    expect(result.minEdad).toBe(18);
    expect(result.maxEdad).toBe(65);
  });

  it("leaves minEdad/maxEdad undefined when omitted", () => {
    const result = parsePacientesSearchParams({});
    expect(result.minEdad).toBeUndefined();
    expect(result.maxEdad).toBeUndefined();
  });

  it("parses chip via parseChip", () => {
    expect(parsePacientesSearchParams({ chip: "saldo" }).chip).toBe("saldo");
    expect(parsePacientesSearchParams({ chip: "entrega" }).chip).toBe("entrega");
    expect(parsePacientesSearchParams({ chip: "foo" }).chip).toBe("none");
  });

  it("parses msg", () => {
    expect(parsePacientesSearchParams({ msg: "success" }).msg).toBe("success");
  });

  it("parses restantes and clamps to 0 minimum", () => {
    const result = parsePacientesSearchParams({ restantes: "5" });
    expect(result.restantesValid).toBe(5);
  });

  it("clamps negative restantes to 0", () => {
    const result = parsePacientesSearchParams({ restantes: "-3" });
    expect(result.restantesValid).toBe(0);
  });

  it("sets restantesValid to null for empty restantes", () => {
    expect(parsePacientesSearchParams({ restantes: "" }).restantesValid).toBeNull();
    expect(parsePacientesSearchParams({}).restantesValid).toBeNull();
  });

  it("floors decimal restantes", () => {
    const result = parsePacientesSearchParams({ restantes: "4.7" });
    expect(result.restantesValid).toBe(4);
  });
});

describe("shortIdPreview", () => {
  it("returns the full id if it fits within len", () => {
    expect(shortIdPreview("abc12345")).toBe("abc12345");
  });

  it("truncates and adds ellipsis if longer than len", () => {
    const result = shortIdPreview("abc123456", 8);
    expect(result).toBe("abc12345…");
  });

  it("uses default length of 8", () => {
    expect(shortIdPreview("abcdefgh")).toBe("abcdefgh");
    expect(shortIdPreview("abcdefghi")).toBe("abcdefgh…");
  });

  it("handles empty string", () => {
    expect(shortIdPreview("")).toBe("");
  });

  it("works with custom len", () => {
    expect(shortIdPreview("abc", 2)).toBe("ab…");
    expect(shortIdPreview("abc", 3)).toBe("abc");
  });
});

describe("baseParamsWithoutChip", () => {
  it("returns empty URLSearchParams for default params", () => {
    const sp = parsePacientesSearchParams({});
    const qp = baseParamsWithoutChip(sp);
    expect([...qp.entries()]).toHaveLength(0);
  });

  it("includes q, minEdad, maxEdad when present", () => {
    const sp = parsePacientesSearchParams({
      q: "test",
      minEdad: "18",
      maxEdad: "65",
    });
    const qp = baseParamsWithoutChip(sp);
    expect(qp.get("q")).toBe("test");
    expect(qp.get("minEdad")).toBe("18");
    expect(qp.get("maxEdad")).toBe("65");
    expect(qp.get("chip")).toBeNull();
  });

  it("excludes chip even when set", () => {
    const sp = parsePacientesSearchParams({ chip: "saldo", q: "test" });
    const qp = baseParamsWithoutChip(sp);
    expect(qp.get("chip")).toBeNull();
  });
});

describe("baseParamsFull", () => {
  it("includes chip when not 'none'", () => {
    const sp = parsePacientesSearchParams({ chip: "saldo" });
    const qp = baseParamsFull(sp);
    expect(qp.get("chip")).toBe("saldo");
  });

  it("excludes chip when 'none'", () => {
    const sp = parsePacientesSearchParams({});
    const qp = baseParamsFull(sp);
    expect(qp.get("chip")).toBeNull();
  });
});

describe("hrefPage", () => {
  it("returns /pacientes?page=N", () => {
    const sp = parsePacientesSearchParams({ q: "test" });
    const href = hrefPage(sp, 3);
    expect(href).toBe("/pacientes?q=test&page=3");
  });

  it("includes chip when active", () => {
    const sp = parsePacientesSearchParams({ chip: "saldo" });
    const href = hrefPage(sp, 2);
    expect(href).toBe("/pacientes?chip=saldo&page=2");
  });
});

describe("hrefChipToggle", () => {
  it("toggles to saldo when chip is none", () => {
    const sp = parsePacientesSearchParams({ q: "test" });
    const href = hrefChipToggle(sp, "saldo");
    expect(href).toBe("/pacientes?q=test&chip=saldo&page=1");
  });

  it("removes saldo when already active", () => {
    const sp = parsePacientesSearchParams({ chip: "saldo", q: "test" });
    const href = hrefChipToggle(sp, "saldo");
    expect(href).toBe("/pacientes?q=test&page=1");
  });

  it("toggles to entrega when chip is none", () => {
    const sp = parsePacientesSearchParams({});
    const href = hrefChipToggle(sp, "entrega");
    expect(href).toBe("/pacientes?chip=entrega&page=1");
  });

  it("removes entrega when already active", () => {
    const sp = parsePacientesSearchParams({ chip: "entrega" });
    const href = hrefChipToggle(sp, "entrega");
    expect(href).toBe("/pacientes?page=1");
  });

  it("switches from saldo to entrega", () => {
    const sp = parsePacientesSearchParams({ chip: "saldo" });
    const href = hrefChipToggle(sp, "entrega");
    expect(href).toBe("/pacientes?chip=entrega&page=1");
  });
});
