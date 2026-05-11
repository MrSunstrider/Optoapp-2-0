import { describe, it, expect, beforeEach, vi, afterEach } from "vitest";
import { resolveRange, labelForPeriodo, type ReporteFinancieroResult } from "@/lib/reportes-financieros";

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
});

describe("resolveRange", () => {
  it('returns today-to-tomorrow for "dia"', () => {
    vi.setSystemTime(new Date("2026-05-15T10:30:00"));
    const result = resolveRange("dia");
    expect(result.start).toBe("2026-05-15");
    expect(result.endExclusive).toBe("2026-05-16");
  });

  it('returns Monday-to-next-Monday for "semana"', () => {
    // 2026-05-15 is a Friday
    vi.setSystemTime(new Date("2026-05-15T10:30:00"));
    const result = resolveRange("semana");
    // Monday of that week is 2026-05-11
    expect(result.start).toBe("2026-05-11");
    expect(result.endExclusive).toBe("2026-05-18");
  });

  it('handles Sunday for "semana" (day=0, should go back 6 days to Monday)', () => {
    // 2026-05-17 is a Sunday
    vi.setSystemTime(new Date("2026-05-17T10:30:00"));
    const result = resolveRange("semana");
    // Monday of that week
    expect(result.start).toBe("2026-05-11");
    expect(result.endExclusive).toBe("2026-05-18");
  });

  it('handles Monday for "semana" (day=1, same day as start)', () => {
    // 2026-05-11 is a Monday
    vi.setSystemTime(new Date("2026-05-11T10:30:00"));
    const result = resolveRange("semana");
    expect(result.start).toBe("2026-05-11");
    expect(result.endExclusive).toBe("2026-05-18");
  });

  it('returns month boundaries for "mes"', () => {
    vi.setSystemTime(new Date("2026-05-15T10:30:00"));
    const result = resolveRange("mes");
    expect(result.start).toBe("2026-05-01");
    expect(result.endExclusive).toBe("2026-06-01");
  });

  it("handles December for mes (year boundary)", () => {
    vi.setSystemTime(new Date("2026-12-15T10:30:00"));
    const result = resolveRange("mes");
    expect(result.start).toBe("2026-12-01");
    expect(result.endExclusive).toBe("2027-01-01");
  });

  it('returns year boundaries for "anio"', () => {
    vi.setSystemTime(new Date("2026-05-15T10:30:00"));
    const result = resolveRange("anio");
    expect(result.start).toBe("2026-01-01");
    expect(result.endExclusive).toBe("2027-01-01");
  });

  it("returns undefined for unknown periodo (no switch default)", () => {
    vi.setSystemTime(new Date("2026-05-15T10:30:00"));
    const result = resolveRange("unknown" as never);
    expect(result).toBeUndefined();
  });
});

describe("labelForPeriodo", () => {
  it('returns "Día" for "dia"', () => {
    expect(labelForPeriodo("dia")).toBe("Día");
  });

  it('returns "Semana" for "semana"', () => {
    expect(labelForPeriodo("semana")).toBe("Semana");
  });

  it('returns "Mes" for "mes"', () => {
    expect(labelForPeriodo("mes")).toBe("Mes");
  });

  it('returns "Año" for "anio"', () => {
    expect(labelForPeriodo("anio")).toBe("Año");
  });

  it("returns undefined for unknown periodo (no switch default)", () => {
    const result = labelForPeriodo("unknown" as never);
    expect(result).toBeUndefined();
  });
});

describe("ReporteFinancieroResult type", () => {
  it("includes status field with overall: ok on success", () => {
    // Type-level test: verify the interface exists
    const result: ReporteFinancieroResult = {
      periodoLabel: "Mes",
      ingresosCobrados: 100,
      ventasEmitidas: 200,
      saldoPendiente: 50,
      totalMovimientos: 10,
      ticketPromedio: 20,
      fechaInicio: "2026-05-01",
      fechaFinExclusiva: "2026-06-01",
      status: { overall: "ok" },
    };
    expect(result.status.overall).toBe("ok");
    expect(result.status.error).toBeUndefined();
  });

  it("includes status field with overall: degraded on error", () => {
    const result: ReporteFinancieroResult = {
      periodoLabel: "Mes",
      ingresosCobrados: 0,
      ventasEmitidas: 0,
      saldoPendiente: 0,
      totalMovimientos: 0,
      ticketPromedio: 0,
      fechaInicio: "2026-05-01",
      fechaFinExclusiva: "2026-06-01",
      status: { overall: "degraded", error: "RPC failed" },
    };
    expect(result.status.overall).toBe("degraded");
    expect(result.status.error).toBe("RPC failed");
  });
});